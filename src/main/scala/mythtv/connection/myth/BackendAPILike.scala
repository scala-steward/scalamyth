package mythtv
package connection
package myth

import java.time.Duration

import model._
import model.EnumTypes._
import util.{ ByteCount, ExpectedCountIterator, FileStats, MythDateTime, MythFileHash, NetworkUtil }
import EnumTypes.{ MythLogLevel, MythProtocolEventMode, SeekWhence }
import MythProtocol.QueryRecorderResult._
import MythProtocol.QueryFileTransferResult._

private trait BackendAPILike {
  self: MythProtocolLike =>

  def allowShutdown(): Boolean = {
    val result = sendCommand("ALLOW_SHUTDOWN")
    (result map { case r: Boolean => r }).right.get
  }

  def announce(mode: String, hostName: String, eventMode: MythProtocolEventMode): Boolean = {
    import MythProtocol.AnnounceResult._
    val localHost =
      if (hostName != "") hostName
      else NetworkUtil.myHostName
    val result = sendCommand("ANN", mode, localHost, eventMode)
    (result map { case AnnounceAcknowledgement => true }).right.get
  }

  def announceFileTransfer(hostName: String, fileName: String, storageGroup: String,
    writeMode: Boolean, useReadAhead: Boolean, timeout: Duration): (FileTransferId, ByteCount) = {
    import MythProtocol.AnnounceResult._
    val result = sendCommand("ANN", "FileTransfer", hostName, writeMode, useReadAhead, timeout, fileName, storageGroup)
    (result map { case AnnounceFileTransfer(ftID, fileSize) => (ftID, fileSize) }).right.get
  }

  def blockShutdown(): Boolean = {
    val result = sendCommand("BLOCK_SHUTDOWN")
    (result map { case r: Boolean => r }).right.get
  }

  def checkRecording(rec: Recording): Boolean = {
    val result = sendCommand("CHECK_RECORDING", rec)
    (result map { case r: Boolean => r }).right.get
  }

  def deleteFile(fileName: String, storageGroup: String): Boolean = {
    val result = sendCommand("DELETE_FILE", fileName, storageGroup)
    (result map { case r: Boolean => r }).right.get
  }

  def deleteRecording(rec: Recording): Int = {
    val result = sendCommand("DELETE_RECORDING", rec)
    (result map { case r: Int => r }).right.get
  }

  def deleteRecording(chanId: ChanId, startTime: MythDateTime): Int = {
    val result = sendCommand("DELETE_RECORDING", chanId, startTime)
    (result map { case r: Int => r }).right.get
  }

  def deleteRecording(chanId: ChanId, startTime: MythDateTime, forceDeleteMetadata: Boolean, forgetHistory: Boolean): Int = {
    val force = if (forceDeleteMetadata) "FORCE" else "-"
    val forget = if (forgetHistory) "FORGET" else ""
    val result = sendCommand("DELETE_RECORDING", chanId, startTime, force, forget)
    (result map { case r: Int => r }).right.get
  }

  def done(): Unit = ???

  def fillProgramInfo(playbackHost: String, p: Recording): Recording = {
    val result = sendCommand("FILL_PROGRAM_INFO", playbackHost, p)
    (result map { case r: Recording => r }).right.get
  }

  // TODO is the result here really Int or Boolean
  def forceDeleteRecording(rec: Recording): Int = {
    val result = sendCommand("FORCE_DELETE_RECORDING", rec)
    (result map { case r: Int => r }).right.get
  }

  // TODO is the result here really Int or Boolean
  def forgetRecording(rec: Recording): Int = {
    val result = sendCommand("FORGET_RECORDING", rec)
    (result map { case r: Int => r }).right.get
  }

  def freeTuner(cardId: CaptureCardId): Boolean = {
    val result = sendCommand("FREE_TUNER", cardId)
    (result map { case r: Boolean => r }).right.get
  }

  def getFreeRecorder: RemoteEncoder = {
    val result = sendCommand("GET_FREE_RECORDER")
    (result map { case e: RemoteEncoder => e }).right.get
  }

  def getFreeRecorderCount: Int = {
    val result = sendCommand("GET_FREE_RECORDER_COUNT")
    (result map { case n: Int => n }).right.get
  }

