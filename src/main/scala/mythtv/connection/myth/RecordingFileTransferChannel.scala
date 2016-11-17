package mythtv
package connection
package myth

import model.{ ChanId, Recording }
import util.MythDateTime

trait RecordingTransferChannel extends FileTransferChannel {
  def recording: Recording
  def isRecordingInProgress: Boolean
}

// Only use this (recording in progress) file channel if we have called
// api.checkRecording(Recording) to verify that the recording is still in progress.

// Otherwise use a CompletedRecordingTransferChannel.  Don't expose these
// differences to clients; hide behind a RecordingTransferChannel trait.

// TODO: also listen for DoneRecording/FileClosed event
// TODO: listen for RecordingListUpdate event to update our copy of Recording object??

private class InProgressRecordingTransferChannel(
  controlChannel: MythFileTransferAPI,
  dataChannel: FileTransferConnection,
  eventChannel: EventConnection,
  rec: Recording
) extends EventingFileTransferChannel(controlChannel, dataChannel, eventChannel)
     with RecordingTransferChannel {

  override def recording: Recording = rec

  override def isRecordingInProgress = true // TODO not always true! just until we hear otherwise

  override def listener: EventListener = updateListener

  // TODO block read if the recording is still in progress but we hit EOF? (wait for the event to arrive...)

  private[this] lazy val updateListener = new EventListener {
    override def listenFor(event: Event): Boolean = event match {
      case e: Event.UpdateFileSizeEvent => true
      case _ => false
    }

    override def handle(event: Event): Unit = event match {
      case Event.UpdateFileSizeEvent(chanId, startTime, newSize) =>
        if (chanId == recording.chanId && startTime == recording.startTime)
          currentSize = newSize.bytes
      case _ => ()
    }
  }
}

private class CompletedRecordingTransferChannel(
  controlChannel: MythFileTransferAPI,
  dataChannel: FileTransferConnection,
  rec: Recording
) extends FileTransferChannelImpl(controlChannel, dataChannel)
     with RecordingTransferChannel {
  override def recording: Recording = rec
  override def isRecordingInProgress = false
}

object RecordingTransferChannel {
  // TODO: method that takes a hostname vs control channel?

  def apply(api: BackendAPIConnection, chanId: ChanId, recStartTs: MythDateTime): RecordingTransferChannel = {
    val rec = api.queryRecording(chanId, recStartTs).right.get   // TODO check for failure/not found
    apply(api, rec)
  }

  def apply(api: BackendAPIConnection, rec: Recording): RecordingTransferChannel = {
    val inProgress = api.checkRecording(rec).right.get

    // TODO who is managing these opened connections??  Also, we have no re-use...

    val controlChannel =
      if (rec.hostname == api.host) api
      else BackendAPIConnection(rec.hostname)

    val dataChannel = FileTransferConnection(
      controlChannel.host,
      rec.filename,
      rec.storageGroup,
      port = controlChannel.port
    )

    val fto = MythFileTransferObject(controlChannel, dataChannel)

    if (inProgress) {
      val eventChannel = EventConnection(controlChannel.host, controlChannel.port)
      new InProgressRecordingTransferChannel(fto, dataChannel, eventChannel, rec)
    } else {
      new CompletedRecordingTransferChannel(fto, dataChannel, rec)
    }
  }
}
