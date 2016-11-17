package mythtv
package connection
package myth

import model._
import model.EnumTypes._
import util.MythDateTime
import MythProtocol.MythProtocolFailure

trait RecorderAPI {
  def cancelNextRecording(cancel: Boolean): Unit
  def changeBrightness(adjType: PictureAdjustType, up: Boolean): MythProtocolResult[Int]
  def changeChannel(dir: ChannelChangeDirection): Unit
  def changeColour(adjType: PictureAdjustType, up: Boolean): MythProtocolResult[Int]
  def changeContrast(adjType: PictureAdjustType, up: Boolean): MythProtocolResult[Int]
  def changeHue(adjType: PictureAdjustType, up: Boolean): MythProtocolResult[Int]
  def checkChannel(channum: ChannelNumber): MythProtocolResult[Boolean]
  def checkChannelPrefix(channumPrefix: ChannelNumber): MythProtocolResult[(Boolean, Option[CaptureCardId], Boolean, String)]

  // This returns a map from frame number to duration, what is that???
  def fillDurationMap(start: VideoPositionFrame, end: VideoPositionFrame): MythProtocolResult[Map[VideoPositionFrame, Long]]

  // This returns a map from frame number to file byte offset
  def fillPositionMap(start: VideoPositionFrame, end: VideoPositionFrame): MythProtocolResult[Map[VideoPositionFrame, Long]]

  def finishRecording(): Unit
  def frontendReady(): Unit
  def getBrightness: MythProtocolResult[Int]
  def getChannelInfo(chanId: ChanId): MythProtocolResult[Channel]
  def getColour: MythProtocolResult[Int]
  def getContrast: MythProtocolResult[Int]
  def getCurrentRecording: MythProtocolResult[Recording]
  def getFilePosition: MythProtocolResult[Long]
  def getFrameRate: MythProtocolResult[Double]
  def getFramesWritten: MythProtocolResult[Long]
  def getFreeInputs(excludedCards: CaptureCardId*): MythProtocolResult[List[CardInput]]
  def getHue: MythProtocolResult[Int]
  def getInput: MythProtocolResult[String]
  // This returns byte offset from the approximate keyframe position
  def getKeyframePos(desiredPos: VideoPositionFrame): MythProtocolResult[Long]
  def getMaxBitrate: MythProtocolResult[Long]
  def getNextProgramInfo(chanId: ChanId, dir: ChannelBrowseDirection, startTime: MythDateTime): MythProtocolResult[UpcomingProgram]
  def getNextProgramInfo(channum: ChannelNumber, dir: ChannelBrowseDirection, startTime: MythDateTime): MythProtocolResult[UpcomingProgram]
  def getRecording: MythProtocolResult[Recording]
  def isRecording: MythProtocolResult[Boolean]
  def pause(): Unit
  // NB Must call pause before setChannel
  def setChannel(channum: ChannelNumber): Unit
  def setInput(inputName: String): MythProtocolResult[String]
  // NB the recordingState parameter is ignored by the backend implementation
  def setLiveRecording(recordingState: Int): Unit
  def setSignalMonitoringRate(rate: Int, notifyFrontend: Boolean): MythProtocolResult[Boolean]
  def shouldSwitchCard(chanId: ChanId): MythProtocolResult[Boolean]
  // FIXME when I invoked spawnLiveTV during testing, it caused SIGABRT on the backend !!
  def spawnLiveTV(usePiP: Boolean, channumStart: ChannelNumber): Unit
  def stopLiveTV(): Unit
  def toggleChannelFavorite(channelGroup: String): Unit
}

// Forwarders to implementation in MythProtocolAPI
trait RecorderAPILike extends RemoteEncoder with RecorderAPI {
  protected def protoApi: MythProtocolAPI

  def cancelNextRecording(cancel: Boolean): Unit =
    protoApi.queryRecorderCancelNextRecording(cardId, cancel)

  def changeBrightness(adjType: PictureAdjustType, up: Boolean): MythProtocolResult[Int] =
    protoApi.queryRecorderChangeBrightness(cardId, adjType, up)

  def changeChannel(dir: ChannelChangeDirection): Unit =
    protoApi.queryRecorderChangeChannel(cardId, dir)

  def changeColour(adjType: PictureAdjustType, up: Boolean): MythProtocolResult[Int] =
    protoApi.queryRecorderChangeColour(cardId, adjType, up)

  def changeContrast(adjType: PictureAdjustType, up: Boolean): MythProtocolResult[Int] =
    protoApi.queryRecorderChangeContrast(cardId, adjType, up)

  def changeHue(adjType: PictureAdjustType, up: Boolean): MythProtocolResult[Int] =
    protoApi.queryRecorderChangeHue(cardId, adjType, up)