  def getFreeRecorderList: List[CaptureCardId] = {
    val result = sendCommand("GET_FREE_RECORDER_LIST")
    (result map { case xs: List[_] => xs.asInstanceOf[List[CaptureCardId]] }).right.get
  }

  def getNextFreeRecorder(cardId: CaptureCardId): RemoteEncoder = {
    val result = sendCommand("GET_NEXT_FREE_RECORDER", cardId)
    (result map { case e: RemoteEncoder => e }).right.get
  }

  def getRecorderFromNum(cardId: CaptureCardId): RemoteEncoder = {
    val result = sendCommand("GET_RECORDER_FROM_NUM", cardId)
    (result map { case e: RemoteEncoder => e }).right.get
  }

  def getRecorderNum(rec: Recording): RemoteEncoder = {
    val result = sendCommand("GET_RECORDER_NUM", rec)
    (result map { case e: RemoteEncoder => e }).right.get
  }

  // TODO a way to return error message if any
  def goToSleep(): Boolean = {
    val result = sendCommand("GO_TO_SLEEP")
    (result map { case r: Boolean => r }).right.get
  }

  def lockTuner(): Any = ??? // TODO capture the appropriate return type
  def lockTuner(cardId: CaptureCardId): Any = ??? // see above for return type

  def message(message: String, extra: String*): Boolean = {
    val args = List(message) ++ extra
    val result = sendCommand("MESSAGE", args: _*)
    (result map { case r: Boolean => r }).right.get
  }

  def messageSetLogLevel(logLevel: MythLogLevel): Boolean = {
    val result = sendCommand("MESSAGE", "SET_LOG_LEVEL", logLevel)
    (result map { case r: Boolean => r }).right.get
  }

  def messageSetVerbose(verboseMask: String): Boolean = {
    val result = sendCommand("MESSAGE", "SET_VERBOSE", verboseMask)
    (result map { case r: Boolean => r }).right.get
  }

  def protocolVersion(version: Int, token: String): (Boolean, Int) = {
    // NOTE that I believe an incorrect protocol version results in socket being closed
    val result = sendCommand("MYTH_PROTO_VERSION", version, token)
    (result map { case (accepted: Boolean, acceptVer: Int) => (accepted, acceptVer) }).right.get
  }

  def queryActiveBackends: List[String] = {
    val result = sendCommand("QUERY_ACTIVE_BACKENDS")
    (result map { case xs: List[_] => xs.asInstanceOf[List[String]] }).right.get
  }

  def queryBookmark(chanId: ChanId, startTime: MythDateTime): VideoPositionFrame = {
    val result = sendCommand("QUERY_BOOKMARK", chanId, startTime)
    (result map { case p: VideoPositionFrame => p }).right.get
  }

  def queryCheckFile(rec: Recording, checkSlaves: Boolean): String = {
    val result = sendCommand("QUERY_CHECKFILE", checkSlaves, rec)
    (result map { case s: String => s }).right.get
  }

  def queryCommBreak(chanId: ChanId, startTime: MythDateTime): List[VideoSegment] = {
    val result = sendCommand("QUERY_COMMBREAK", chanId, startTime)
    (result map { case xs: List[_] => xs.asInstanceOf[List[VideoSegment]] }).right.get
  }

  def queryCutList(chanId: ChanId, startTime: MythDateTime): List[VideoSegment] = {
    val result = sendCommand("QUERY_CUTLIST", chanId, startTime)
    (result map { case xs: List[_] => xs.asInstanceOf[List[VideoSegment]] }).right.get
  }

  def queryFileExists(fileName: String, storageGroup: String): (String, FileStats) = {
    val result =
      if (storageGroup.isEmpty) sendCommand("QUERY_FILE_EXISTS", fileName)
      else sendCommand("QUERY_FILE_EXISTS", fileName, storageGroup)
    (result map { case (fullName: String, stats: FileStats) => (fullName, stats) }).right.get
  }

  def queryFileTransferDone(ftId: FileTransferId): Unit = {
    val result = sendCommand("QUERY_FILETRANSFER", ftId, "DONE")
    (result map { case QueryFileTransferAcknowledgement => true }).right.get
  }

  def queryFileTransferIsOpen(ftId: FileTransferId): Boolean = {
    val result = sendCommand("QUERY_FILETRANSFER", ftId, "IS_OPEN")
    (result map { case QueryFileTransferBoolean(bool) => bool }).right.get
  }

