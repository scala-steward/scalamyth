// SPDX-License-Identifier: LGPL-2.1-only
/*
 * RecordingTransferChannel.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth

import java.io.Closeable

import util.MythDateTime
import model.{ ChanId, Recording }
import Event.{ DoneRecordingEvent, RecordingListUpdateEvent, UpdateFileSizeEvent }

trait RecordingTransferChannel extends FileTransferChannel {
  def recording: Recording
  def isRecordingInProgress: Boolean
}

private class RecordingTransfer(api: MythProtocolAPIConnection, @volatile private[this] var rec: Recording)
  extends Closeable { outer =>
  @volatile private[this] var inProgress = true

  // Keep track of the cardId this recording is using, it will be overwritten by
  // later updates to 'rec' by the RecordingListUpdate event
  private[this] val usingCardId = rec.cardId

  // TODO who is managing these opened connections??  Also, we have no re-use...

  private[this] val controlConn =
    if (rec.hostname == api.host) api
    else MythProtocolAPIConnection(rec.hostname)

  private[this] val dataConn = FileTransferConnection(
    controlConn.host,
    rec.filename,
    rec.storageGroup,
    port = controlConn.port,
    useReadAhead = true
  )

  private[this] val xferChannel: TransferChannel = setupTransferChannel()

  private[this] val eventConn = EventConnection(controlConn.host, controlConn.port, listener = updateListener)

  checkInProgress()   // NB must check *after* event listener is in place

  override def close(): Unit = {
    eventConn.close()
  }

  // Only update inProgress status if checkRecording says false. We don't want to overwrite
  // any updates we may have received from the event listener (which can only change the value
  // of inProgress to false)
  private def checkInProgress(): Unit = {
    val p = api.checkRecording(rec).get
    if (!p) inProgress = p
  }

  private def setupTransferChannel(): TransferChannel = {
    val fto = MythFileTransferObject(controlConn, dataConn)
    new TransferChannel(fto, dataConn)
  }

  final def transferChannel: RecordingTransferChannel = xferChannel

  /* We listen for DoneRecording event rather than RecordingFinished because
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
      case UpdateFileSizeEvent(recordedId, newSize) =>
        if (recordedId == rec.recordedId)
          xferChannel.updateSize(newSize.bytes)
      case RecordingListUpdateEvent(r: Recording) =>
        if (r.chanId == rec.chanId && r.startTime == rec.startTime)
          rec = r
      case DoneRecordingEvent(cardId, _, _) =>
        if (cardId == usingCardId) {
          inProgress = false
          xferChannel.signalSizeChanged()
          // TODO remove listener?
        }
      case _ => ()
    }
  }

  class TransferChannel(controlChannel: FileTransferAPI, dataChannel: FileTransferConnection)
    extends FileTransferChannelImpl(controlChannel, dataChannel)
       with WaitableFileTransferChannel
       with RecordingTransferChannel {

    override def close(): Unit = {
      outer.close()
      super.close()
    }

    final def updateSize(sz: Long): Unit = {
      currentSize = sz
      signalSizeChanged()
    }

    override final def isInProgress: Boolean = inProgress

    final def isRecordingInProgress: Boolean = inProgress

    final def recording: Recording = rec
  }
}

object RecordingTransferChannel {
  def apply(api: MythProtocolAPIConnection, chanId: ChanId, recStartTs: MythDateTime): RecordingTransferChannel = {
    val rec = api.queryRecording(chanId, recStartTs).get   // TODO check for failure/not found, wrap exception?
    apply(api, rec)
  }

  def apply(api: MythProtocolAPIConnection, rec: Recording): RecordingTransferChannel = {
    new RecordingTransfer(api, rec).transferChannel
  }
}
