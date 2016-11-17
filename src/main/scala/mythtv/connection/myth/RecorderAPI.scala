package mythtv
package connection
package myth

import model._
import model.EnumTypes._
import util.MythDateTime
import MythProtocol.MythProtocolFailure

trait RecorderAPI {
  def cancelNextRecording(cancel: Boolean): Unit
  def changeBrightness(adjType: PictureAdjustType, up: Boolean): Either[MythProtocolFailure, Int]
  def changeChannel(dir: ChannelChangeDirection): Unit
  def changeColour(adjType: PictureAdjustType, up: Boolean): Either[MythProtocolFailure, Int]
  def changeContrast(adjType: PictureAdjustType, up: Boolean): Either[MythProtocolFailure, Int]
  def changeHue(adjType: PictureAdjustType, up: Boolean): Either[MythProtocolFailure, Int]
  def checkChannel(channum: ChannelNumber): Either[MythProtocolFailure, Boolean]
  def checkChannelPrefix(channumPrefix: ChannelNumber): Either[MythProtocolFailure, (Boolean, Option[CaptureCardId], Boolean, String)]

  // This returns a map from frame number to duration, what is that???
  def fillDurationMap(start: VideoPositionFrame, end: VideoPositionFrame): Either[MythProtocolFailure, Map[VideoPositionFrame, Long]]

  // This returns a map from frame number to file byte offset
  def fillPositionMap(start: VideoPositionFrame, end: VideoPositionFrame): Either[MythProtocolFailure, Map[VideoPositionFrame, Long]]

  def finishRecording(): Unit
  def frontendReady(): Unit
  def getBrightness: Either[MythProtocolFailure, Int]
  def getChannelInfo(chanId: ChanId): Either[MythProtocolFailure, Channel]
  def getColour: Either[MythProtocolFailure, Int]
  def getContrast: Either[MythProtocolFailure, Int]
  def getCurrentRecording: Either[MythProtocolFailure, Recording]
  def getFilePosition: Either[MythProtocolFailure, Long]
  def getFrameRate: Either[MythProtocolFailure, Double]
  def getFramesWritten: Either[MythProtocolFailure, Long]
  def getFreeInputs(excludedCards: CaptureCardId*): Either[MythProtocolFailure, List[CardInput]]
  def getHue: Either[MythProtocolFailure, Int]
  def getInput: Either[MythProtocolFailure, String]
  // This returns byte offset from the approximate keyframe position
  def getKeyframePos(desiredPos: VideoPositionFrame): Either[MythProtocolFailure, Long]
  def getMaxBitrate: Either[MythProtocolFailure, Long]
  def getNextProgramInfo(chanId: ChanId, dir: ChannelBrowseDirection, startTime: MythDateTime): Either[MythProtocolFailure, UpcomingProgram]
  def getNextProgramInfo(channum: ChannelNumber, dir: ChannelBrowseDirection, startTime: MythDateTime): Either[MythProtocolFailure, UpcomingProgram]
  def getRecording: Either[MythProtocolFailure, Recording]
  def isRecording: Either[MythProtocolFailure, Boolean]
  def pause(): Unit
  // NB Must call pause before setChannel
  def setChannel(channum: ChannelNumber): Unit
  def setInput(inputName: String): Either[MythProtocolFailure, String]
  // NB the recordingState parameter is ignored by the backend implementation
  def setLiveRecording(recordingState: Int): Unit
  def setSignalMonitoringRate(rate: Int, notifyFrontend: Boolean): Either[MythProtocolFailure, Boolean]
  def shouldSwitchCard(chanId: ChanId): Either[MythProtocolFailure, Boolean]
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

  def changeBrightness(adjType: PictureAdjustType, up: Boolean): Either[MythProtocolFailure, Int] =
    protoApi.queryRecorderChangeBrightness(cardId, adjType, up)

  def changeChannel(dir: ChannelChangeDirection): Unit =
    protoApi.queryRecorderChangeChannel(cardId, dir)

  def changeColour(adjType: PictureAdjustType, up: Boolean): Either[MythProtocolFailure, Int] =
    protoApi.queryRecorderChangeColour(cardId, adjType, up)

  def changeContrast(adjType: PictureAdjustType, up: Boolean): Either[MythProtocolFailure, Int] =
    protoApi.queryRecorderChangeContrast(cardId, adjType, up)

  def changeHue(adjType: PictureAdjustType, up: Boolean): Either[MythProtocolFailure, Int] =
    protoApi.queryRecorderChangeHue(cardId, adjType, up)

  def checkChannel(channum: ChannelNumber): Either[MythProtocolFailure, Boolean] =
    protoApi.queryRecorderCheckChannel(cardId, channum)

