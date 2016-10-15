package mythtv
package connection
package myth

import java.net.InetAddress
import java.time.{ Duration, Instant, ZoneOffset }

import model.{ CaptureCardId, CardInput, Channel, ChanId, FreeSpace, ListingSourceId, Recording,
  RecordRuleId, RemoteEncoder, UpcomingProgram, VideoPosition, VideoSegment }
import util.{ ByteCount, ExpectedCountIterator, FileStats, MythDateTime }
import model.EnumTypes.{ ChannelBrowseDirection, ChannelChangeDirection, PictureAdjustType, RecStatus }
import EnumTypes.MythProtocolEventMode
import MythProtocol.QueryRecorder._

private trait BackendAPILike {
  self: MythProtocolLike =>

  def allowShutdown(): Boolean = {
    val result = sendCommand("ALLOW_SHUTDOWN")
    (result map { case r: Boolean => r }).get
  }

  def announce(mode: String, hostName: String, eventMode: MythProtocolEventMode): Boolean = {
    import MythProtocol.Announce._
    val localHost =
      if (hostName != "") hostName
      else InetAddress.getLocalHost().getHostName()
    val result = sendCommand("ANN", mode, localHost, eventMode)
    (result map { case AnnounceAcknowledgement => true }).get
  }

  def announceFileTransfer(hostName: String, fileName: String, storageGroup: String,
    writeMode: Boolean, useReadAhead: Boolean, timeout: Duration): (Int, ByteCount) = {
    import MythProtocol.Announce._
    val result = sendCommand("ANN", "FileTransfer", hostName, writeMode, useReadAhead, timeout, fileName, storageGroup)
    (result map { case AnnounceFileTransfer(ftID, fileSize) => (ftID, fileSize) }).get
  }

  def blockShutdown(): Boolean = {
    val result = sendCommand("BLOCK_SHUTDOWN")
    (result map { case r: Boolean => r }).get
  }

  def checkRecording(rec: Recording): Boolean = {
    val result = sendCommand("CHECK_RECORDING", rec)
    (result map { case r: Boolean => r }).get
  }

  def deleteFile(fileName: String, storageGroup: String): Boolean = {
    val result = sendCommand("DELETE_FILE", fileName, storageGroup)
    (result map { case r: Boolean => r }).get
  }

  def deleteRecording(rec: Recording): Int = {
    val result = sendCommand("DELETE_RECORDING", rec)
    (result map { case r: Int => r }).get
  }

  def done(): Unit = ???

  def fillProgramInfo(playbackHost: String, p: Recording): Recording = ???

  // TODO is the result here really Int or Boolean
  def forceDeleteRecording(rec: Recording): Int = {
    val result = sendCommand("FORCE_DELETE_RECORDING", rec)
    (result map { case r: Int => r }).get
  }

  // TODO is the result here really Int or Boolean
  def forgetRecording(rec: Recording): Int = {
    val result = sendCommand("FORGET_RECORDING", rec)
    (result map { case r: Int => r }).get
  }

  def freeTuner(cardId: CaptureCardId): Boolean = {
    val result = sendCommand("FREE_TUNER", cardId)
    (result map { case r: Boolean => r }).get
  }

  def getFreeRecorder: RemoteEncoder = {
    val result = sendCommand("GET_FREE_RECORDER")
    (result map { case e: RemoteEncoder => e }).get
  }

  def getFreeRecorderCount: Int = {
    val result = sendCommand("GET_FREE_RECORDER_COUNT")
    (result map { case n: Int => n }).get
  }

  def getFreeRecorderList: List[CaptureCardId] = {
    val result = sendCommand("GET_FREE_RECORDER_LIST")
    (result map { case xs: List[_] => xs.asInstanceOf[List[CaptureCardId]] }).get
  }

  def getNextFreeRecorder(cardId: CaptureCardId): RemoteEncoder = {
    val result = sendCommand("GET_NEXT_FREE_RECORDER", cardId)
    (result map { case e: RemoteEncoder => e }).get
  }

  def getRecorderFromNum(cardId: CaptureCardId): RemoteEncoder = {
    val result = sendCommand("GET_RECORDER_FROM_NUM", cardId)
    (result map { case e: RemoteEncoder => e }).get
  }

