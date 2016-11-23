package mythtv
package connection
package myth

import java.net.URI
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
  def allowShutdown(): MythProtocolResult[Boolean]
  def announce(mode: String, hostName: String = "", eventMode: MythProtocolEventMode = MythProtocolEventMode.None): MythProtocolResult[Boolean]
  def announceFileTransfer(hostName: String, fileName: String, storageGroup: String,
    writeMode: Boolean = false, useReadAhead: Boolean = false, timeout: Duration = Duration.ofSeconds(2)): MythProtocolResult[(FileTransferId, ByteCount)]
  // TODO SlaveBackend announce (more complex)
  def blockShutdown(): MythProtocolResult[Boolean]
  def checkRecording(rec: Recording): MythProtocolResult[Boolean]
  def deleteFile(fileName: String, storageGroup: String): MythProtocolResult[Boolean]
  def deleteRecording(rec: Recording): MythProtocolResult[Int]
  def deleteRecording(chanId: ChanId, startTime: MythDateTime): MythProtocolResult[Int]
  def deleteRecording(chanId: ChanId, startTime: MythDateTime, forceDeleteMetadata: Boolean = false, forgetHistory: Boolean = false): MythProtocolResult[Int]
  def done(): Unit
  def downloadFile(sourceUrl: URI, storageGroup: String, fileName: String = ""): MythProtocolResult[URI]
  def downloadFileNow(sourceUrl: URI, storageGroup: String, fileName: String = ""): MythProtocolResult[URI]
  def fillProgramInfo(playbackHost: String, p: Recording): MythProtocolResult[Recording]
  def forceDeleteRecording(rec: Recording): MythProtocolResult[Int]
  def forgetRecording(rec: Recording): MythProtocolResult[Boolean]
  def freeTuner(cardId: CaptureCardId): MythProtocolResult[Boolean]
  def getFreeRecorder: MythProtocolResult[RemoteEncoder]
  def getFreeRecorderCount: MythProtocolResult[Int]
  def getFreeRecorderList: MythProtocolResult[List[CaptureCardId]]
  def getNextFreeRecorder(cardId: CaptureCardId): MythProtocolResult[RemoteEncoder]
  def getRecorderFromNum(cardId: CaptureCardId): MythProtocolResult[RemoteEncoder]
  def getRecorderNum(rec: Recording): MythProtocolResult[RemoteEncoder]
  def goToSleep(): MythProtocolResult[Boolean]
  def lockTuner(): Any // TODO capture the appropriate return type
  def lockTuner(cardId: CaptureCardId): Any // see above for return type
  def message(message: String, extra: String*): MythProtocolResult[Boolean]
  def messageSetLogLevel(logLevel: MythLogLevel): MythProtocolResult[Boolean]
  def messageSetVerbose(verboseMask: String): MythProtocolResult[Boolean]
  def protocolVersion(ver: Int, token: String): MythProtocolResult[(Boolean, Int)]
  def queryActiveBackends: MythProtocolResult[List[String]]
  def queryBookmark(chanId: ChanId, startTime: MythDateTime): MythProtocolResult[VideoPositionFrame]
  def queryCheckFile(rec: Recording, checkSlaves: Boolean = true): MythProtocolResult[String]
  def queryCommBreak(chanId: ChanId, startTime: MythDateTime): MythProtocolResult[List[VideoSegment]]
  def queryCutList(chanId: ChanId, startTime: MythDateTime): MythProtocolResult[List[VideoSegment]]
  def queryFileExists(fileName: String, storageGroup: String = ""): MythProtocolResult[(String, FileStats)]
  def queryFileHash(fileName: String, storageGroup: String, hostName: String = ""): MythProtocolResult[MythFileHash]
  def queryFileTransferDone(ftId: FileTransferId): Unit
  def queryFileTransferIsOpen(ftId: FileTransferId): MythProtocolResult[Boolean]
  def queryFileTransferReopen(ftId: FileTransferId, newFileName: String): MythProtocolResult[Boolean]
  def queryFileTransferRequestBlock(ftId: FileTransferId, blockSize: Int): MythProtocolResult[Int]
  def queryFileTransferRequestSize(ftId: FileTransferId): MythProtocolResult[Long]
  def queryFileTransferSeek(ftId: FileTransferId, pos: Long, whence: SeekWhence, currentPos: Long): MythProtocolResult[Long]
  def queryFileTransferSetTimeout(ftId: FileTransferId, fast: Boolean): Unit
  def queryFileTransferWriteBlock(ftId: FileTransferId, blockSize: Int): MythProtocolResult[Int]
  def queryFreeSpace: MythProtocolResult[List[FreeSpace]]
  def queryFreeSpaceList: MythProtocolResult[List[FreeSpace]]
  def queryFreeSpaceSummary: MythProtocolResult[(ByteCount, ByteCount)]
  def queryGenPixmap(rec: Recording, token: String = "", time: VideoPosition = VideoPositionSeconds(-1),
    outputFile: String = "", width: Int = 0, height: Int = 0): MythProtocolResult[Boolean]
  def queryGetAllPending: MythProtocolResult[ExpectedCountIterator[Recording]]
  def queryGetAllScheduled: MythProtocolResult[ExpectedCountIterator[Recording]]
  def queryGetConflicting(rec: Recording): MythProtocolResult[ExpectedCountIterator[Recording]]  // TODO should parameter really be a Recordable?
  def queryGetExpiring: MythProtocolResult[ExpectedCountIterator[Recording]]
  def queryGuideDataThrough: MythProtocolResult[MythDateTime]
  def queryHostname: MythProtocolResult[String]
  def queryIsActiveBackend(hostName: String): MythProtocolResult[Boolean]
  def queryIsRecording: MythProtocolResult[(Int, Int)]
  def queryLoad: MythProtocolResult[(Double, Double, Double)]
  def queryMemStats: MythProtocolResult[(ByteCount, ByteCount, ByteCount, ByteCount)]
  def queryPixmapGetIfModified(maxFileSize: Long, rec: Recording): MythProtocolResult[(MythDateTime, Option[PixmapInfo])]
  def queryPixmapGetIfModified(modifiedSince: MythDateTime, maxFileSize: Long, rec: Recording): MythProtocolResult[(MythDateTime, Option[PixmapInfo])]
  def queryPixmapLastModified(rec: Recording): MythProtocolResult[MythDateTime]
  def queryRecorderCancelNextRecording(cardId: CaptureCardId, cancel: Boolean): Unit
  def queryRecorderChangeBrightness(cardId: CaptureCardId, adjType: PictureAdjustType, up: Boolean): MythProtocolResult[Int]
  def queryRecorderChangeChannel(cardId: CaptureCardId, dir: ChannelChangeDirection): Unit
  def queryRecorderChangeColour(cardId: CaptureCardId, adjType: PictureAdjustType, up: Boolean): MythProtocolResult[Int]
  def queryRecorderChangeContrast(cardId: CaptureCardId, adjType: PictureAdjustType, up: Boolean): MythProtocolResult[Int]
  def queryRecorderChangeHue(cardId: CaptureCardId, adjType: PictureAdjustType, up: Boolean): MythProtocolResult[Int]
  def queryRecorderCheckChannel(cardId: CaptureCardId, channum: ChannelNumber): MythProtocolResult[Boolean]
  def queryRecorderCheckChannelPrefix(cardId: CaptureCardId, channumPrefix: ChannelNumber): MythProtocolResult[(Boolean, Option[CaptureCardId], Boolean, String)]
  // This returns a map from frame number to duration, what is that???
  def queryRecorderFillDurationMap(cardId: CaptureCardId, start: VideoPositionFrame, end: VideoPositionFrame): MythProtocolResult[Map[VideoPositionFrame, Long]]
  // This returns a map from frame number to file byte offset
  def queryRecorderFillPositionMap(cardId: CaptureCardId, start: VideoPositionFrame, end: VideoPositionFrame): MythProtocolResult[Map[VideoPositionFrame, Long]]
  def queryRecorderFinishRecording(cardId: CaptureCardId): Unit
  def queryRecorderFrontendReady(cardId: CaptureCardId): Unit
  def queryRecorderGetBrightness(cardId: CaptureCardId): MythProtocolResult[Int]
  def queryRecorderGetChannelInfo(cardId: CaptureCardId, chanId: ChanId): MythProtocolResult[Channel]
  def queryRecorderGetColour(cardId: CaptureCardId): MythProtocolResult[Int]
  def queryRecorderGetContrast(cardId: CaptureCardId): MythProtocolResult[Int]
  def queryRecorderGetCurrentRecording(cardId: CaptureCardId): MythProtocolResult[Recording]
  def queryRecorderGetFilePosition(cardId: CaptureCardId): MythProtocolResult[Long]
  def queryRecorderGetFrameRate(cardId: CaptureCardId): MythProtocolResult[Double]
  def queryRecorderGetFramesWritten(cardId: CaptureCardId): MythProtocolResult[Long]
  def queryRecorderGetFreeInputs(cardId: CaptureCardId, excludedCards: CaptureCardId*): MythProtocolResult[List[CardInput]]
  def queryRecorderGetHue(cardId: CaptureCardId): MythProtocolResult[Int]
  def queryRecorderGetInput(cardId: CaptureCardId): MythProtocolResult[String]
  // This returns byte offset from the approximate keyframe position
  def queryRecorderGetKeyframePos(cardId: CaptureCardId, desiredPos: VideoPositionFrame): MythProtocolResult[Long]
  def queryRecorderGetMaxBitrate(cardId: CaptureCardId): MythProtocolResult[Long]
  def queryRecorderGetNextProgramInfo(cardId: CaptureCardId, chanId: ChanId, dir: ChannelBrowseDirection,
    startTime: MythDateTime): MythProtocolResult[UpcomingProgram]
  def queryRecorderGetNextProgramInfo(cardId: CaptureCardId, channum: ChannelNumber, dir: ChannelBrowseDirection,
    startTime: MythDateTime): MythProtocolResult[UpcomingProgram]
  def queryRecorderGetRecording(cardId: CaptureCardId): MythProtocolResult[Recording]
  def queryRecorderIsRecording(cardId: CaptureCardId): MythProtocolResult[Boolean]
  def queryRecorderPause(cardId: CaptureCardId): Unit
  // NB Must call queryRecorderPause before queryRecorderSetChannel
  def queryRecorderSetChannel(cardId: CaptureCardId, channum: ChannelNumber): Unit
  def queryRecorderSetInput(cardId: CaptureCardId, inputName: String): MythProtocolResult[String]
  // NB the recordingState parameter is ignored by the backend implementation
  def queryRecorderSetLiveRecording(cardId: CaptureCardId, recordingState: Int): Unit
  def queryRecorderSetSignalMonitoringRate(cardId: CaptureCardId, rate: Int, notifyFrontend: Boolean): MythProtocolResult[Boolean]
  def queryRecorderShouldSwitchCard(cardId: CaptureCardId, chanId: ChanId): MythProtocolResult[Boolean]
  // FIXME when I invoked spawnLiveTV during testing, it caused SIGABRT on the backend !!
  def queryRecorderSpawnLiveTV(cardId: CaptureCardId, usePiP: Boolean, channumStart: ChannelNumber): Unit
  def queryRecorderStopLiveTV(cardId: CaptureCardId): Unit
  def queryRecorderToggleChannelFavorite(cardId: CaptureCardId, channelGroup: String): Unit
  def queryRecording(pathName: String): MythProtocolResult[Recording]
  def queryRecording(chanId: ChanId, startTime: MythDateTime): MythProtocolResult[Recording]
  def queryRecordings(specifier: String = "Unsorted"): MythProtocolResult[ExpectedCountIterator[Recording]]
  def queryRemoteEncoderCancelNextRecording(cardId: CaptureCardId, cancel: Boolean): Unit
  def queryRemoteEncoderGetCurrentRecording(cardId: CaptureCardId): MythProtocolResult[Recording]
  def queryRemoteEncoderGetFreeInputs(cardId: CaptureCardId, excludedCards: CaptureCardId*): MythProtocolResult[List[CardInput]]
  def queryRemoteEncoderGetMaxBitrate(cardId: CaptureCardId): MythProtocolResult[Long]
  def queryRemoteEncoderGetRecordingStatus(cardId: CaptureCardId): MythProtocolResult[RecStatus]
  def queryRemoteEncoderGetState(cardId: CaptureCardId): MythProtocolResult[TvState]
  def queryRemoteEncoderGetSleepStatus(cardId: CaptureCardId): MythProtocolResult[SleepStatus]
  def queryRemoteEncoderGetFlags(cardId: CaptureCardId): MythProtocolResult[Int]
  def queryRemoteEncoderIsBusy(cardId: CaptureCardId): MythProtocolResult[(Boolean, Option[CardInput], Option[ChanId])]
  def queryRemoteEncoderIsBusy(cardId: CaptureCardId, timeBufferSeconds: Int): MythProtocolResult[(Boolean, Option[CardInput], Option[ChanId])]
  def queryRemoteEncoderMatchesRecording(cardId: CaptureCardId, rec: Recording): MythProtocolResult[Boolean]
  def queryRemoteEncoderRecordPending(cardId: CaptureCardId, secondsLeft: Int, hasLaterShowing: Boolean, rec: Recording): Unit
  def queryRemoteEncoderStartRecording(cardId: CaptureCardId, rec: Recording): MythProtocolResult[RecStatus]
  def queryRemoteEncoderStopRecording(cardId: CaptureCardId): Unit
  def querySetting(hostName: String, settingName: String): MythProtocolResult[String]
  def querySGGetFileList(hostName: String, storageGroup: String, path: String): MythProtocolResult[List[String]]
  def querySGGetFileList(hostName: String, storageGroup: String, path: String, fileNamesOnly: Boolean): MythProtocolResult[List[String]]
  def querySGFileQuery(hostName: String, storageGroup: String, fileName: String): MythProtocolResult[(String, MythDateTime, ByteCount)]
  def queryTimeZone: MythProtocolResult[TimeZoneInfo]
  def queryUptime: MythProtocolResult[Duration]
  def refreshBackend: MythProtocolResult[Boolean]
  def rescheduleRecordingsCheck(recStatus: RecStatus = RecStatus.Unknown, recordId: RecordRuleId = RecordRuleId(0), findId: Int = 0,
    title: String = "", subtitle: String = "", description: String = "", programId: String = "", reason: String = "Scala"): MythProtocolResult[Boolean]
  def rescheduleRecordingsMatch(recordId: RecordRuleId = RecordRuleId(0), sourceId: ListingSourceId = ListingSourceId(0),
    mplexId: MultiplexId = MultiplexId(0), maxStartTime: Option[MythDateTime] = None, reason: String = "Scala"): MythProtocolResult[Boolean]
  def rescheduleRecordingsPlace(reason: String = "Scala"): MythProtocolResult[Boolean]
  def scanVideos: MythProtocolResult[Boolean]
  def setBookmark(chanId: ChanId, startTime: MythDateTime, pos: VideoPositionFrame): MythProtocolResult[Boolean]
  def setSetting(hostName: String, settingName: String, value: String): MythProtocolResult[Boolean]
  def shutdownNow(haltCommand: String = ""): Unit
  def stopRecording(rec: Recording): MythProtocolResult[CaptureCardId]
  def undeleteRecording(rec: Recording): MythProtocolResult[Boolean]
  def undeleteRecording(chanId: ChanId, startTime: MythDateTime): MythProtocolResult[Boolean]
  // TODO more methods
}