  def checkChannel(channum: ChannelNumber): MythProtocolResult[Boolean] =
    protoApi.queryRecorderCheckChannel(cardId, channum)

  def checkChannelPrefix(channumPrefix: ChannelNumber): MythProtocolResult[(Boolean, Option[CaptureCardId], Boolean, String)] =
    protoApi.queryRecorderCheckChannelPrefix(cardId, channumPrefix)

  def fillDurationMap(start: VideoPositionFrame, end: VideoPositionFrame): MythProtocolResult[Map[VideoPositionFrame, Long]] =
    protoApi.queryRecorderFillDurationMap(cardId, start, end)

  def fillPositionMap(start: VideoPositionFrame, end: VideoPositionFrame): MythProtocolResult[Map[VideoPositionFrame, Long]] =
    protoApi.queryRecorderFillPositionMap(cardId, start, end)

  def finishRecording(): Unit =
    protoApi.queryRecorderFinishRecording(cardId)

  def frontendReady(): Unit =
    protoApi.queryRecorderFrontendReady(cardId)

  def getBrightness: MythProtocolResult[Int] =
    protoApi.queryRecorderGetBrightness(cardId)

  def getChannelInfo(chanId: ChanId): MythProtocolResult[Channel] =
    protoApi.queryRecorderGetChannelInfo(cardId, chanId)

  def getColour: MythProtocolResult[Int] =
    protoApi.queryRecorderGetColour(cardId)

  def getContrast: MythProtocolResult[Int] =
    protoApi.queryRecorderGetContrast(cardId)

  def getCurrentRecording: MythProtocolResult[Recording] =
    protoApi.queryRecorderGetCurrentRecording(cardId)

  def getFilePosition: MythProtocolResult[Long] =
    protoApi.queryRecorderGetFilePosition(cardId)

  def getFrameRate: MythProtocolResult[Double] =
    protoApi.queryRecorderGetFrameRate(cardId)

  def getFramesWritten: MythProtocolResult[Long] =
    protoApi.queryRecorderGetFramesWritten(cardId)

  def getFreeInputs(excludedCards: CaptureCardId*): MythProtocolResult[List[CardInput]] =
    protoApi.queryRecorderGetFreeInputs(cardId, excludedCards: _*)

  def getHue: MythProtocolResult[Int] =
    protoApi.queryRecorderGetHue(cardId)

  def getInput: MythProtocolResult[String] =
    protoApi.queryRecorderGetInput(cardId)

  def getKeyframePos(desiredPos: VideoPositionFrame): MythProtocolResult[Long] =
    protoApi.queryRecorderGetKeyframePos(cardId, desiredPos)

  def getMaxBitrate: MythProtocolResult[Long] =
    protoApi.queryRecorderGetMaxBitrate(cardId)

  def getNextProgramInfo(chanId: ChanId, dir: ChannelBrowseDirection, startTime: MythDateTime): MythProtocolResult[UpcomingProgram] =
    protoApi.queryRecorderGetNextProgramInfo(cardId, chanId, dir, startTime)

  def getNextProgramInfo(channum: ChannelNumber, dir: ChannelBrowseDirection, startTime: MythDateTime): MythProtocolResult[UpcomingProgram] =
    protoApi.queryRecorderGetNextProgramInfo(cardId, channum, dir, startTime)

  def getRecording: MythProtocolResult[Recording] =
    protoApi.queryRecorderGetRecording(cardId)

  def isRecording: MythProtocolResult[Boolean] =
    protoApi.queryRecorderIsRecording(cardId)

  def pause(): Unit =
    protoApi.queryRecorderPause(cardId)

  def setChannel(channum: ChannelNumber): Unit =
    protoApi.queryRecorderSetChannel(cardId, channum)

  def setInput(inputName: String): MythProtocolResult[String] =
    protoApi.queryRecorderSetInput(cardId, inputName)

  def setLiveRecording(recordingState: Int): Unit =
    protoApi.queryRecorderSetLiveRecording(cardId, recordingState)

  def setSignalMonitoringRate(rate: Int, notifyFrontend: Boolean): MythProtocolResult[Boolean] =
    protoApi.queryRecorderSetSignalMonitoringRate(cardId, rate, notifyFrontend)

  def shouldSwitchCard(chanId: ChanId): MythProtocolResult[Boolean] =
    protoApi.queryRecorderShouldSwitchCard(cardId, chanId)

  def spawnLiveTV(usePiP: Boolean, channumStart: ChannelNumber): Unit =
    protoApi.queryRecorderSpawnLiveTV(cardId, usePiP, channumStart)

  def stopLiveTV(): Unit =
    protoApi.queryRecorderStopLiveTV(cardId)

  def toggleChannelFavorite(channelGroup: String): Unit =
    protoApi.queryRecorderToggleChannelFavorite(cardId, channelGroup)
}