  def getRecorderNum(rec: Recording): RemoteEncoder = {
    val result = sendCommand("GET_RECORDER_NUM", rec)
    (result map { case e: RemoteEncoder => e }).get
  }

  // TODO a way to return error message if any
  def goToSleep(): Boolean = {
    val result = sendCommand("GO_TO_SLEEP")
    (result map { case r: Boolean => r }).get
  }

  def lockTuner(): Any = ??? // TODO capture the appropriate return type
  def lockTuner(cardId: CaptureCardId): Any = ???// see above for return type

  def message(message: String, extra: String*): Boolean = {
    val args = List(message) ++ extra
    val result = sendCommand("MESSAGE", args: _*)
    (result map { case r: Boolean => r }).get
  }

  def messageSetLogLevel(logLevel: String): Boolean = {
    val result = sendCommand("MESSAGE", "SET_LOG_LEVEL", logLevel)
    (result map { case r: Boolean => r }).get
  }

  def messageSetVerbose(verboseMask: String): Boolean = {
    val result = sendCommand("MESSAGE", "SET_VERBOSE", verboseMask)
    (result map { case r: Boolean => r }).get
  }

  def protocolVersion(version: Int, token: String): (Boolean, Int) = {
    // NOTE that I believe an incorrect protocol version results in socket being closed
    val result = sendCommand("MYTH_PROTO_VERSION", version, token)
    (result map { case (accepted: Boolean, acceptVer: Int) => (accepted, acceptVer) }).get
  }

  def queryActiveBackends: List[String] = {
    val result = sendCommand("QUERY_ACTIVE_BACKENDS")
    (result map { case xs: List[_] => xs.asInstanceOf[List[String]] }).get
  }

  def queryBookmark(chanId: ChanId, startTime: MythDateTime): VideoPosition = {
    val result = sendCommand("QUERY_BOOKMARK", chanId, startTime)
    (result map { case p: VideoPosition => p }).get
  }

  def queryCheckFile(rec: Recording, checkSlaves: Boolean): String = {
    val result = sendCommand("QUERY_CHECKFILE", checkSlaves, rec)
    (result map { case s: String => s }).get
  }

  def queryCommBreak(chanId: ChanId, startTime: MythDateTime): List[VideoSegment] = {
    val result = sendCommand("QUERY_COMMBREAK", chanId, startTime)
    (result map { case xs: List[_] => xs.asInstanceOf[List[VideoSegment]] }).get
  }

  def queryCutList(chanId: ChanId, startTime: MythDateTime): List[VideoSegment] = {
    val result = sendCommand("QUERY_CUTLIST", chanId, startTime)
    (result map { case xs: List[_] => xs.asInstanceOf[List[VideoSegment]] }).get
  }

  // TODO storageGroup is optional parameter ....
  def queryFileExists(fileName: String, storageGroup: String): (String, FileStats) = {
    val result = sendCommand("QUERY_FILE_EXISTS", fileName, storageGroup)
    (result map { case (fullName: String, stats: FileStats) => (fullName, stats) }).get
  }

  def queryFileHash(fileName: String, storageGroup: String, hostName: String): String = {
    val result =
      if (hostName == "") sendCommand("QUERY_FILE_HASH", fileName, storageGroup)
      else sendCommand("QUERY_FILE_HASH", fileName, storageGroup, hostName)
    (result map { case h: String => h }).get
  }

  def queryFreeSpace: List[FreeSpace] = {
    val result = sendCommand("QUERY_FREE_SPACE")
    (result map { case xs: List[_] => xs.asInstanceOf[List[FreeSpace]] }).get
  }

  def queryFreeSpaceList: List[FreeSpace] = {
    val result = sendCommand("QUERY_FREE_SPACE_LIST")
    (result map { case xs: List[_] => xs.asInstanceOf[List[FreeSpace]] }).get
  }

  def queryFreeSpaceSummary: (ByteCount, ByteCount) = {
    val result = sendCommand("QUERY_FREE_SPACE_SUMMARY")
    (result map { case (total: ByteCount, used: ByteCount) => (total, used) }).get
  }

