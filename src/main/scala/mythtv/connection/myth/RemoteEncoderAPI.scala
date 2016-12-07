package mythtv
package connection
package myth

import model._
import model.EnumTypes._

// TODO Factor out commonality with RecorderAPI ? what to call it?
//  (cancelNextRecording, getMaxBitrate, getCurrentRecording, getFreeInputs)

trait RemoteEncoderAPI {
  def cancelNextRecording(cancel: Boolean): MythProtocolResult[Unit]
  def getCurrentRecording: MythProtocolResult[Recording]
  @deprecated("use MythProtocolApi.getFreeInputInfo", "MythTV 0.28")
  def getFreeInputs(excludedCards: CaptureCardId*): MythProtocolResult[List[CardInput]]
  def getMaxBitrate: MythProtocolResult[Long]
  def getRecordingStatus: MythProtocolResult[RecStatus]
  def getState: MythProtocolResult[TvState]
  def getSleepStatus: MythProtocolResult[SleepStatus]
  // getFlags is equivalent to getState (at least for non-local recorders)? for local, need an Enumeration?
  def getFlags: MythProtocolResult[Int]
  def isBusy: MythProtocolResult[(Boolean, Option[CardInput], Option[ChanId])]
  // returns true if the card is busy or will be within the next timeBufferSeconds
  def isBusy(timeBufferSeconds: Int): MythProtocolResult[(Boolean, Option[CardInput], Option[ChanId])]
  def matchesRecording(rec: Recording): MythProtocolResult[Boolean]
  // TODO arguments to recordPending and startRecording should probably be Recordable?
  def recordPending(secondsLeft: Int, hasLaterShowing: Boolean, rec: Recording): MythProtocolResult[Unit]
  def startRecording(rec: Recording): MythProtocolResult[RecStatus]
  def stopRecording(): MythProtocolResult[Unit]
}


trait RemoteEncoderAPILike extends RemoteEncoder with RemoteEncoderAPI {
  protected def protoApi: MythProtocolAPI

  def cancelNextRecording(cancel: Boolean): MythProtocolResult[Unit] =
    protoApi.queryRemoteEncoderCancelNextRecording(cardId, cancel)

  def getCurrentRecording: MythProtocolResult[Recording] =
    protoApi.queryRemoteEncoderGetCurrentRecording(cardId)

  def getFreeInputs(excludedCards: CaptureCardId*): MythProtocolResult[List[CardInput]] =
    protoApi.queryRemoteEncoderGetFreeInputs(cardId, excludedCards: _*)

  def getMaxBitrate: MythProtocolResult[Long] =
    protoApi.queryRemoteEncoderGetMaxBitrate(cardId)

  def getRecordingStatus: MythProtocolResult[RecStatus] =
    protoApi.queryRemoteEncoderGetRecordingStatus(cardId)

  def getState: MythProtocolResult[TvState] =
    protoApi.queryRemoteEncoderGetState(cardId)

  def getSleepStatus: MythProtocolResult[SleepStatus] =
    protoApi.queryRemoteEncoderGetSleepStatus(cardId)

  def getFlags: MythProtocolResult[Int] =
    protoApi.queryRemoteEncoderGetFlags(cardId)

  def isBusy: MythProtocolResult[(Boolean, Option[CardInput], Option[ChanId])] =
    protoApi.queryRemoteEncoderIsBusy(cardId)

  def isBusy(timeBufferSeconds: Int): MythProtocolResult[(Boolean, Option[CardInput], Option[ChanId])] =
    protoApi.queryRemoteEncoderIsBusy(cardId, timeBufferSeconds)

  def matchesRecording(rec: Recording): MythProtocolResult[Boolean] =
    protoApi.queryRemoteEncoderMatchesRecording(cardId, rec)

  def recordPending(secondsLeft: Int, hasLaterShowing: Boolean, rec: Recording): MythProtocolResult[Unit] =
    protoApi.queryRemoteEncoderRecordPending(cardId, secondsLeft, hasLaterShowing, rec)

  def startRecording(rec: Recording): MythProtocolResult[RecStatus] =
    protoApi.queryRemoteEncoderStartRecording(cardId, rec)

  def stopRecording(): MythProtocolResult[Unit] =
    protoApi.queryRemoteEncoderStopRecording(cardId)
}
