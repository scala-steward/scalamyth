package mythtv
package connection
package myth

import java.net.InetAddress
import java.time.{ Duration, Instant, ZoneOffset }

import model.{ CaptureCardId, ChanId, FreeSpace, Recording, RemoteEncoder, VideoPosition, VideoSegment }
import util.{ ByteCount, ExpectedCountIterator, FileStats, MythDateTime }
import EnumTypes.MythProtocolEventMode

private[myth] trait BackendAPILike {
  self: MythProtocolLike =>

  def allowShutdown(): Boolean = {
    val result = sendCommand("ALLOW_SHUTDOWN")
    (result map { case r: Boolean => r }).get
  }

  def announce(mode: String, hostName: String, eventMode: MythProtocolEventMode): Boolean = {
    val localHost =
      if (hostName != "") hostName
      else InetAddress.getLocalHost().getHostName()
    val result = sendCommand("ANN", mode, localHost, eventMode)
    (result map { case r: Boolean => r }).get
  }

  def announceFileTransfer(hostName: String, fileName: String, storageGroup: String,
    writeMode: Boolean = false,
    useReadAhead: Boolean = true,
    timeout: Duration = Duration.ofSeconds(2)
  ): (Int, ByteCount) = {
    val result = sendCommand("ANN", "FileTransfer", hostName, writeMode, useReadAhead, timeout, fileName, storageGroup)
    (result map { case (ftId: Int, fileSize: ByteCount) => (ftId, fileSize) }).get
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
