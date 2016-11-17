package mythtv
package connection
package myth

import java.time.Duration

import model._
import model.EnumTypes._
import util.{ ByteCount, ExpectedCountIterator, FileStats, MythDateTime, MythFileHash }
import EnumTypes.{ MythLogLevel, MythProtocolEventMode, SeekWhence }
import MythProtocol.MythProtocolFailure

/**
  * A strongly-typed, thin API wrapper over the MythProtocol commands.
  */
trait MythProtocolAPI {
  def allowShutdown(): Either[MythProtocolFailure, Boolean]
  def announce(mode: String, hostName: String = "", eventMode: MythProtocolEventMode = MythProtocolEventMode.None): Either[MythProtocolFailure, Boolean]
  // TODO: do we want default value of useReadAhead to be true or false?
  def announceFileTransfer(hostName: String, fileName: String, storageGroup: String,
    writeMode: Boolean = false, useReadAhead: Boolean = true, timeout: Duration = Duration.ofSeconds(2)): Either[MythProtocolFailure, (FileTransferId, ByteCount)]
  // TODO SlaveBackend announce (more complex)
  def blockShutdown(): Either[MythProtocolFailure, Boolean]
  def checkRecording(rec: Recording): Either[MythProtocolFailure, Boolean]
  def deleteFile(fileName: String, storageGroup: String): Either[MythProtocolFailure, Boolean]
  def deleteRecording(rec: Recording): Either[MythProtocolFailure, Int]
  def deleteRecording(chanId: ChanId, startTime: MythDateTime): Either[MythProtocolFailure, Int]
  def deleteRecording(chanId: ChanId, startTime: MythDateTime, forceDeleteMetadata: Boolean = false, forgetHistory: Boolean = false): Either[MythProtocolFailure, Int]
  def done(): Unit
  def fillProgramInfo(playbackHost: String, p: Recording): Either[MythProtocolFailure, Recording]
  def forceDeleteRecording(rec: Recording): Either[MythProtocolFailure, Int]
  def forgetRecording(rec: Recording): Either[MythProtocolFailure, Int]   // TODO something better to indicate success/failure; Either?
  def freeTuner(cardId: CaptureCardId): Either[MythProtocolFailure, Boolean]
  def getFreeRecorder: Either[MythProtocolFailure, RemoteEncoder]
  def getFreeRecorderCount: Either[MythProtocolFailure, Int]
  def getFreeRecorderList: Either[MythProtocolFailure, List[CaptureCardId]]
  def getNextFreeRecorder(cardId: CaptureCardId): Either[MythProtocolFailure, RemoteEncoder]
  def getRecorderFromNum(cardId: CaptureCardId): Either[MythProtocolFailure, RemoteEncoder]
  def getRecorderNum(rec: Recording): Either[MythProtocolFailure, RemoteEncoder]
  def goToSleep(): Either[MythProtocolFailure, Boolean]  // TODO a way to return error message if any
  def lockTuner(): Any // TODO capture the appropriate return type
  def lockTuner(cardId: CaptureCardId): Any // see above for return type
  def message(message: String, extra: String*): Either[MythProtocolFailure, Boolean]
  def messageSetLogLevel(logLevel: MythLogLevel): Either[MythProtocolFailure, Boolean]
  def messageSetVerbose(verboseMask: String): Either[MythProtocolFailure, Boolean]
  def protocolVersion(ver: Int, token: String): Either[MythProtocolFailure, (Boolean, Int)]
  def queryActiveBackends: Either[MythProtocolFailure, List[String]]
  def queryBookmark(chanId: ChanId, startTime: MythDateTime): Either[MythProtocolFailure, VideoPositionFrame]
  def queryCheckFile(rec: Recording, checkSlaves: Boolean = true): Either[MythProtocolFailure, String]
  def queryCommBreak(chanId: ChanId, startTime: MythDateTime): Either[MythProtocolFailure, List[VideoSegment]]
  def queryCutList(chanId: ChanId, startTime: MythDateTime): Either[MythProtocolFailure, List[VideoSegment]]
  def queryFileExists(fileName: String, storageGroup: String = ""): Either[MythProtocolFailure, (String, FileStats)]
  def queryFileHash(fileName: String, storageGroup: String, hostName: String = ""): Either[MythProtocolFailure, MythFileHash]
  def queryFileTransferDone(ftId: FileTransferId): Unit
  def queryFileTransferIsOpen(ftId: FileTransferId): Either[MythProtocolFailure, Boolean]
  def queryFileTransferReopen(ftId: FileTransferId, newFileName: String): Either[MythProtocolFailure, Boolean]
  def queryFileTransferRequestBlock(ftId: FileTransferId, blockSize: Int): Either[MythProtocolFailure, Int]
  def queryFileTransferRequestSize(ftId: FileTransferId): Either[MythProtocolFailure, Long]
  def queryFileTransferSeek(ftId: FileTransferId, pos: Long, whence: SeekWhence, currentPos: Long): Either[MythProtocolFailure, Long]
  def queryFileTransferSetTimeout(ftId: FileTransferId, fast: Boolean): Unit
  def queryFileTransferWriteBlock(ftId: FileTransferId, blockSize: Int): Either[MythProtocolFailure, Int]
  def queryFreeSpace: Either[MythProtocolFailure, List[FreeSpace]]
  def queryFreeSpaceList: Either[MythProtocolFailure, List[FreeSpace]]
  def queryFreeSpaceSummary: Either[MythProtocolFailure, (ByteCount, ByteCount)]
  def queryGenPixmap(rec: Recording, token: String = "", time: VideoPosition = VideoPositionSeconds(-1),
    outputFile: String = "", width: Int = 0, height: Int = 0): Either[MythProtocolFailure, Boolean]
  def queryGetAllPending: Either[MythProtocolFailure, ExpectedCountIterator[Recording]]
  def queryGetAllScheduled: Either[MythProtocolFailure, ExpectedCountIterator[Recording]]
  def queryGetConflicting(rec: Recording): Either[MythProtocolFailure, ExpectedCountIterator[Recording]]  // TODO should parameter really be a Recordable?
  def queryGetExpiring: Either[MythProtocolFailure, ExpectedCountIterator[Recording]]
  def queryGuideDataThrough: Either[MythProtocolFailure, MythDateTime]
  def queryHostname: Either[MythProtocolFailure, String]
  def queryIsActiveBackend(hostName: String): Either[MythProtocolFailure, Boolean]
  def queryIsRecording: Either[MythProtocolFailure, (Int, Int)]
  def queryLoad: Either[MythProtocolFailure, (Double, Double, Double)]
  def queryMemStats: Either[MythProtocolFailure, (ByteCount, ByteCount, ByteCount, ByteCount)]
  def queryPixmapGetIfModified(maxFileSize: Long, rec: Recording): Either[MythProtocolFailure, (MythDateTime, Option[PixmapInfo])]
  def queryPixmapGetIfModified(modifiedSince: MythDateTime, maxFileSize: Long, rec: Recording): Either[MythProtocolFailure, (MythDateTime, Option[PixmapInfo])]
  def queryPixmapLastModified(rec: Recording): Either[MythProtocolFailure, MythDateTime]
  def queryRecorderCancelNextRecording(cardId: CaptureCardId, cancel: Boolean): Unit
  def queryRecorderChangeBrightness(cardId: CaptureCardId, adjType: PictureAdjustType, up: Boolean): Either[MythProtocolFailure, Int]
  def queryRecorderChangeChannel(cardId: CaptureCardId, dir: ChannelChangeDirection): Unit
  def queryRecorderChangeColour(cardId: CaptureCardId, adjType: PictureAdjustType, up: Boolean): Either[MythProtocolFailure, Int]
  def queryRecorderChangeContrast(cardId: CaptureCardId, adjType: PictureAdjustType, up: Boolean): Either[MythProtocolFailure, Int]
  def queryRecorderChangeHue(cardId: CaptureCardId, adjType: PictureAdjustType, up: Boolean): Either[MythProtocolFailure, Int]
  def queryRecorderCheckChannel(cardId: CaptureCardId, channum: ChannelNumber): Either[MythProtocolFailure, Boolean]
  def queryRecorderCheckChannelPrefix(cardId: CaptureCardId, channumPrefix: ChannelNumber): Either[MythProtocolFailure, (Boolean, Option[CaptureCardId], Boolean, String)]
  // This returns a map from frame number to duration, what is that???
  def queryRecorderFillDurationMap(cardId: CaptureCardId, start: VideoPositionFrame, end: VideoPositionFrame): Either[MythProtocolFailure, Map[VideoPositionFrame, Long]]
  // This returns a map from frame number to file byte offset
  def queryRecorderFillPositionMap(cardId: CaptureCardId, start: VideoPositionFrame, end: VideoPositionFrame): Either[MythProtocolFailure, Map[VideoPositionFrame, Long]]
  def queryRecorderFinishRecording(cardId: CaptureCardId): Unit
  def queryRecorderFrontendReady(cardId: CaptureCardId): Unit
  def queryRecorderGetBrightness(cardId: CaptureCardId): Either[MythProtocolFailure, Int]
  def queryRecorderGetChannelInfo(cardId: CaptureCardId, chanId: ChanId): Either[MythProtocolFailure, Channel]
  def queryRecorderGetColour(cardId: CaptureCardId): Either[MythProtocolFailure, Int]
  def queryRecorderGetContrast(cardId: CaptureCardId): Either[MythProtocolFailure, Int]
  def queryRecorderGetCurrentRecording(cardId: CaptureCardId): Either[MythProtocolFailure, Recording]
  def queryRecorderGetFilePosition(cardId: CaptureCardId): Either[MythProtocolFailure, Long]
  def queryRecorderGetFrameRate(cardId: CaptureCardId): Either[MythProtocolFailure, Double]
  def queryRecorderGetFramesWritten(cardId: CaptureCardId): Either[MythProtocolFailure, Long]
  def queryRecorderGetFreeInputs(cardId: CaptureCardId, excludedCards: CaptureCardId*): Either[MythProtocolFailure, List[CardInput]]
  def queryRecorderGetHue(cardId: CaptureCardId): Either[MythProtocolFailure, Int]
  def queryRecorderGetInput(cardId: CaptureCardId): Either[MythProtocolFailure, String]
  // This returns byte offset from the approximate keyframe position
  def queryRecorderGetKeyframePos(cardId: CaptureCardId, desiredPos: VideoPositionFrame): Either[MythProtocolFailure, Long]
  def queryRecorderGetMaxBitrate(cardId: CaptureCardId): Either[MythProtocolFailure, Long]
  def queryRecorderGetNextProgramInfo(cardId: CaptureCardId, chanId: ChanId, dir: ChannelBrowseDirection,
    startTime: MythDateTime): Either[MythProtocolFailure, UpcomingProgram]
  def queryRecorderGetNextProgramInfo(cardId: CaptureCardId, channum: ChannelNumber, dir: ChannelBrowseDirection,
    startTime: MythDateTime): Either[MythProtocolFailure, UpcomingProgram]
  def queryRecorderGetRecording(cardId: CaptureCardId): Either[MythProtocolFailure, Recording]
  def queryRecorderIsRecording(cardId: CaptureCardId): Either[MythProtocolFailure, Boolean]
  def queryRecorderPause(cardId: CaptureCardId): Unit
  // NB Must call queryRecorderPause before queryRecorderSetChannel
  def queryRecorderSetChannel(cardId: CaptureCardId, channum: ChannelNumber): Unit
  def queryRecorderSetInput(cardId: CaptureCardId, inputName: String): Either[MythProtocolFailure, String]
  // NB the recordingState parameter is ignored by the backend implementation
  def queryRecorderSetLiveRecording(cardId: CaptureCardId, recordingState: Int): Unit
  def queryRecorderSetSignalMonitoringRate(cardId: CaptureCardId, rate: Int, notifyFrontend: Boolean): Either[MythProtocolFailure, Boolean]
  def queryRecorderShouldSwitchCard(cardId: CaptureCardId, chanId: ChanId): Either[MythProtocolFailure, Boolean]
  // TODO FIXME when I invoked spawnLiveTV during testing, it caused SIGABRT on the backend !!
  def queryRecorderSpawnLiveTV(cardId: CaptureCardId, usePiP: Boolean, channumStart: ChannelNumber): Unit
  def queryRecorderStopLiveTV(cardId: CaptureCardId): Unit
  def queryRecorderToggleChannelFavorite(cardId: CaptureCardId, channelGroup: String): Unit
  def queryRecording(pathName: String): Either[MythProtocolFailure, Recording]
  def queryRecording(chanId: ChanId, startTime: MythDateTime): Either[MythProtocolFailure, Recording]
  def queryRecordings(specifier: String = "Unsorted"): Either[MythProtocolFailure, ExpectedCountIterator[Recording]]
  def querySetting(hostName: String, settingName: String): Either[MythProtocolFailure, String]
  def querySGGetFileList(hostName: String, storageGroup: String, path: String): Either[MythProtocolFailure, List[String]]
  def querySGGetFileList(hostName: String, storageGroup: String, path: String, fileNamesOnly: Boolean): Either[MythProtocolFailure, List[String]]
  def querySGFileQuery(hostName: String, storageGroup: String, fileName: String): Either[MythProtocolFailure, (String, MythDateTime, ByteCount)]
  def queryTimeZone: Either[MythProtocolFailure, TimeZoneInfo]
  def queryUptime: Either[MythProtocolFailure, Duration]
  def refreshBackend: Either[MythProtocolFailure, Boolean]
  def rescheduleRecordingsCheck(recStatus: RecStatus = RecStatus.Unknown, recordId: RecordRuleId = RecordRuleId(0), findId: Int = 0,
    title: String = "", subtitle: String = "", description: String = "", programId: String = "", reason: String = "Scala"): Either[MythProtocolFailure, Boolean]
  def rescheduleRecordingsMatch(recordId: RecordRuleId = RecordRuleId(0), sourceId: ListingSourceId = ListingSourceId(0),
    mplexId: MultiplexId = MultiplexId(0), maxStartTime: Option[MythDateTime] = None, reason: String = "Scala"): Either[MythProtocolFailure, Boolean]
  def rescheduleRecordingsPlace(reason: String = "Scala"): Either[MythProtocolFailure, Boolean]
  def scanVideos: Either[MythProtocolFailure, Boolean]
  def setBookmark(chanId: ChanId, startTime: MythDateTime, pos: VideoPositionFrame): Either[MythProtocolFailure, Boolean]
  def setSetting(hostName: String, settingName: String, value: String): Either[MythProtocolFailure, Boolean]
  def shutdownNow(haltCommand: String = ""): Unit
  def stopRecording(rec: Recording): Either[MythProtocolFailure, Int]  // TODO better encapsulate return codes
  def undeleteRecording(rec: Recording): Either[MythProtocolFailure, Boolean]
  def undeleteRecording(chanId: ChanId, startTime: MythDateTime): Either[MythProtocolFailure, Boolean]
  // TODO more methods
}