  def checkChannelPrefix(channumPrefix: ChannelNumber): Either[MythProtocolFailure, (Boolean, Option[CaptureCardId], Boolean, String)] =
    protoApi.queryRecorderCheckChannelPrefix(cardId, channumPrefix)

  def fillDurationMap(start: VideoPositionFrame, end: VideoPositionFrame): Either[MythProtocolFailure, Map[VideoPositionFrame, Long]] =
    protoApi.queryRecorderFillDurationMap(cardId, start, end)

  def fillPositionMap(start: VideoPositionFrame, end: VideoPositionFrame): Either[MythProtocolFailure, Map[VideoPositionFrame, Long]] =
    protoApi.queryRecorderFillPositionMap(cardId, start, end)

  def finishRecording(): Unit =
    protoApi.queryRecorderFinishRecording(cardId)

  def frontendReady(): Unit =
    protoApi.queryRecorderFrontendReady(cardId)

  def getBrightness: Either[MythProtocolFailure, Int] =
    protoApi.queryRecorderGetBrightness(cardId)

  def getChannelInfo(chanId: ChanId): Either[MythProtocolFailure, Channel] =
    protoApi.queryRecorderGetChannelInfo(cardId, chanId)

  def getColour: Either[MythProtocolFailure, Int] =
    protoApi.queryRecorderGetColour(cardId)

  def getContrast: Either[MythProtocolFailure, Int] =
    protoApi.queryRecorderGetContrast(cardId)

  def getCurrentRecording: Either[MythProtocolFailure, Recording] =
    protoApi.queryRecorderGetCurrentRecording(cardId)

  def getFilePosition: Either[MythProtocolFailure, Long] =
    protoApi.queryRecorderGetFilePosition(cardId)

  def getFrameRate: Either[MythProtocolFailure, Double] =
    protoApi.queryRecorderGetFrameRate(cardId)

  def getFramesWritten: Either[MythProtocolFailure, Long] =
    protoApi.queryRecorderGetFramesWritten(cardId)

  def getFreeInputs(excludedCards: CaptureCardId*): Either[MythProtocolFailure, List[CardInput]] =
    protoApi.queryRecorderGetFreeInputs(cardId, excludedCards: _*)

  def getHue: Either[MythProtocolFailure, Int] =
    protoApi.queryRecorderGetHue(cardId)

  def getInput: Either[MythProtocolFailure, String] =
    protoApi.queryRecorderGetInput(cardId)

  def getKeyframePos(desiredPos: VideoPositionFrame): Either[MythProtocolFailure, Long] =
    protoApi.queryRecorderGetKeyframePos(cardId, desiredPos)

  def getMaxBitrate: Either[MythProtocolFailure, Long] =
    protoApi.queryRecorderGetMaxBitrate(cardId)

  def getNextProgramInfo(chanId: ChanId, dir: ChannelBrowseDirection, startTime: MythDateTime): Either[MythProtocolFailure, UpcomingProgram] =
    protoApi.queryRecorderGetNextProgramInfo(cardId, chanId, dir, startTime)

  def getNextProgramInfo(channum: ChannelNumber, dir: ChannelBrowseDirection, startTime: MythDateTime): Either[MythProtocolFailure, UpcomingProgram] =
    protoApi.queryRecorderGetNextProgramInfo(cardId, channum, dir, startTime)

  def getRecording: Either[MythProtocolFailure, Recording] =
    protoApi.queryRecorderGetRecording(cardId)

  def isRecording: Either[MythProtocolFailure, Boolean] =
    protoApi.queryRecorderIsRecording(cardId)

  def pause(): Unit =
    protoApi.queryRecorderPause(cardId)

  def setChannel(channum: ChannelNumber): Unit =
    protoApi.queryRecorderSetChannel(cardId, channum)

  def setInput(inputName: String): Either[MythProtocolFailure, String] =
    protoApi.queryRecorderSetInput(cardId, inputName)

  def setLiveRecording(recordingState: Int): Unit =
    protoApi.queryRecorderSetLiveRecording(cardId, recordingState)

  def setSignalMonitoringRate(rate: Int, notifyFrontend: Boolean): Either[MythProtocolFailure, Boolean] =
    protoApi.queryRecorderSetSignalMonitoringRate(cardId, rate, notifyFrontend)

  def shouldSwitchCard(chanId: ChanId): Either[MythProtocolFailure, Boolean] =
    protoApi.queryRecorderShouldSwitchCard(cardId, chanId)

  def spawnLiveTV(usePiP: Boolean, channumStart: ChannelNumber): Unit =
    protoApi.queryRecorderSpawnLiveTV(cardId, usePiP, channumStart)

  def stopLiveTV(): Unit =
    protoApi.queryRecorderStopLiveTV(cardId)

  def toggleChannelFavorite(channelGroup: String): Unit =
    protoApi.queryRecorderToggleChannelFavorite(cardId, channelGroup)
}