  def queryFileTransferReopen(ftId: FileTransferId, newFileName: String): Boolean = {
    val result = sendCommand("QUERY_FILETRANSFER", ftId, "REOPEN")
    (result map { case QueryFileTransferBoolean(bool) => bool }).right.get
  }

  def queryFileTransferRequestBlock(ftId: FileTransferId, blockSize: Int): Int = {
    val result = sendCommand("QUERY_FILETRANSFER", ftId, "REQUEST_BLOCK", blockSize)
    (result map { case QueryFileTransferBytesTransferred(count) => count }).right.get
  }

  def queryFileTransferRequestSize(ftId: FileTransferId): Long = {
    val result = sendCommand("QUERY_FILETRANSFER", ftId, "REQUEST_SIZE")
    (result map { case QueryFileTransferRequestSize(size, _) => size }).right.get
  }

  def queryFileTransferSeek(ftId: FileTransferId, pos: Long, whence: SeekWhence, currentPos: Long): Long = {
    val result = sendCommand("QUERY_FILETRANSFER", ftId, "SEEK", pos, whence, currentPos)
    (result map { case QueryFileTransferPosition(newPos) => newPos}).right.get
  }

  def queryFileTransferSetTimeout(ftId: FileTransferId, fast: Boolean): Unit = {
    val result = sendCommand("QUERY_FILETRANSFER", ftId, "SET_TIMEOUT", fast)
    (result map { case QueryFileTransferAcknowledgement => true }).right.get
  }

  def queryFileTransferWriteBlock(ftId: FileTransferId, blockSize: Int): Int = {
    val result = sendCommand("QUERY_FILETRANSFER", ftId, "WRITE_BLOCK", blockSize)
    (result map { case QueryFileTransferBytesTransferred(count) => count }).right.get
  }

  def queryFileHash(fileName: String, storageGroup: String, hostName: String): MythFileHash = {
    val result =
      if (hostName == "") sendCommand("QUERY_FILE_HASH", fileName, storageGroup)
      else sendCommand("QUERY_FILE_HASH", fileName, storageGroup, hostName)
    (result map { case h: MythFileHash => h }).right.get
  }

  def queryFreeSpace: List[FreeSpace] = {
    val result = sendCommand("QUERY_FREE_SPACE")
    (result map { case xs: List[_] => xs.asInstanceOf[List[FreeSpace]] }).right.get
  }

  def queryFreeSpaceList: List[FreeSpace] = {
    val result = sendCommand("QUERY_FREE_SPACE_LIST")
    (result map { case xs: List[_] => xs.asInstanceOf[List[FreeSpace]] }).right.get
  }

  def queryFreeSpaceSummary: (ByteCount, ByteCount) = {
    val result = sendCommand("QUERY_FREE_SPACE_SUMMARY")
    (result map { case (total: ByteCount, used: ByteCount) => (total, used) }).right.get
  }

  def queryGenPixmap(rec: Recording, token: String, time: VideoPosition, outputFile: String, width: Int, height: Int): Boolean = {
    val sentToken = if (token.isEmpty) "do_not_care" else token
    val fileName = if (outputFile.isEmpty) "<EMPTY>" else outputFile
    val hasExtra = time.pos != -1 || outputFile.nonEmpty || width != 0 || height != 0

    val result =
      if (hasExtra) sendCommand("QUERY_GENPIXMAP2", sentToken, rec, time.units, time, fileName, width, height)
      else          sendCommand("QUERY_GENPIXMAP2", sentToken, rec)

    (result map { case r: Boolean => r }).right.get
  }

  def queryGetAllPending: ExpectedCountIterator[Recording] = {
    val result = sendCommand("QUERY_GETALLPENDING")
    (result map { case it: ExpectedCountIterator[_] => it.asInstanceOf[ExpectedCountIterator[Recording]] }).right.get
  }

  def queryGetAllScheduled: ExpectedCountIterator[Recording] = {
    val result = sendCommand("QUERY_GETALLSCHEDULED")
    (result map { case it: ExpectedCountIterator[_] => it.asInstanceOf[ExpectedCountIterator[Recording]] }).right.get
  }

  def queryGetConflicting(rec: Recording): ExpectedCountIterator[Recording] = {
    val result = sendCommand("QUERY_GETCONFLICTING", rec)
    (result map { case it: ExpectedCountIterator[_] => it.asInstanceOf[ExpectedCountIterator[Recording]] }).right.get
  }