  def queryGetAllPending: ExpectedCountIterator[Recording] = {
    val result = sendCommand("QUERY_GETALLPENDING")
    (result map { case it: ExpectedCountIterator[_] => it.asInstanceOf[ExpectedCountIterator[Recording]] }).get
  }

  def queryGetAllScheduled: ExpectedCountIterator[Recording] = {
    val result = sendCommand("QUERY_GETALLSCHEDULED")
    (result map { case it: ExpectedCountIterator[_] => it.asInstanceOf[ExpectedCountIterator[Recording]] }).get
  }

  def queryGetConflicting: Iterable[Recording] = ??? // TODO expected count iterator?

  def queryGetExpiring: ExpectedCountIterator[Recording] = {
    val result = sendCommand("QUERY_GETEXPIRING")
    (result map { case it: ExpectedCountIterator[_] => it.asInstanceOf[ExpectedCountIterator[Recording]] }).get
  }

  def queryGuideDataThrough: MythDateTime = {
    val result = sendCommand("QUERY_GUIDEDATATHROUGH")
    (result map { case d: MythDateTime => d }).get
  }

  def queryHostname: String = {
    val result = sendCommand("QUERY_HOSTNAME")
    (result map { case h: String => h }).get
  }

  def queryIsActiveBackend(hostName: String): Boolean = {
    val result = sendCommand("QUERY_IS_ACTIVE_BACKEND", hostName)
    (result map { case a: Boolean => a }).get
  }

  def queryIsRecording: (Int, Int) = {
    val result = sendCommand("QUERY_ISRECORDING")
    (result map { case (rec: Int, live: Int) => (rec, live) }).get
  }

  def queryLoad: (Double, Double, Double) = {
    val result = sendCommand("QUERY_LOAD")
    (result map { case (one: Double, five: Double, fifteen: Double) => (one, five, fifteen) }).get
  }

  def queryMemStats: (ByteCount, ByteCount, ByteCount, ByteCount) = {
    val result = sendCommand("QUERY_MEMSTATS")
    (result map {
      case (total: ByteCount, free: ByteCount, totalVM: ByteCount, freeVM: ByteCount) =>
        (total, free, totalVM, freeVM)
    }).get
  }

  def queryPixmapGetIfModified(maxFileSize: Long, rec: Recording): (MythDateTime, Option[PixmapInfo]) = {
    val result = sendCommand("QUERY_PIXMAP_GET_IF_MODIFIED", maxFileSize, rec)
    (result map {
      case (lastModified: MythDateTime, pixmapInfo: Option[_]) =>
        (lastModified, pixmapInfo map { case (p: PixmapInfo) => p })
    }).get
  }

  def queryPixmapGetIfModified(modifiedSince: MythDateTime, maxFileSize: Long, rec: Recording): (MythDateTime, Option[PixmapInfo]) = {
    val result = sendCommand("QUERY_PIXMAP_GET_IF_MODIFIED", modifiedSince, maxFileSize, rec)
    (result map {
      case (lastModified: MythDateTime, pixmapInfo: Option[_]) =>
        (lastModified, pixmapInfo map { case (p: PixmapInfo) => p })
    }).get
  }

  def queryPixmapLastModified(rec: Recording): MythDateTime = {
    val result = sendCommand("QUERY_PIXMAP_LASTMODIFIED", rec)
    (result map { case d: MythDateTime => d }).get
  }

