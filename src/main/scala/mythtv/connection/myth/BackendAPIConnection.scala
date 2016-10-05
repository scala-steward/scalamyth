package mythtv
package connection
package myth

import java.time.{ Duration, Instant, ZoneOffset }

import model.{ CaptureCardId, ChanId, FreeSpace, Recording, RemoteEncoder, VideoPosition, VideoSegment }
import util.{ ByteCount, ExpectedCountIterator, FileStats, MythDateTime }

class BackendAPIConnection(host: String, port: Int, timeout: Int, blockShutdown: Boolean)
    extends BackendConnection(host, port, timeout, blockShutdown)
    with MythProtocol
    with MythProtocolAPI {

  def this(host: String, port: Int, timeout: Int) = this(host, port, timeout, false)
  def this(host: String, port: Int) = this(host, port, 10)

  /*private*/ def execute(command: String, args: Any*): Option[_] = {
    if (!isConnected) throw new IllegalStateException
    if (commands contains command) {
      val (check, serialize, handle) = commands(command)
      if (check(args)) {
        val cmdstring = serialize(command, args)
        val response = sendCommand(cmdstring).get
        handle(response)
      }
      else {
        println("failed argument type check")
        None
      }
    } else {
      println(s"invalid command $command")
      None
    }
  }

  def allowShutdown(): Boolean = {
    val result = execute("ALLOW_SHUTDOWN")
    (result map { case r: Boolean => r }).get
  }

//  def blockShutdown(): Boolean = ??? // TODO name clash with var in BackendConnection (and constructor param here)

  def checkRecording(rec: Recording): Boolean = {
    val result = execute("CHECK_RECORDING", rec)
    (result map { case r: Boolean => r }).get
  }

  def deleteRecording(rec: Recording): Int = {
    val result = execute("DELETE_RECORDING", rec)
    (result map { case r: Int => r }).get
  }

  def done(): Unit = ???

  def fillProgramInfo(playbackHost: String, p: Recording): Recording = ???

  // TODO is the result here really Int or Boolean
  def forceDeleteRecording(rec: Recording): Int = {
    val result = execute("FORCE_DELETE_RECORDING", rec)
    (result map { case r: Int => r }).get
  }

  // TODO is the result here really Int or Boolean
  def forgetRecording(rec: Recording): Int = {
    val result = execute("FORGET_RECORDING", rec)
    (result map { case r: Int => r }).get
  }

  def getFreeRecorder: RemoteEncoder = {
    val result = execute("GET_FREE_RECORDER")
    (result map { case e: RemoteEncoder => e }).get
  }

  def getFreeRecorderCount: Int = {
    val result = execute("GET_FREE_RECORDER_COUNT")
    (result map { case n: Int => n }).get
  }

  def getFreeRecorderList: List[CaptureCardId] = {
    val result = execute("GET_FREE_RECORDER_LIST")
    (result map { case xs: List[_] => xs.asInstanceOf[List[CaptureCardId]] }).get
  }

  def getNextFreeRecorder(cardId: CaptureCardId): RemoteEncoder = {
    val result = execute("GET_NEXT_FREE_RECORDER", cardId)
    (result map { case e: RemoteEncoder => e }).get
  }

  def getRecorderFromNum(encoderId: Int): Any = ??? // see above for return type

  def getRecorderNum(rec: Recording): RemoteEncoder = {
    val result = execute("GET_RECORDER_NUM", rec)
    (result map { case e: RemoteEncoder => e }).get
  }

  // TODO a way to return error message if any
  def goToSleep(): Boolean = {
    val result = execute("GO_TO_SLEEP")
    (result map { case r: Boolean => r }).get
  }

  def lockTuner(): Any = ??? // TODO capture the appropriate return type
  def lockTuner(cardId: Int): Any = ???// see above for return type

  def protocolVersion(version: Int, token: String): (Boolean, Int) = {
    // NOTE that I believe an incorrect protocol version results in socket being closed
    val result = execute("MYTH_PROTO_VERSION", version, token)
    (result map { case (accepted: Boolean, acceptVer: Int) => (accepted, acceptVer) }).get
  }

  def queryActiveBackends: List[String] = {
    val result = execute("QUERY_ACTIVE_BACKENDS")
    (result map { case xs: List[_] => xs.asInstanceOf[List[String]] }).get
  }

  def queryBookmark(chanId: ChanId, startTime: MythDateTime): VideoPosition = {
    val result = execute("QUERY_BOOKMARK", chanId, startTime)
    (result map { case p: VideoPosition => p }).get
  }

  def queryCommBreak(chanId: ChanId, startTime: MythDateTime): List[VideoSegment] = {
    val result = execute("QUERY_COMMBREAK", chanId, startTime)
    (result map { case xs: List[_] => xs.asInstanceOf[List[VideoSegment]] }).get
  }

  def queryCutList(chanId: ChanId, startTime: MythDateTime): List[VideoSegment] = {
    val result = execute("QUERY_CUTLIST", chanId, startTime)
    (result map { case xs: List[_] => xs.asInstanceOf[List[VideoSegment]] }).get
  }

  // TODO storageGroup is optional parameter ....
  def queryFileExists(fileName: String, storageGroup: String): (String, FileStats) = {
    val result = execute("QUERY_FILE_EXISTS", fileName, storageGroup)
    (result map { case (fullName: String, stats: FileStats) => (fullName, stats) }).get
  }

  def queryFileHash(fileName: String, storageGroup: String, hostName: String = ""): String = {
    val result =
      if (hostName == "") execute("QUERY_FILE_HASH", fileName, storageGroup)
      else execute("QUERY_FILE_HASH", fileName, storageGroup, hostName)
    (result map { case h: String => h }).get
  }

  def queryFreeSpace: List[FreeSpace] = {
    val result = execute("QUERY_FREE_SPACE")
    (result map { case xs: List[_] => xs.asInstanceOf[List[FreeSpace]] }).get
  }

  def queryFreeSpaceList: List[FreeSpace] = {
    val result = execute("QUERY_FREE_SPACE_LIST")
    (result map { case xs: List[_] => xs.asInstanceOf[List[FreeSpace]] }).get
  }

  def queryFreeSpaceSummary: (ByteCount, ByteCount) = {
    val result = execute("QUERY_FREE_SPACE_SUMMARY")
    (result map { case (total: ByteCount, used: ByteCount) => (total, used) }).get
  }

  def queryGetAllPending: ExpectedCountIterator[Recording] = {
    val result = execute("QUERY_GETALLPENDING")
    (result map { case it: ExpectedCountIterator[_] => it.asInstanceOf[ExpectedCountIterator[Recording]] }).get
  }

  def queryGetAllScheduled: ExpectedCountIterator[Recording] = {
    val result = execute("QUERY_GETALLSCHEDULED")
    (result map { case it: ExpectedCountIterator[_] => it.asInstanceOf[ExpectedCountIterator[Recording]] }).get
  }

  def queryGetConflicting: Iterable[Recording] = ??? // TODO expected count iterator?

  def queryGetExpiring: ExpectedCountIterator[Recording] = {
    val result = execute("QUERY_GETEXPIRING")
    (result map { case it: ExpectedCountIterator[_] => it.asInstanceOf[ExpectedCountIterator[Recording]] }).get
  }

  def queryGuideDataThrough: MythDateTime = {
    val result = execute("QUERY_GUIDEDATATHROUGH")
    (result map { case d: MythDateTime => d }).get
  }

  def queryHostname: String = {
    val result = execute("QUERY_HOSTNAME")
    (result map { case h: String => h }).get
  }

  def queryIsActiveBackend(hostName: String): Boolean = {
    val result = execute("QUERY_IS_ACTIVE_BACKEND", hostName)
    (result map { case a: Boolean => a }).get
  }

  def queryIsRecording: (Int, Int) = {
    val result = execute("QUERY_ISRECORDING")
    (result map { case (rec: Int, live: Int) => (rec, live) }).get
  }

  def queryLoad: (Double, Double, Double) = {
    val result = execute("QUERY_LOAD")
    (result map { case (one: Double, five: Double, fifteen: Double) => (one, five, fifteen) }).get
  }

  def queryMemStats: (ByteCount, ByteCount, ByteCount, ByteCount) = {
    val result = execute("QUERY_MEMSTATS")
    (result map {
      case (total: ByteCount, free: ByteCount, totalVM: ByteCount, freeVM: ByteCount) =>
        (total, free, totalVM, freeVM)
    }).get
  }

  def queryPixmapLastModified(rec: Recording): MythDateTime = {
    val result = execute("QUERY_PIXMAP_LASTMODIFIED", rec)
    (result map { case d: MythDateTime => d }).get
  }

  def queryRecording(pathName: String): Recording = {
    val result = execute("QUERY_RECORDING", "BASENAME", pathName)
    (result map { case r: Recording => r }).get
  }

  def queryRecording(chanId: ChanId, startTime: MythDateTime): Recording = {
    val result = execute("QUERY_RECORDING", "TIMESLOT", chanId, startTime)
    (result map { case r: Recording => r }).get
  }

  def querySetting(hostName: String, settingName: String): Option[String] = {
    val result = execute("QUERY_SETTING", hostName, settingName)
    result map { case s: String => s }
  }

  def queryTimeZone: (String, ZoneOffset, Instant) = {
    val result = execute("QUERY_TIME_ZONE")
    (result map { case (tzName: String, offset: ZoneOffset, time: Instant) => (tzName, offset, time) }).get
  }

  def queryUptime: Duration = {
    val result = execute("QUERY_UPTIME")
    (result map { case d: Duration => d }).get
  }

  def refreshBackend: Boolean = {
    val result = execute("REFRESH_BACKEND")
    (result map { case r: Boolean => r }).get
  }

  def scanVideos: Boolean = {
    // TODO this may need a longer timeout, may take some time? Is this true?
    val result = execute("SCAN_VIDEOS")
    (result map { case r: Boolean => r }).get
  }

  def setBookmark(chanId: ChanId, startTime: MythDateTime, pos: VideoPosition): Boolean = {
    val result = execute("SET_BOOKMARK", chanId, startTime, pos)
    (result map { case r: Boolean => r }).get
  }

  def setSetting(hostName: String, settingName: String, value: String): Boolean = {
    val result = execute("SET_SETTING", hostName, settingName, value)
    (result map { case r: Boolean => r }).get
  }

  // TODO do we need to post this message rather than send it?
  def shutdownNow(haltCommand: String = ""): Unit = {
    if (haltCommand == "") execute("SHUTDOWN_NOW")
    else execute("SHUTDOWN_NOW", haltCommand)
  }

  // TODO better encapsulate return codes
  def stopRecording(rec: Recording): Int = {
    val result = execute("STOP_RECORDING", rec)
    (result map { case e: Int => e }).get
  }

  def undeleteRecording(rec: Recording): Boolean = {
    val result = execute("UNDELETE_RECORDING", rec)
    (result map { case r: Boolean => r }).get
  }

  def undeleteRecording(chanId: ChanId, startTime: MythDateTime): Boolean = {
    val result = execute("UNDELETE_RECORDING", chanId, startTime)
    (result map { case r: Boolean => r }).get
  }

}
