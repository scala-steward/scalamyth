package mythtv
package connection
package myth

import model._
import model.EnumTypes._
import util.MythDateTime

trait RecorderAPI {
  def cancelNextRecording(cancel: Boolean): Unit
  def changeBrightness(adjType: PictureAdjustType, up: Boolean): Int
  def changeChannel(dir: ChannelChangeDirection): Unit
  def changeColour(adjType: PictureAdjustType, up: Boolean): Int
  def changeContrast(adjType: PictureAdjustType, up: Boolean): Int
  def changeHue(adjType: PictureAdjustType, up: Boolean): Int
  def checkChannel(channum: ChannelNumber): Boolean
  def checkChannelPrefix(channumPrefix: ChannelNumber): (Boolean, Option[CaptureCardId], Boolean, String)

  // This returns a map from frame number to duration, what is that???
  def fillDurationMap(start: VideoPositionFrame, end: VideoPositionFrame): Map[VideoPositionFrame, Long]

  // This returns a map from frame number to file byte offset
  def fillPositionMap(start: VideoPositionFrame, end: VideoPositionFrame): Map[VideoPositionFrame, Long]

  def finishRecording(): Unit
  def frontendReady(): Unit
  def getBrightness: Int
  def getChannelInfo(chanId: ChanId): Channel
  def getColour: Int
  def getContrast: Int
  def getCurrentRecording: Recording
  def getFilePosition: Long
  def getFrameRate: Double
  def getFramesWritten: Long
  def getFreeInputs(excludedCards: CaptureCardId*): List[CardInput]
  def getHue: Int
  def getInput: String
  // This returns byte offset from the approximate keyframe position
  def getKeyframePos(desiredPos: VideoPositionFrame): Long
  def getMaxBitrate: Long
  def getNextProgramInfo(chanId: ChanId, dir: ChannelBrowseDirection, startTime: MythDateTime): UpcomingProgram
  def getNextProgramInfo(channum: ChannelNumber, dir: ChannelBrowseDirection, startTime: MythDateTime): UpcomingProgram
  def getRecording: Recording
  def isRecording: Boolean
  def pause(): Unit
  // NB Must call pause before setChannel
  def setChannel(channum: ChannelNumber): Unit
  def setInput(inputName: String): String
  // NB the recordingState parameter is ignored by the backend implementation
  def setLiveRecording(recordingState: Int): Unit
  def setSignalMonitoringRate(rate: Int, notifyFrontend: Boolean): Boolean
  def shouldSwitchCard(chanId: ChanId): Boolean
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

  def changeBrightness(adjType: PictureAdjustType, up: Boolean): Int =
    protoApi.queryRecorderChangeBrightness(cardId, adjType, up)

  def changeChannel(dir: ChannelChangeDirection): Unit =
    protoApi.queryRecorderChangeChannel(cardId, dir)

  def changeColour(adjType: PictureAdjustType, up: Boolean): Int =
    protoApi.queryRecorderChangeColour(cardId, adjType, up)

  def changeContrast(adjType: PictureAdjustType, up: Boolean): Int =
    protoApi.queryRecorderChangeContrast(cardId, adjType, up)

  def changeHue(adjType: PictureAdjustType, up: Boolean): Int =
    protoApi.queryRecorderChangeHue(cardId, adjType, up)

  def checkChannel(channum: ChannelNumber): Boolean =
    protoApi.queryRecorderCheckChannel(cardId, channum)

  def checkChannelPrefix(channumPrefix: ChannelNumber): (Boolean, Option[CaptureCardId], Boolean, String) =
    protoApi.queryRecorderCheckChannelPrefix(cardId, channumPrefix)

  def fillDurationMap(start: VideoPositionFrame, end: VideoPositionFrame): Map[VideoPositionFrame, Long] =
    protoApi.queryRecorderFillDurationMap(cardId, start, end)

  def fillPositionMap(start: VideoPositionFrame, end: VideoPositionFrame): Map[VideoPositionFrame, Long] =
    protoApi.queryRecorderFillPositionMap(cardId, start, end)

  def finishRecording(): Unit =
    protoApi.queryRecorderFinishRecording(cardId)

  def frontendReady(): Unit =
    protoApi.queryRecorderFrontendReady(cardId)

  def getBrightness: Int =
    protoApi.queryRecorderGetBrightness(cardId)

  def getChannelInfo(chanId: ChanId): Channel =
    protoApi.queryRecorderGetChannelInfo(cardId, chanId)

  def getColour: Int =
    protoApi.queryRecorderGetColour(cardId)

  def getContrast: Int =
    protoApi.queryRecorderGetContrast(cardId)

  def getCurrentRecording: Recording =
    protoApi.queryRecorderGetCurrentRecording(cardId)

  def getFilePosition: Long =
    protoApi.queryRecorderGetFilePosition(cardId)

  def getFrameRate: Double =
    protoApi.queryRecorderGetFrameRate(cardId)

  def getFramesWritten: Long =
    protoApi.queryRecorderGetFramesWritten(cardId)

  def getFreeInputs(excludedCards: CaptureCardId*): List[CardInput] =
    protoApi.queryRecorderGetFreeInputs(cardId, excludedCards: _*)

  def getHue: Int =
    protoApi.queryRecorderGetHue(cardId)

  def getInput: String =
    protoApi.queryRecorderGetInput(cardId)

  def getKeyframePos(desiredPos: VideoPositionFrame): Long =
    protoApi.queryRecorderGetKeyframePos(cardId, desiredPos)

  def getMaxBitrate: Long =
    protoApi.queryRecorderGetMaxBitrate(cardId)

  def getNextProgramInfo(chanId: ChanId, dir: ChannelBrowseDirection, startTime: MythDateTime): UpcomingProgram =
    protoApi.queryRecorderGetNextProgramInfo(cardId, chanId, dir, startTime)

  def getNextProgramInfo(channum: ChannelNumber, dir: ChannelBrowseDirection, startTime: MythDateTime): UpcomingProgram =
    protoApi.queryRecorderGetNextProgramInfo(cardId, channum, dir, startTime)

  def getRecording: Recording =
    protoApi.queryRecorderGetRecording(cardId)

  def isRecording: Boolean =
    protoApi.queryRecorderIsRecording(cardId)

  def pause(): Unit =
    protoApi.queryRecorderPause(cardId)

  def setChannel(channum: ChannelNumber): Unit =
    protoApi.queryRecorderSetChannel(cardId, channum)

  def setInput(inputName: String): String =
    protoApi.queryRecorderSetInput(cardId, inputName)

  def setLiveRecording(recordingState: Int): Unit =
    protoApi.queryRecorderSetLiveRecording(cardId, recordingState)

  def setSignalMonitoringRate(rate: Int, notifyFrontend: Boolean): Boolean =
    protoApi.queryRecorderSetSignalMonitoringRate(cardId, rate, notifyFrontend)

  def shouldSwitchCard(chanId: ChanId): Boolean =
    protoApi.queryRecorderShouldSwitchCard(cardId, chanId)

  def spawnLiveTV(usePiP: Boolean, channumStart: ChannelNumber): Unit =
    protoApi.queryRecorderSpawnLiveTV(cardId, usePiP, channumStart)

  def stopLiveTV(): Unit =
    protoApi.queryRecorderStopLiveTV(cardId)

  def toggleChannelFavorite(channelGroup: String): Unit =
    protoApi.queryRecorderToggleChannelFavorite(cardId, channelGroup)
}