  def queryRecorderCancelNextRecording(cardId: CaptureCardId, cancel: Boolean): Unit = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CANCEL_NEXT_RECORDING", cancel)
  }

  def queryRecorderChangeBrightness(cardId: CaptureCardId, adjType: PictureAdjustType, up: Boolean): Int = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CHANGE_BRIGHTNESS", adjType, up)
    (result map { case QueryRecorderPictureAttribute(a) => a }).get
  }

  def queryRecorderChangeChannel(cardId: CaptureCardId, dir: ChannelChangeDirection): Unit = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CHANGE_CHANNEL", dir)
  }

  def queryRecorderChangeColour(cardId: CaptureCardId, adjType: PictureAdjustType, up: Boolean): Int = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CHANGE_COLOUR", adjType, up)
    (result map { case QueryRecorderPictureAttribute(a) => a }).get
  }

  def queryRecorderChangeContrast(cardId: CaptureCardId, adjType: PictureAdjustType, up: Boolean): Int = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CHANGE_CONTRAST", adjType, up)
    (result map { case QueryRecorderPictureAttribute(a) => a }).get
  }

  def queryRecorderChangeHue(cardId: CaptureCardId, adjType: PictureAdjustType, up: Boolean): Int = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CHANGE_HUE", adjType, up)
    (result map { case QueryRecorderPictureAttribute(a) => a }).get
  }

  def queryRecorderCheckChannel(cardId: CaptureCardId, channum: String): Boolean = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CHECK_CHANNEL", channum)
    (result map { case QueryRecorderBoolean(b) => b }).get
  }

  def queryRecorderCheckChannelPrefix(cardId: CaptureCardId, channumPrefix: String):
      (Boolean, Option[CaptureCardId], Boolean, String) = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CHECK_CHANNEL_PREFIX", channumPrefix)
    (result map { case QueryRecorderCheckChannelPrefix(matched, cardId, extraCharUseful, spacer) =>
      (matched, cardId, extraCharUseful, spacer) }).get
  }
  // TODO return type more specific?
  def queryRecorderFillDurationMap(cardId: CaptureCardId, start: VideoPosition, end: VideoPosition): Map[VideoPosition, Long] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "FILL_DURATION_MAP", start, end)
    (result map { case QueryRecorderPositionMap(m) => m }).get
  }

  // TODO return type more specific?
  def queryRecorderFillPositionMap(cardId: CaptureCardId, start: VideoPosition, end: VideoPosition): Map[VideoPosition, Long] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "FILL_POSITION_MAP", start, end)
    (result map { case QueryRecorderPositionMap(m) => m }).get
  }

  def queryRecorderFinishRecording(cardId: CaptureCardId): Unit = {
    val result = sendCommand("QUERY_RECORDER", cardId, "FINISH_RECORDING")
  }

  def queryRecorderFrontendReady(cardId: CaptureCardId): Unit = {
    val result = sendCommand("QUERY_RECORDER", cardId, "FRONTEND_READY")
  }

  def queryRecorderGetBrightness(cardId: CaptureCardId): Int = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_BRIGHTNESS")
    (result map { case QueryRecorderPictureAttribute(a) => a }).get
  }

  def queryRecorderGetChannelInfo(cardId: CaptureCardId, chanId: ChanId): Channel = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_CHANNEL_INFO", chanId)
    (result map { case QueryRecorderChannelInfo(c) => c }).get
  }

  def queryRecorderGetColour(cardId: CaptureCardId): Int = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_COLOUR")
    (result map { case QueryRecorderPictureAttribute(a) => a }).get
  }

  def queryRecorderGetContrast(cardId: CaptureCardId): Int = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_CONTRAST")
    (result map { case QueryRecorderPictureAttribute(a) => a }).get
  }

  def queryRecorderGetCurrentRecording(cardId: CaptureCardId): Recording = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_CURRENT_RECORDING")
    (result map { case QueryRecorderRecording(r) => r }).get
  }

  def queryRecorderGetFilePosition(cardId: CaptureCardId): Long = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_FILE_POSITION")
    (result map { case QueryRecorderPosition(p) => p }).get
  }

  def queryRecorderGetFrameRate(cardId: CaptureCardId): Double = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_FRAMERATE")
    (result map { case QueryRecorderFrameRate(r) => r }).get
  }

  def queryRecorderGetFramesWritten(cardId: CaptureCardId): Long = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_FRAMES_WRITTEN")
    (result map { case QueryRecorderFrameCount(n) => n }).get
  }

  def queryRecorderGetFreeInputs(cardId: CaptureCardId, excludedCards: CaptureCardId*): List[CardInput] = {
    val args = List(cardId) ++ excludedCards
    val result = sendCommand("QUERY_RECORDER", args: _*)
    (result map { case QueryRecorderCardInputList(i) => i }).get
  }

  def queryRecorderGetHue(cardId: CaptureCardId): Int = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_HUE")
    (result map { case QueryRecorderPictureAttribute(a) => a }).get
  }

  def queryRecorderGetInput(cardId: CaptureCardId): String = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_INPUT")
    (result map { case QueryRecorderInput(input) => input }).get
  }

  def queryRecorderGetKeyframePos(cardId: CaptureCardId, desiredPos: VideoPosition): Long = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_KEYFRAME_POS", desiredPos)
    (result map { case QueryRecorderPosition(p) => p }).get
  }

  def queryRecorderGetMaxBitrate(cardId: CaptureCardId): Long = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_MAX_BITRATE")
    (result map { case QueryRecorderBitrate(b) => b }).get
  }

  def queryRecorderGetNextProgramInfo(cardId: CaptureCardId, chanId: ChanId, dir: ChannelBrowseDirection,
    startTime: MythDateTime): UpcomingProgram = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_NEXT_PROGRAM_INFO", "", chanId, dir, startTime)
    (result map { case QueryRecorderNextProgramInfo(p) => p }).get
  }

  def queryRecorderGetNextProgramInfo(cardId: CaptureCardId, channum: String, dir: ChannelBrowseDirection,
    startTime: MythDateTime): UpcomingProgram = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_NEXT_PROGRAM_INFO", channum, ChanId(0), dir, startTime)
    (result map { case QueryRecorderNextProgramInfo(p) => p }).get
  }

  def queryRecorderGetRecording(cardId: CaptureCardId): Recording = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_RECORDING")
    (result map { case QueryRecorderRecording(r) => r }).get
  }

  def queryRecorderIsRecording(cardId: CaptureCardId): Boolean = {
    val result = sendCommand("QUERY_RECORDER", cardId, "IS_RECORDING")
    (result map { case QueryRecorderBoolean(b) => b }).get
  }

  def queryRecorderPause(cardId: CaptureCardId): Unit = {
    val result = sendCommand("QUERY_RECORDER", cardId, "PAUSE")
  }

  def queryRecorderSetChannel(cardId: CaptureCardId, channum: String): Unit = {
    val result = sendCommand("QUERY_RECORDER", cardId, "SET_CHANNEL", channum)
  }

  def queryRecorderSetInput(cardId: CaptureCardId, inputName: String): String = {
    val result = sendCommand("QUERY_RECORDER", cardId, "SET_INPUT", inputName)
    (result map { case QueryRecorderInput(input) => input }).get
  }

  def queryRecorderSetLiveRecording(cardId: CaptureCardId, recordingState: Int): Unit = {
    val result = sendCommand("QUERY_RECORDER", cardId, "SET_LIVE_RECORDING", recordingState)
  }

  def queryRecorderSetSignalMonitoringRate(cardId: CaptureCardId, rate: Int, notifyFrontend: Boolean): Boolean = {
    val result = sendCommand("QUERY_RECORDER", cardId, "SET_SIGNAL_MONITORING_RATE", rate, notifyFrontend)
    (result map { case QueryRecorderBoolean(b) => b }).get
  }

  def queryRecorderShouldSwitchCard(cardId: CaptureCardId, chanId: ChanId): Boolean = {
    val result = sendCommand("QUERY_RECORDER", cardId, "SHOULD_SWITCH_CARD", chanId)
    (result map { case QueryRecorderBoolean(b) => b }).get
  }

  def queryRecorderSpawnLiveTV(cardId: CaptureCardId, usePiP: Boolean, channumStart: String): Unit = {
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
    (result map { case r: Recording => r }).get
  }

  def queryRecording(chanId: ChanId, startTime: MythDateTime): Recording = {
    val result = sendCommand("QUERY_RECORDING", "TIMESLOT", chanId, startTime)
    (result map { case r: Recording => r }).get
  }

  def queryRecordings(specifier: String): ExpectedCountIterator[Recording] = {
    val result = sendCommand("QUERY_RECORDINGS", specifier)
    (result map { case it: ExpectedCountIterator[_] => it.asInstanceOf[ExpectedCountIterator[Recording]] }).get
  }

  def querySGGetFileList(hostName: String, storageGroup: String, path: String): List[String] = {
    val result = sendCommand("QUERY_SG_GETFILELIST", hostName, storageGroup, path)
    (result map { case xs: List[_] => xs.asInstanceOf[List[String]] }).get
  }

  def querySGGetFileList(hostName: String, storageGroup: String, path: String, fileNamesOnly: Boolean): List[String] = {
    val result = sendCommand("QUERY_SG_GETFILELIST", hostName, storageGroup, path, fileNamesOnly)
    (result map { case xs: List[_] => xs.asInstanceOf[List[String]] }).get
  }

  def querySGFileQuery(hostName: String, storageGroup: String, fileName: String): (String, MythDateTime, ByteCount) = {
    val result = sendCommand("QUERY_SG_FILEQUERY", hostName, storageGroup, fileName)
    (result map { case (fullPath: String, fileTime: MythDateTime, fileSize: ByteCount) => (fullPath, fileTime, fileSize) }).get
  }

  def querySetting(hostName: String, settingName: String): Option[String] = {
    val result = sendCommand("QUERY_SETTING", hostName, settingName)
    result map { case s: String => s }
  }

  def queryTimeZone: (String, ZoneOffset, Instant) = {
    val result = sendCommand("QUERY_TIME_ZONE")
    (result map { case (tzName: String, offset: ZoneOffset, time: Instant) => (tzName, offset, time) }).get
  }

  def queryUptime: Duration = {
    val result = sendCommand("QUERY_UPTIME")
    (result map { case d: Duration => d }).get
  }

  def refreshBackend: Boolean = {
    val result = sendCommand("REFRESH_BACKEND")
    (result map { case r: Boolean => r }).get
  }

  def rescheduleRecordingsCheck(recStatus: RecStatus, recordId: RecordRuleId, findId: Int, title: String, subtitle: String,
    description: String, programId: String, reason: String): Boolean = {
    val result = sendCommand("RESCHEDULE_RECORDINGS", "CHECK", recStatus, recordId, findId, reason,
      title, subtitle, description, programId)
    (result map { case r: Boolean => r }).get
  }

  def rescheduleRecordingsMatch(recordId: RecordRuleId, sourceId: ListingSourceId, mplexId: Int,
    maxStartTime: Option[MythDateTime], reason: String): Boolean = {
    val result =
      if (maxStartTime.isEmpty) sendCommand("RESCHEDULE_RECORDINGS", "MATCH", recordId, sourceId, mplexId, reason)
      else sendCommand("RESCHEDULE_RECORDINGS", "MATCH", recordId, sourceId, mplexId, maxStartTime.get, reason)
    (result map { case r: Boolean => r }).get
  }

  def rescheduleRecordingsPlace(reason: String): Boolean = {
    val result = sendCommand("RESCHEDULE_RECORDINGS", "PLACE", reason)
    (result map { case r: Boolean => r }).get
  }

  def scanVideos: Boolean = {
    // TODO this may need a longer timeout, may take some time? Is this true?
    val result = sendCommand("SCAN_VIDEOS")
    (result map { case r: Boolean => r }).get
  }

  def setBookmark(chanId: ChanId, startTime: MythDateTime, pos: VideoPosition): Boolean = {
    val result = sendCommand("SET_BOOKMARK", chanId, startTime, pos)
    (result map { case r: Boolean => r }).get
  }

  def setSetting(hostName: String, settingName: String, value: String): Boolean = {
    val result = sendCommand("SET_SETTING", hostName, settingName, value)
    (result map { case r: Boolean => r }).get
  }

  // TODO do we need to post this message rather than send it?
  def shutdownNow(haltCommand: String): Unit = {
    if (haltCommand == "") sendCommand("SHUTDOWN_NOW")
    else sendCommand("SHUTDOWN_NOW", haltCommand)
  }

  // TODO better encapsulate return codes
  def stopRecording(rec: Recording): Int = {
    val result = sendCommand("STOP_RECORDING", rec)
    (result map { case e: Int => e }).get
  }

  def undeleteRecording(rec: Recording): Boolean = {
    val result = sendCommand("UNDELETE_RECORDING", rec)
    (result map { case r: Boolean => r }).get
  }

  def undeleteRecording(chanId: ChanId, startTime: MythDateTime): Boolean = {
    val result = sendCommand("UNDELETE_RECORDING", chanId, startTime)
    (result map { case r: Boolean => r }).get
  }

}