  def queryGetExpiring: ExpectedCountIterator[Recording] = {
    val result = sendCommand("QUERY_GETEXPIRING")
    (result map { case it: ExpectedCountIterator[_] => it.asInstanceOf[ExpectedCountIterator[Recording]] }).right.get
  }

  def queryGuideDataThrough: MythDateTime = {
    val result = sendCommand("QUERY_GUIDEDATATHROUGH")
    (result map { case d: MythDateTime => d }).right.get
  }

  def queryHostname: String = {
    val result = sendCommand("QUERY_HOSTNAME")
    (result map { case h: String => h }).right.get
  }

  def queryIsActiveBackend(hostName: String): Boolean = {
    val result = sendCommand("QUERY_IS_ACTIVE_BACKEND", hostName)
    (result map { case a: Boolean => a }).right.get
  }

  def queryIsRecording: (Int, Int) = {
    val result = sendCommand("QUERY_ISRECORDING")
    (result map { case (rec: Int, live: Int) => (rec, live) }).right.get
  }

  def queryLoad: (Double, Double, Double) = {
    val result = sendCommand("QUERY_LOAD")
    (result map { case (one: Double, five: Double, fifteen: Double) => (one, five, fifteen) }).right.get
  }

  def queryMemStats: (ByteCount, ByteCount, ByteCount, ByteCount) = {
    val result = sendCommand("QUERY_MEMSTATS")
    (result map {
      case (total: ByteCount, free: ByteCount, totalVM: ByteCount, freeVM: ByteCount) =>
        (total, free, totalVM, freeVM)
    }).right.get
  }

  def queryPixmapGetIfModified(maxFileSize: Long, rec: Recording): (MythDateTime, Option[PixmapInfo]) = {
    val result = sendCommand("QUERY_PIXMAP_GET_IF_MODIFIED", maxFileSize, rec)
    (result map {
      case (lastModified: MythDateTime, pixmapInfo: Option[_]) =>
        (lastModified, pixmapInfo map { case (p: PixmapInfo) => p })
    }).right.get
  }

  def queryPixmapGetIfModified(modifiedSince: MythDateTime, maxFileSize: Long, rec: Recording): (MythDateTime, Option[PixmapInfo]) = {
    val result = sendCommand("QUERY_PIXMAP_GET_IF_MODIFIED", modifiedSince, maxFileSize, rec)
    (result map {
      case (lastModified: MythDateTime, pixmapInfo: Option[_]) =>
        (lastModified, pixmapInfo map { case (p: PixmapInfo) => p })
    }).right.get
  }

  def queryPixmapLastModified(rec: Recording): MythDateTime = {
    val result = sendCommand("QUERY_PIXMAP_LASTMODIFIED", rec)
    (result map { case d: MythDateTime => d }).right.get
  }

