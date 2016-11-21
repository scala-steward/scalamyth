package mythtv
package connection
package myth

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

import util.MythDateTime
import model.{ ChanId, Recording }
import Event.{ DoneRecordingEvent, RecordingListUpdateEvent, UpdateFileSizeEvent }

trait RecordingTransferChannel extends FileTransferChannel {
  def recording: Recording
  def isRecordingInProgress: Boolean
}

// Only use this (recording in progress) file channel if we have called
// api.checkRecording(Recording) to verify that the recording is still in progress.

// Otherwise use a CompletedRecordingTransferChannel.  Don't expose these
// differences to clients; hide behind a RecordingTransferChannel trait.

private class InProgressRecordingTransferChannel(
  controlChannel: FileTransferAPI,
  dataChannel: FileTransferConnection,
  eventChannel: EventConnection,
  @volatile private[this] var rec: Recording
) extends EventingFileTransferChannel(controlChannel, dataChannel, eventChannel)
     with RecordingTransferChannel {

  // Keep track of the cardId this recording is using, it will be overwritten by
  // later updates to 'rec' by the RecordingListUpdate event
  private[this] val usingCardId = rec.cardId

  @volatile private[this] var inProgress = true

  private[this] val lock = new ReentrantLock
  private[this] val sizeChanged = lock.newCondition

  override def recording: Recording = rec

  override def isRecordingInProgress = inProgress

  override def listener: EventListener = updateListener

  override protected def waitForMoreData(oldSize: Long): Boolean = {
    if (inProgress) doWaitForMoreData(oldSize)
    else false
  }

  private def doWaitForMoreData(oldSize: Long): Boolean = {
    println("waiting for more data " + oldSize)
    lock.lock()
    try {
      while (inProgress && currentSize <= oldSize)
        sizeChanged.await(10, TimeUnit.MINUTES)  // TODO wait forever? probably a bad idea
    }
    finally lock.unlock()
    true
  }

  private def signalSizeChanged(): Unit = {
    lock.lock()
    try sizeChanged.signalAll()
    finally lock.unlock()
  }

  /* We listen for DoneRecording event rather than RecordingFinished bacause
     the latter is a system event, and we are not guaranteed to receive system
     events in all cases (they are only dispatched once per target host unless
     you are registered as system-events-only.) */

  private[this] lazy val updateListener = new EventListener {
    override def listenFor(event: Event): Boolean = event match {
      case _: UpdateFileSizeEvent => true
      case _: DoneRecordingEvent => true
      case _: RecordingListUpdateEvent => true
      case _ => false
    }

    override def handle(event: Event): Unit = event match {
      case UpdateFileSizeEvent(chanId, recStartTs, newSize) =>
        if (chanId == rec.chanId && recStartTs == rec.recStartTS) {
          currentSize = newSize.bytes
          signalSizeChanged()
        }
      case RecordingListUpdateEvent(r: Recording) =>
        if (r.chanId == rec.chanId && r.startTime == rec.startTime)
          rec = r
      case DoneRecordingEvent(cardId, _, _) =>
        if (cardId == usingCardId) {
          inProgress = false
          signalSizeChanged()
        }
      case _ => ()
    }
  }
}

private class CompletedRecordingTransferChannel(
  controlChannel: FileTransferAPI,
  dataChannel: FileTransferConnection,
  rec: Recording
) extends FileTransferChannelImpl(controlChannel, dataChannel)
     with RecordingTransferChannel {
  override def recording: Recording = rec
  override def isRecordingInProgress = false
}

object RecordingTransferChannel {
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