  def queryRecorderCancelNextRecording(cardId: CaptureCardId, cancel: Boolean): Unit = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CANCEL_NEXT_RECORDING", cancel)
  }

  def queryRecorderChangeBrightness(cardId: CaptureCardId, adjType: PictureAdjustType, up: Boolean): Int = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CHANGE_BRIGHTNESS", adjType, up)
    (result map { case QueryRecorderPictureAttribute(a) => a }).right.get
  }

  def queryRecorderChangeChannel(cardId: CaptureCardId, dir: ChannelChangeDirection): Unit = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CHANGE_CHANNEL", dir)
  }

  def queryRecorderChangeColour(cardId: CaptureCardId, adjType: PictureAdjustType, up: Boolean): Int = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CHANGE_COLOUR", adjType, up)
    (result map { case QueryRecorderPictureAttribute(a) => a }).right.get
  }

  def queryRecorderChangeContrast(cardId: CaptureCardId, adjType: PictureAdjustType, up: Boolean): Int = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CHANGE_CONTRAST", adjType, up)
    (result map { case QueryRecorderPictureAttribute(a) => a }).right.get
  }

  def queryRecorderChangeHue(cardId: CaptureCardId, adjType: PictureAdjustType, up: Boolean): Int = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CHANGE_HUE", adjType, up)
    (result map { case QueryRecorderPictureAttribute(a) => a }).right.get
  }

  def queryRecorderCheckChannel(cardId: CaptureCardId, channum: ChannelNumber): Boolean = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CHECK_CHANNEL", channum)
    (result map { case QueryRecorderBoolean(b) => b }).right.get
  }

  def queryRecorderCheckChannelPrefix(cardId: CaptureCardId, channumPrefix: ChannelNumber):
      (Boolean, Option[CaptureCardId], Boolean, String) = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CHECK_CHANNEL_PREFIX", channumPrefix)
    (result map { case QueryRecorderCheckChannelPrefix(matched, card, extraCharUseful, spacer) =>
      (matched, card, extraCharUseful, spacer) }).right.get
  }
  // TODO return type more specific?
  def queryRecorderFillDurationMap(cardId: CaptureCardId, start: VideoPositionFrame, end: VideoPositionFrame): Map[VideoPositionFrame, Long] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "FILL_DURATION_MAP", start, end)
    (result map { case QueryRecorderPositionMap(m) => m }).right.get
  }

  // TODO return type more specific?
  def queryRecorderFillPositionMap(cardId: CaptureCardId, start: VideoPositionFrame, end: VideoPositionFrame): Map[VideoPositionFrame, Long] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "FILL_POSITION_MAP", start, end)
    (result map { case QueryRecorderPositionMap(m) => m }).right.get
  }

  def queryRecorderFinishRecording(cardId: CaptureCardId): Unit = {
    val result = sendCommand("QUERY_RECORDER", cardId, "FINISH_RECORDING")
  }

  def queryRecorderFrontendReady(cardId: CaptureCardId): Unit = {
    val result = sendCommand("QUERY_RECORDER", cardId, "FRONTEND_READY")
  }

  def queryRecorderGetBrightness(cardId: CaptureCardId): Int = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_BRIGHTNESS")
    (result map { case QueryRecorderPictureAttribute(a) => a }).right.get
  }

  def queryRecorderGetChannelInfo(cardId: CaptureCardId, chanId: ChanId): Channel = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_CHANNEL_INFO", chanId)
    (result map { case QueryRecorderChannelInfo(c) => c }).right.get
  }

  def queryRecorderGetColour(cardId: CaptureCardId): Int = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_COLOUR")
    (result map { case QueryRecorderPictureAttribute(a) => a }).right.get
  }

  def queryRecorderGetContrast(cardId: CaptureCardId): Int = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_CONTRAST")
    (result map { case QueryRecorderPictureAttribute(a) => a }).right.get
  }

  def queryRecorderGetCurrentRecording(cardId: CaptureCardId): Recording = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_CURRENT_RECORDING")
    (result map { case QueryRecorderRecording(r) => r }).right.get
  }

  def queryRecorderGetFilePosition(cardId: CaptureCardId): Long = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_FILE_POSITION")
    (result map { case QueryRecorderPosition(p) => p }).right.get
  }

  def queryRecorderGetFrameRate(cardId: CaptureCardId): Double = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_FRAMERATE")
    (result map { case QueryRecorderFrameRate(r) => r }).right.get
  }

  def queryRecorderGetFramesWritten(cardId: CaptureCardId): Long = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_FRAMES_WRITTEN")
    (result map { case QueryRecorderFrameCount(n) => n }).right.get
  }

  def queryRecorderGetFreeInputs(cardId: CaptureCardId, excludedCards: CaptureCardId*): List[CardInput] = {
    val args = List(cardId) ++ excludedCards
    val result = sendCommand("QUERY_RECORDER", args: _*)
    (result map { case QueryRecorderCardInputList(i) => i }).right.get
  }

  def queryRecorderGetHue(cardId: CaptureCardId): Int = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_HUE")
    (result map { case QueryRecorderPictureAttribute(a) => a }).right.get
  }

  def queryRecorderGetInput(cardId: CaptureCardId): String = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_INPUT")
    (result map { case QueryRecorderInput(input) => input }).right.get
  }

  def queryRecorderGetKeyframePos(cardId: CaptureCardId, desiredPos: VideoPositionFrame): Long = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_KEYFRAME_POS", desiredPos)
    (result map { case QueryRecorderPosition(p) => p }).right.get
  }

  def queryRecorderGetMaxBitrate(cardId: CaptureCardId): Long = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_MAX_BITRATE")
    (result map { case QueryRecorderBitrate(b) => b }).right.get
  }

  def queryRecorderGetNextProgramInfo(cardId: CaptureCardId, chanId: ChanId, dir: ChannelBrowseDirection,
    startTime: MythDateTime): UpcomingProgram = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_NEXT_PROGRAM_INFO", ChannelNumber(""), chanId, dir, startTime)
    (result map { case QueryRecorderNextProgramInfo(p) => p }).right.get
  }

  def queryRecorderGetNextProgramInfo(cardId: CaptureCardId, channum: ChannelNumber, dir: ChannelBrowseDirection,
    startTime: MythDateTime): UpcomingProgram = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_NEXT_PROGRAM_INFO", channum, ChanId(0), dir, startTime)
    (result map { case QueryRecorderNextProgramInfo(p) => p }).right.get
  }

  def queryRecorderGetRecording(cardId: CaptureCardId): Recording = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_RECORDING")
    (result map { case QueryRecorderRecording(r) => r }).right.get
  }

  def queryRecorderIsRecording(cardId: CaptureCardId): Boolean = {
    val result = sendCommand("QUERY_RECORDER", cardId, "IS_RECORDING")
    (result map { case QueryRecorderBoolean(b) => b }).right.get
  }

  def queryRecorderPause(cardId: CaptureCardId): Unit = {
    val result = sendCommand("QUERY_RECORDER", cardId, "PAUSE")
  }

  def queryRecorderSetChannel(cardId: CaptureCardId, channum: ChannelNumber): Unit = {
    val result = sendCommand("QUERY_RECORDER", cardId, "SET_CHANNEL", channum)
  }

  def queryRecorderSetInput(cardId: CaptureCardId, inputName: String): String = {
    val result = sendCommand("QUERY_RECORDER", cardId, "SET_INPUT", inputName)
    (result map { case QueryRecorderInput(input) => input }).right.get
  }

  def queryRecorderSetLiveRecording(cardId: CaptureCardId, recordingState: Int): Unit = {
    val result = sendCommand("QUERY_RECORDER", cardId, "SET_LIVE_RECORDING", recordingState)
  }

  def queryRecorderSetSignalMonitoringRate(cardId: CaptureCardId, rate: Int, notifyFrontend: Boolean): Boolean = {
    val result = sendCommand("QUERY_RECORDER", cardId, "SET_SIGNAL_MONITORING_RATE", rate, notifyFrontend)
    (result map { case QueryRecorderBoolean(b) => b }).right.get
  }

  def queryRecorderShouldSwitchCard(cardId: CaptureCardId, chanId: ChanId): Boolean = {
    val result = sendCommand("QUERY_RECORDER", cardId, "SHOULD_SWITCH_CARD", chanId)
    (result map { case QueryRecorderBoolean(b) => b }).right.get
  }

  def queryRecorderSpawnLiveTV(cardId: CaptureCardId, usePiP: Boolean, channumStart: ChannelNumber): Unit = {
    val result = sendCommand("QUERY_RECORDER", cardId, "SPAWN_LIVETV", usePiP, channumStart)
  }

  def queryRecorderStopLiveTV(cardId: CaptureCardId): Unit = {
    val result = sendCommand("QUERY_RECORDER", cardId, "STOP_LIVETV")
  }

  def queryRecorderToggleChannelFavorite(cardId: CaptureCardId, channelGroup: String): Unit = {
    val result = sendCommand("QUERY_RECORDER", cardId, "TOGGLE_CHANNEL_FAVORITE", channelGroup)
  }

  def queryRecording(pathName: String): Recording = {
    val result = sendCommand("QUERY_RECORDING", "BASENAME", pathName)
    (result map { case r: Recording => r }).right.get
  }

  def queryRecording(chanId: ChanId, startTime: MythDateTime): Recording = {
    val result = sendCommand("QUERY_RECORDING", "TIMESLOT", chanId, startTime)
    (result map { case r: Recording => r }).right.get
  }

  def queryRecordings(specifier: String): ExpectedCountIterator[Recording] = {
    val result = sendCommand("QUERY_RECORDINGS", specifier)
    (result map { case it: ExpectedCountIterator[_] => it.asInstanceOf[ExpectedCountIterator[Recording]] }).right.get
  }

  def querySGGetFileList(hostName: String, storageGroup: String, path: String): List[String] = {
    val result = sendCommand("QUERY_SG_GETFILELIST", hostName, storageGroup, path)
    (result map { case xs: List[_] => xs.asInstanceOf[List[String]] }).right.get
  }

  def querySGGetFileList(hostName: String, storageGroup: String, path: String, fileNamesOnly: Boolean): List[String] = {
    val result = sendCommand("QUERY_SG_GETFILELIST", hostName, storageGroup, path, fileNamesOnly)
    (result map { case xs: List[_] => xs.asInstanceOf[List[String]] }).right.get
  }

  def querySGFileQuery(hostName: String, storageGroup: String, fileName: String): (String, MythDateTime, ByteCount) = {
    val result = sendCommand("QUERY_SG_FILEQUERY", hostName, storageGroup, fileName)
    (result map { case (fullPath: String, fileTime: MythDateTime, fileSize: ByteCount) => (fullPath, fileTime, fileSize) }).right.get
  }

  def querySetting(hostName: String, settingName: String): Option[String] = {
    val result = sendCommand("QUERY_SETTING", hostName, settingName)
    (result map { case s: String => s }).toOption
  }

  def queryTimeZone: TimeZoneInfo = {
    val result = sendCommand("QUERY_TIME_ZONE")
    (result map { case (tzi: TimeZoneInfo) => tzi }).right.get
  }

  def queryUptime: Duration = {
    val result = sendCommand("QUERY_UPTIME")
    (result map { case d: Duration => d }).right.get
  }

  def refreshBackend: Boolean = {
    val result = sendCommand("REFRESH_BACKEND")
    (result map { case r: Boolean => r }).right.get
  }

  def rescheduleRecordingsCheck(recStatus: RecStatus, recordId: RecordRuleId, findId: Int, title: String, subtitle: String,
    description: String, programId: String, reason: String): Boolean = {
    val result = sendCommand("RESCHEDULE_RECORDINGS", "CHECK", recStatus, recordId, findId, reason,
      title, subtitle, description, programId)
    (result map { case r: Boolean => r }).right.get
  }

  def rescheduleRecordingsMatch(recordId: RecordRuleId, sourceId: ListingSourceId, mplexId: MultiplexId,
    maxStartTime: Option[MythDateTime], reason: String): Boolean = {
    val result =
      if (maxStartTime.isEmpty) sendCommand("RESCHEDULE_RECORDINGS", "MATCH", recordId, sourceId, mplexId, reason)
      else sendCommand("RESCHEDULE_RECORDINGS", "MATCH", recordId, sourceId, mplexId, maxStartTime.get, reason)
    (result map { case r: Boolean => r }).right.get
  }

  def rescheduleRecordingsPlace(reason: String): Boolean = {
    val result = sendCommand("RESCHEDULE_RECORDINGS", "PLACE", reason)
    (result map { case r: Boolean => r }).right.get
  }

  def scanVideos: Boolean = {
    // TODO this may need a longer timeout, may take some time? Is this true?
    val result = sendCommand("SCAN_VIDEOS")
    (result map { case r: Boolean => r }).right.get
  }

  def setBookmark(chanId: ChanId, startTime: MythDateTime, pos: VideoPositionFrame): Boolean = {
    val result = sendCommand("SET_BOOKMARK", chanId, startTime, pos)
    (result map { case r: Boolean => r }).right.get
  }

  def setSetting(hostName: String, settingName: String, value: String): Boolean = {
    val result = sendCommand("SET_SETTING", hostName, settingName, value)
    (result map { case r: Boolean => r }).right.get
  }

  // TODO do we need to post this message rather than send it?
  def shutdownNow(haltCommand: String): Unit = {
    if (haltCommand == "") sendCommand("SHUTDOWN_NOW")
    else sendCommand("SHUTDOWN_NOW", haltCommand)
  }

  // TODO better encapsulate return codes
  def stopRecording(rec: Recording): Int = {
    val result = sendCommand("STOP_RECORDING", rec)
    (result map { case e: Int => e }).right.get
  }

  def undeleteRecording(rec: Recording): Boolean = {
    val result = sendCommand("UNDELETE_RECORDING", rec)
    (result map { case r: Boolean => r }).right.get
  }

  def undeleteRecording(chanId: ChanId, startTime: MythDateTime): Boolean = {
    val result = sendCommand("UNDELETE_RECORDING", chanId, startTime)
    (result map { case r: Boolean => r }).right.get
  }

}
