package mythtv
package connection
package myth

import java.time.{ Duration, Instant, LocalDate, ZoneOffset }

import model.{ CaptureCardId, ChanId, FreeSpace, Markup, RecordedMarkup, Recording, RemoteEncoder, VideoPosition, VideoSegment }
import util.{ ByteCount, BinaryByteCount, DecimalByteCount, ExpectedCountIterator, FileStats, MythDateTime, MythDateTimeString }

trait MythProtocolLike extends MythProtocolSerializer {
  import MythProtocol._
  // TODO can we structure this so that it's possible to support more than one protocol version?
  final val PROTO_VERSION = 77
  final val PROTO_TOKEN = "WindMark"

  type CheckArgs = (Seq[Any]) => Boolean
  type Serialize = (String, Seq[Any]) => String
  type HandleResponse = (BackendResponse) => Option[_]  // TODO what is result type?, maybe Either[_]

  // TODO FIXME would be very useful to have access to data from CheckArgs/Serialize during HandleResponse
  // TODO FIXME we lose the type of the option going through the message dispatch map
  //            is there a way around this?

  def commands: Map[String, (CheckArgs, Serialize, HandleResponse)] = internalMap

  protected def verifyArgsNOP(args: Seq[Any]): Boolean = true
  protected def serializeNOP(command: String, args: Seq[Any]) = ""

  protected def verifyArgsEmpty(args: Seq[Any]): Boolean = args match {
    case Seq() => true
    case _ => false
  }

  protected def serializeEmpty(command: String, args: Seq[Any]): String = args match {
    case Seq() => command
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsProgramInfo(args: Seq[Any]): Boolean = args match {
    case Seq(x: Recording) => true
    case _ => false
  }

  protected def serializeProgramInfo(command: String, args: Seq[Any]): String = args match {
    case Seq(rec: Recording) =>
      val bldr = new StringBuilder(command).append(BACKEND_SEP)
      implicit val piser = ProgramInfoSerializerCurrent
      serialize(rec, bldr).toString
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsChanIdStartTime(args: Seq[Any]): Boolean = args match {
    case Seq(chanId: ChanId, startTime: MythDateTime) => true
    case _ => false
  }

  protected def serializeChanIdStartTime(command: String, args: Seq[Any]): String = args match {
    case Seq(chanId: ChanId, startTime: MythDateTime) =>
      val elems = List(command, serialize(chanId), serialize(startTime))
      elems mkString " "
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsCaptureCard(args: Seq[Any]): Boolean = args match {
    case Seq(cardId: CaptureCardId) => true
    case _ => false
  }

  protected def serializeCaptureCard(command: String, args: Seq[Any]): String = args match {
    case Seq(cardId: CaptureCardId) =>
      val elems = List(command, serialize(cardId))
      elems mkString BACKEND_SEP
    case _ => throw new IllegalArgumentException
  }

  /***/

  protected def verifyArgsAnnounce(args: Seq[Any]): Boolean = args match {
    case Seq("Monitor", clientHostName: String, eventsMode: Int) => true
    case Seq("Playback", clientHostName: String, eventsMode: Int) => true
    case Seq("MediaServer", clientHostName: String) => true
      // TODO SlaveBackend and FileTransfer are more complex
    case _ => false
  }

  protected def serializeAnnounce(command: String, args: Seq[Any]): String = args match {
    case Seq(mode @ "Monitor", clientHostName: String, eventsMode: Int) =>
      val elems = List(command, mode, clientHostName, serialize(eventsMode))
      elems mkString " "
    case Seq(mode @ "Playback", clientHostName: String, eventsMode: Int) =>
      val elems = List(command, mode, clientHostName, serialize(eventsMode))
      elems mkString " "
    case Seq(mode @ "MediaServer", clientHostName: String) =>
      val elems = List(command, mode, clientHostName)
      elems mkString " "
     // TODO SlaveBackend and FileTransfer are more complex
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsDeleteFile(args: Seq[Any]): Boolean = args match {
    case Seq(fileName: String, storageGroup: String) => true
    case _ => false
  }

  protected def serializeDeleteFile(command: String, args: Seq[Any]): String = args match {
    case Seq(fileName: String, storageGroup: String) =>
      val elems = List(command, fileName, storageGroup)
      elems mkString BACKEND_SEP
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsDeleteRecording(args: Seq[Any]): Boolean = args match {
    case Seq(x: Recording) => true
    case Seq(chanId: ChanId, startTime: MythDateTime) => true
    case Seq(chanId: ChanId, startTime: MythDateTime, forceOpt: String) => true
    case Seq(chanId: ChanId, startTime: MythDateTime, forceOpt: String, forgetOpt: String) => true
    case _ => false
  }

  protected def serializeDeleteRecording(command: String, args: Seq[Any]): String = {
    def ser(chanId: ChanId, startTime: MythDateTime, forceOpt: Option[String], forgetOpt: Option[String]): String = {
      val builder = new StringBuilder(command)
      val time: MythDateTimeString = startTime
      serialize(chanId, builder.append(' '))
      serialize(time, builder.append(' '))
      if (forceOpt.nonEmpty) builder.append(' ').append(forceOpt.get)
      if (forgetOpt.nonEmpty) builder.append(' ').append(forgetOpt.get)
      builder.toString
    }
    args match {
      case Seq(rec: Recording) => serializeProgramInfo(command, args) // TODO this will pattern match again
      case Seq(chanId: ChanId, startTime: MythDateTime) => ser(chanId, startTime, None, None)
      case Seq(chanId: ChanId, startTime: MythDateTime, forceOpt: String) => ser(chanId, startTime, Some(forceOpt), None)
      case Seq(chanId: ChanId, startTime: MythDateTime, forceOpt: String, forgetOpt: String) =>
        ser(chanId, startTime, Some(forceOpt), Some(forgetOpt))
      case _ => throw new IllegalArgumentException
    }
  }

  protected def verifyArgsDownloadFile(args: Seq[Any]): Boolean = args match {
    case Seq(srcURL: String, storageGroup: String, fileName: String) => true
    case _ => false
  }

  protected def serializeDownloadFile(command: String, args: Seq[Any]): String = args match {
    case Seq(srcURL: String, storageGroup: String, fileName: String) =>
      val elems = List(command, srcURL, storageGroup, fileName)
      elems mkString BACKEND_SEP
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsFreeTuner(args: Seq[Any]): Boolean = args match {
    case Seq(id: CaptureCardId) => true
    case _ => false
  }

  protected def serializeFreeTuner(command: String, args: Seq[Any]): String = args match {
    case Seq(cardId: CaptureCardId) =>
      val elems = List(command, serialize(cardId))
      elems mkString " "
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsLockTuner(args: Seq[Any]): Boolean = args match {
    case Seq(cardId: CaptureCardId) => true
    case Seq() => true
    case _ => false
  }

  protected def serializeLockTuner(command: String, args: Seq[Any]): String = args match {
    case Seq(cardId: CaptureCardId) =>
      val elems = List(command, serialize(cardId))
      elems mkString " "
    case Seq() => command
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsMythProtoVersion(args: Seq[Any]): Boolean = args match {
    case Seq(version: Int, token: String) => true
    case _ => false
  }

  protected def serializeMythProtoVersion(command: String, args: Seq[Any]): String = args match {
    case Seq(version: Int, token: String) =>
      val elems = List(command, serialize(version), token)
      elems mkString " "
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsQueryCheckFile(args: Seq[Any]): Boolean = args match {
    case Seq(checkSlaves: Boolean, rec: Recording) => true
    case _ => false
  }

  protected def serializeQueryCheckFile(command: String, args: Seq[Any]): String = args match {
    case Seq(checkSlaves: Boolean, rec: Recording) =>
      implicit val piser = ProgramInfoSerializerCurrent  // TODO FIXME shouldn't need to delclare here...
      val elems = List(command, serialize(checkSlaves), serialize(rec))
      elems mkString BACKEND_SEP
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsQueryFileExists(args: Seq[Any]): Boolean = args match {
    case Seq(fileName: String, storageGroup: String) => true
    case Seq(fileName: String) => true
    case _ => false
  }

  protected def serializeQueryFileExists(command: String, args: Seq[Any]): String = args match {
    case Seq(fileName: String, storageGroup: String) =>
      val elems = List(command, fileName, storageGroup)
      elems mkString BACKEND_SEP
    case Seq(fileName: String) =>
      val elems = List(command, fileName)
      elems mkString BACKEND_SEP
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsQueryFileHash(args: Seq[Any]): Boolean = args match {
    case Seq(fileName: String, storageGroup: String, hostName: String) => true
    case Seq(fileName: String, storageGroup: String) => true
    case _ => false
  }

  protected def serializeQueryFileHash(command: String, args: Seq[Any]): String = args match {
    case Seq(fileName: String, storageGroup: String, hostName: String) =>
      val elems = List(command, fileName, storageGroup, hostName)
      elems mkString BACKEND_SEP
    case Seq(fileName: String, storageGroup: String) =>
      val elems = List(command, fileName, storageGroup)
      elems mkString BACKEND_SEP
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsQueryGetAllPending(args: Seq[Any]): Boolean = args match {
    case Seq() => true
    // TODO: case with optional arguments; rarely used?
    case _ => false
  }

  protected def serializeQueryGetAllPending(command: String, args: Seq[Any]): String = args match {
    case Seq() => command
    // TODO: case with optional arguments; rarely used?
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsQueryIsActiveBackend(args: Seq[Any]): Boolean = args match {
    case Seq(hostName: String) => true
    case _ => false
  }

  protected def serializeQueryIsActiveBackend(command: String, args: Seq[Any]): String = args match {
    case Seq(hostName: String) =>
      val elems = List(command, hostName)
      elems mkString BACKEND_SEP
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsQueryPixmapGetIfModified(args: Seq[Any]): Boolean = args match {
    case Seq(modifiedSince: MythDateTime, maxFileSize: Long, rec: Recording) => true
    case Seq(maxFileSize: Long, rec: Recording) => true
    case _ => false
  }

  protected def serializeQueryPixmapGetIfModified(command: String, args: Seq[Any]): String = args match {
    // TODO use StringBuilder for efficiency?
    case Seq(modifiedSince: MythDateTime, maxFileSize: Long, rec: Recording) =>
      implicit val piser = ProgramInfoSerializerCurrent
      val elems = List(command, serialize(modifiedSince), serialize(maxFileSize), serialize(rec))
      elems mkString BACKEND_SEP
    case Seq(maxFileSize: Long, rec: Recording) =>
      implicit val piser = ProgramInfoSerializerCurrent
      val elems = List(command, "-1", serialize(maxFileSize), serialize(rec))
      elems mkString BACKEND_SEP
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsQueryRecording(args: Seq[Any]): Boolean = args match {
    case Seq("TIMESLOT", chanId: ChanId, startTime: MythDateTime) => true
    case Seq("BASENAME", basePathName: String) => true
    case _ => false
  }

  protected def serializeQueryRecording(command: String, args: Seq[Any]): String = args match {
    case Seq(sub @ "TIMESLOT", chanId: ChanId, startTime: MythDateTime) =>
      val time: MythDateTimeString = startTime
      val elems = List(command, sub, serialize(chanId), serialize(time))
      elems mkString " "
    case Seq(sub @ "BASENAME", basePathName: String) =>
      val elems = List(command, sub, basePathName)
      elems mkString " "
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsQueryRecordings(args: Seq[Any]): Boolean = args match {
    case Seq(sortOrFilter: String) => true
    case _ => false
  }

  protected def serializeQueryRecordings(command: String, args: Seq[Any]): String = args match {
    case Seq(sortOrFilter: String) =>
      val elems = List(command, sortOrFilter)
      elems mkString " "
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsQuerySGFileQuery(args: Seq[Any]): Boolean = args match {
    case Seq(hostName: String, storageGroup: String, fileName: String) => true
    case _ => false
  }

  protected def serializeQuerySGFileQuery(command: String, args: Seq[Any]): String = args match {
    case Seq(hostName: String, storageGroup: String, fileName: String) =>
      val elems = List(command, hostName, storageGroup, fileName)
      elems mkString BACKEND_SEP
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsQuerySGGetFileList(args: Seq[Any]): Boolean = args match {
    case Seq(hostName: String, storageGroup: String, path: String) => true
    case Seq(hostName: String, storageGroup: String, path: String, fileNamesOnly: Boolean) => true
    case _ => false
  }

  protected def serializeQuerySGGetFileList(command: String, args: Seq[Any]): String = args match {
    case Seq(hostName: String, storageGroup: String, path: String) =>
      val elems = List(command, hostName, storageGroup, path)
      elems mkString BACKEND_SEP
    case Seq(hostName: String, storageGroup: String, path: String, fileNamesOnly: Boolean) =>
      val elems = List(command, hostName, storageGroup, path, serialize(fileNamesOnly))
      elems mkString BACKEND_SEP
    case _ => throw new IllegalArgumentException
 }

  protected def verifyArgsQuerySetting(args: Seq[Any]): Boolean = args match {
    case Seq(hostName: String, settingName: String) => true
    case _ => false
  }

  protected def serializeQuerySetting(command: String, args: Seq[Any]): String = args match {
    case Seq(hostName: String, settingName: String) =>
      val elems = List(command, hostName, settingName)
      elems mkString " "
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsSetBookmark(args: Seq[Any]): Boolean = args match {
    case Seq(chanId: ChanId, startTime: MythDateTime, position: VideoPosition) => true
    case _ => false
  }

  protected def serializeSetBookmark(command: String, args: Seq[Any]): String = args match {
    case Seq(chanId: ChanId, startTime: MythDateTime, position: VideoPosition) =>
      val elems = List(command, serialize(chanId), serialize(startTime), serialize(position))
      elems mkString " "
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsSetSetting(args: Seq[Any]): Boolean = args match {
    case Seq(hostName: String, settingName: String, settingValue: String) => true
    case _ => false
  }

  protected def serializeSetSetting(command: String, args: Seq[Any]): String = args match {
    case Seq(hostName: String, settingName: String, settingValue: String) =>
      val elems = List(command, hostName, settingName, settingValue)
      elems mkString " "
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsShutdownNow(args: Seq[Any]): Boolean = args match {
    case Seq(haltCommand: String) => true
    case Seq() => true
    case _ => false
  }

  protected def serializeShutdownNow(command: String, args: Seq[Any]): String = args match {
    case Seq(haltCommand: String) =>
      val elems = List(command, haltCommand)
      elems mkString " "
    case Seq() => command
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsUndeleteRecording(args: Seq[Any]): Boolean = args match {
    case Seq(x: Recording) => true
    case Seq(chanId: ChanId, startTime: MythDateTime) => true
    case _ => false
  }

  protected def serializeUndeleteRecording(command: String, args: Seq[Any]): String = args match {
    case Seq(rec: Recording) => serializeProgramInfo(command, args) // TODO this will pattern match again
    case Seq(chanId: ChanId, startTime: MythDateTime) =>
      val start: MythDateTimeString = startTime
      val elems = List(command, serialize(chanId), serialize(start))
      elems mkString BACKEND_SEP
    case _ => throw new IllegalArgumentException

  }

  /**
    * Myth protocol commands: (from programs/mythbackend/mainserver.cpp)
    */
  private val internalMap = Map[String, (CheckArgs, Serialize, HandleResponse)](
    /*
     * ALLOW_SHUTDOWN
     *  @responds sometime; only if tokenCount == 1
     *  @returns "OK"
     */
    "ALLOW_SHUTDOWN" -> (verifyArgsEmpty, serializeEmpty, handleAllowShutdown),

    /*
     * ANN Monitor %s %d                <clientHostName> <eventsMode>
     * ANN Playback %s %d               <clientHostName> <eventsMode>
     * ANN MediaServer %s               <IPAddress>
     * ANN SlaveBackend %s %s { %p }*   <IPAddress> <slaveIPAddr?> [<ProgramInfo>]*
     * ANN FileTransfer %s { %d { %d { %d }}} [%s %s {, %s}*]
     *                    <clientHostName> { writeMode {, useReadAhead {, timeoutMS }}}
     *                    [ url, wantgroup, checkfile {, ...} ]
     *  @responds
     *  @returns
     *      Monitor:       "OK"
     *      Playback:      "OK"
     *      MediaServer:   "OK"
     *      SlaveBackend:  "OK"
     *      FileTransfer: ["OK", %d, %ld]        <ftID>, <fileSize>
     *    or ["ERROR", ... ] on error conditions
     */
    "ANN" -> (verifyArgsAnnounce, serializeAnnounce, handleNOP),

    /*
     * BACKEND_MESSAGE [] [%s {, %s}* ]   [<message> <extra...>]
     *  @responds never
     *  @returns nothing
     */
    "BACKEND_MESSAGE" -> (verifyArgsNOP, serializeNOP, handleNOP),

    /*
     * BLOCK_SHUTDOWN
     *  responds sometimes; only if tokenCount == 1
     *  @returns "OK"
     */
    "BLOCK_SHUTDOWN" -> (verifyArgsEmpty, serializeEmpty, handleBlockShutdown),

    /*
     * CHECK_RECORDING [] [%p]     [<ProgramInfo>]
     *  @responds always
     *  @returns boolean 0/1 as to whether the recording is currently taking place
     */
    "CHECK_RECORDING" -> (verifyArgsProgramInfo, serializeProgramInfo, handleCheckRecording),

    /*
     * DELETE_FILE [] [%s, %s]   [<filename> <storage group name>]
     *  @responds sometime; only if slistCount >= 3
     *  @returns Boolean "0" on error, "1" on succesful file deletion
     */
    "DELETE_FILE" -> (verifyArgsDeleteFile, serializeDeleteFile, handleDeleteFile),

    /*
     * DELETE_RECORDING %d %mt { FORCE { FORGET }}  <ChanId> <starttime> { can we specify NOFORCE or NOFORGET? }
     * DELETE_RECORDING [] [%p]   [<ProgramInfo>]
     * NB starttime is in myth/ISO string format rather than timestamp
     *  @responds sometimes; only if ChanId in program info
     *  @returns Int result code:
     *     0 Successful (expiration only?)
     *    -1 Unspecified error? Or deletion pending in background?
     *    -2 Error deleting file
     *  TODO needs more investigation
     */
    "DELETE_RECORDING" -> (verifyArgsDeleteRecording, serializeDeleteRecording, handleDeleteRecording),

    /*
     * DONE
     *  @responds never
     *  @returns nothing, closes the client's socket
     */
    "DONE" -> (verifyArgsEmpty, serializeEmpty, handleNOP),

    /*
     * DOWNLOAD_FILE [] [%s, %s, %s]       [<srcURL> <storageGroup> <fileName>]
     *  @responds sometimes; only if slistCount == 4
     *  @returns result token:
     *       downloadfile_directory_not_found
     *       downloadfile_filename_dangerous
     *       OK <storagegroup> <filename>      ??
     *       ERROR                             ?? only if synchronous?
     */
    "DOWNLOAD_FILE" -> (verifyArgsDownloadFile, serializeDownloadFile, handleNOP),

    /*
     * DOWNLOAD_FILE_NOW [] [%s, %s, %s]   [<srcURL> <storageGroup> <fileName>]
     *   (this command sets synchronous = true as opposed to DOWNLOAD_FILE)
     *  @responds sometimes; only if slistCount == 4
     *  @returns see DOWNLOAD_FILE
     */
    "DOWNLOAD_FILE_NOW" -> (verifyArgsDownloadFile, serializeDownloadFile, handleNOP),

    /*
     * FILL_PROGRAM_INFO [] [%s, %p]     [<playback host> <ProgramInfo>]
     *  @responds always
     *  @returns ProgramInfo structure, populated
     *           (if already contained pathname, otherwise unchanged)
     */
    "FILL_PROGRAM_INFO" -> (verifyArgsNOP, serializeNOP, handleNOP),

    /*
     * FORCE_DELETE_RECORDING [] [%p]   [<ProgramInfo>]
     *  @responds sometimes; only if ChanId in program info
     *  @returns see DELETE_RECORDING
     */
    "FORCE_DELETE_RECORDING" -> (verifyArgsProgramInfo, serializeProgramInfo, handleForceDeleteRecording),

    /*
     * FORGET_RECORDING [] [%p]    [<ProgramInfo>]
     *  @responds always
     *  @returns "0"
     */
    "FORGET_RECORDING" -> (verifyArgsProgramInfo, serializeProgramInfo, handleForgetRecording),

    /*
     * FREE_TUNER %d        <cardId>
     *  @responds sometimes; only if tokens == 2
     *  @returns "OK" or "FAILED"
     */
    "FREE_TUNER" -> (verifyArgsFreeTuner, serializeFreeTuner, handleNOP),

    /*
     * GET_FREE_RECORDER
     *  @responds always
     *  @returns [%d, %s, %d] = <best free encoder id> <host or IP> <port>
     *        or [-1, "nohost", -1] if no suitable encoder found
     */
    "GET_FREE_RECORDER" -> (verifyArgsEmpty, serializeEmpty, handleGetFreeRecorder),

    /*
     * GET_FREE_RECORDER_COUNT
     *  @responds always
     *  @returns Int: number of available encoders
     */
    "GET_FREE_RECORDER_COUNT" -> (verifyArgsEmpty, serializeEmpty, handleGetFreeRecorderCount),

    /*
     * GET_FREE_RECORDER_LIST
     *  @responds always
     *  @returns [%d, {, %d}] = list of available encoder ids, or "0" if none
     */
    "GET_FREE_RECORDER_LIST" -> (verifyArgsEmpty, serializeEmpty, handleGetFreeRecorderList),

    /*
     * GET_NEXT_FREE_RECORDER [] [%d]  [<currentRecorder#>]
     *  @responds always
     *  @returns [%d, %s, %d] = <next free encoder id> <host or IP> <port>
     *        or [-1, "nohost", -1] if no suitable encoder found
     */
    "GET_NEXT_FREE_RECORDER" -> (verifyArgsCaptureCard, serializeCaptureCard, handleGetNextFreeRecorder),

    /*
     * GET_RECORDER_FROM_NUM [] [%d]   [<recorder#>]
     *  @responds always
     *  @returns [%s, %d] = <host or IP> <port>
     *        or ["nohost", -1] if no matching recorder found
     */
    "GET_RECORDER_FROM_NUM" -> (verifyArgsCaptureCard, serializeCaptureCard, handleNOP),

    /*
     * GET_RECORDER_NUM [] [%p]        [<ProgramInfo>]
     *  @responds always
     *  @returns [%d, %s, %d] =   <encoder#> <host or IP> <port>
     *        or [-1, "nohost", -1] if no matching recorder found
     */
    "GET_RECORDER_NUM" -> (verifyArgsProgramInfo, serializeProgramInfo, handleGetRecorderNum),

    /*
     * GO_TO_SLEEP
     *  @responds always
     *  @returns "OK" or "ERROR: SleepCommand is empty"
     * (only for slaves, but no checking?! Looks @ CoreContext "SleepCommand" setting)
     */
    "GO_TO_SLEEP" -> (verifyArgsEmpty, serializeEmpty, handleGoToSleep),

    /*
     * LOCK_TUNER  (implicitly passes -1 as tuner id, what does this accomplish? first available local tuner?)
     * LOCK_TUNER %d  <cardId>
     *  @responds sometimes; only if tokenCount in { 1, 2 }
     *  @returns [%d, %s, %s, %s]  <cardid> <videodevice> <audiodevice> <vbidevice> (from capturecard table)
     *       or  [-2, "", "", ""]  if tuner is already locked
     *       or  [-1, "", "", ""]  if no tuner found to lock
     */
    "LOCK_TUNER" -> (verifyArgsLockTuner, serializeLockTuner, handleNOP),

    /*
     * MESSAGE [] [ %s {, %s }* ]        [<message> <extra...>]
     * MESSAGE [] [ SET_VERBOSE %s ]     [<verboseMask>]
     * MESSAGE [] [ SET_LOG_LEVEL %s ]   [<logLevel>]
     *  @responds sometimes; if SET_xxx then always, otherwise if slistCount >= 2
     *  @returns          "OK"
     *     SET_VERBOSE:   "OK" or "Failed"
     *     SET_LOG_LEVEL: "OK" or "Failed"
     */
    "MESSAGE" -> (verifyArgsNOP, serializeNOP, handleNOP),

    /*
     * MYTH_PROTO_VERSION %s %s    <version> <protocolToken>
     *  @responds sometimes; only if tokenCount >= 2
     *  @returns ["REJECT, %d"] or ["ACCEPT, %d"] where %d is MYTH_PROTO_VERSION
     */
    "MYTH_PROTO_VERSION" -> (verifyArgsMythProtoVersion, serializeMythProtoVersion, handleMythProtoVersion),

    /*
     * QUERY_ACTIVE_BACKENDS
     *  @responds always
     *  @returns %d [] [ %s {, %s }* ]  <count> [ hostName, ... ]
     */
    "QUERY_ACTIVE_BACKENDS" -> (verifyArgsEmpty, serializeEmpty, handleQueryActiveBackends),

    /*
     * QUERY_BOOKMARK %d %t   <ChanId> <starttime>
     *  @responds sometimes, only if tokenCount == 3
     *  @returns %ld   <bookmarkPos> (frame number)
     */
    "QUERY_BOOKMARK" -> (verifyArgsChanIdStartTime, serializeChanIdStartTime, handleQueryBookmark),

    /*
     * QUERY_CHECKFILE [] [%b, %p]     <checkSlaves> <ProgramInfo>
     *  @responds always
     *  @returns %d %s      <exists:0/1?>  <playbackURL>
     *    note playback url will be "" if file does not exist
     */
    "QUERY_CHECKFILE" -> (verifyArgsQueryCheckFile, serializeQueryCheckFile, handleQueryCheckFile),

    /*
     * QUERY_COMMBREAK %d %t           <ChanId> <starttime>
     *  @responds sometimes; only if tokenCount == 3
     *  @returns %d {[ %d %d ]* }
     *              first integer is count of tuples (-1 if none found?)
     *              tuples are (mark type, mark pos) from recordedmarkup
     *          gather result into a tuple of two lists (start/end of a commbreak)
     *          of course it is possible for one side to be missing? what do we do then?
     * Are the returned positions guaranteed to be in sorted order?
     */
    "QUERY_COMMBREAK" -> (verifyArgsChanIdStartTime, serializeChanIdStartTime, handleQueryCommBreak),

    /*
     * QUERY_CUTLIST %d %t             <ChanId> <starttime>
     *  @responds sometimes; only if tokenCount == 3
     *  @returns see QUERY_COMMBREAK
     */
    "QUERY_CUTLIST" -> (verifyArgsChanIdStartTime, serializeChanIdStartTime, handleQueryCutList),

    /*
     * QUERY_FILE_EXISTS [] [%s {, %s}]   <filename> {<storageGroup>}
     *  @responds sometimes; only if slistCount >= 2
     *  @returns
     *      "0"   file not found or other error
     *      "1" + if sucessful
     *        [%s { %d * 13 }]  <fullname> + stat result fields:
     *             <st_dev> <st_ino> <st_mode> <st_nlink> <st_uid> <st_gid>
     *             <st_rdev> <st_size> <st_blksize> <st_blocks>
     *             <st_atime> <st_mtime> <st_ctime>
     *
     * If storage group name is not specified, then "Default" will be used as the default.
     */
    "QUERY_FILE_EXISTS" -> (verifyArgsQueryFileExists, serializeQueryFileExists, handleQueryFileExists),

    /*
     * QUERY_FILE_HASH [] [%s, %s {, %s}]     <filename> <storageGroup> {<hostname>}
     *  @responds sometimes; only if slistCount >= 3
     *  @returns
     *      ""  on error checking for file, invalid input  ----> TODO cannot reproduce this
     *      %s  hash of the file (currently 64-bit, so 16 hex characters)
     *     "NULL" if file was zero-length or did not exist (any other conditions? TODO)
     * NB storageGroup parameter seems to be a hint at most, specifying a non-existing or
     *    incorrect storageGroup does not prevent the proper hash being returned
     */
    "QUERY_FILE_HASH" -> (verifyArgsQueryFileHash, serializeQueryFileHash, handleQueryFileHash),

    /*
     * QUERY_FILETRANSFER %d [DONE]                 <ftID>
     * QUERY_FILETRANSFER %d [REQUEST_BLOCK, %d]    <ftID> [ blockSize ]
     * QUERY_FILETRANSFER %d [WRITE_BLOCK, %d]      <ftID> [ blockSize ]
     * QUERY_FILETRANSFER %d [SEEK, %d, %d, %d]     <ftID> [ pos, whence, curPos ]
     * QUERY_FILETRANSFER %d [IS_OPEN]              <ftID>
     * QUERY_FILETRANSFER %d [REOPEN %s]            <ftID> [ newFilename ]
     * QUERY_FILETRANSFER %d [SET_TIMEOUT %b]       <ftID> [ fast ]
     * QUERY_FILETRANSFER %d [REQUEST_SIZE]         <ftID>
     *  @responds TODO
     *  @returns TODO
     */
    "QUERY_FILETRANSFER" -> (verifyArgsNOP, serializeNOP, handleNOP),

    /*
     * QUERY_FREE_SPACE
     *  @responds always
     *  @returns  TODO
     */
    "QUERY_FREE_SPACE" -> (verifyArgsEmpty, serializeEmpty, handleQueryFreeSpace),

    /*
     * QUERY_FREE_SPACE_LIST
     *  @responds always
     *  @returns TODO
     *
     * Like QUERY_FREE_SPACE but returns free space on all hosts, each directory
     * is reported as a URL, and a TotalDiskSpace is appended.
     */
    "QUERY_FREE_SPACE_LIST" -> (verifyArgsEmpty, serializeEmpty, handleQueryFreeSpaceList),

    /*
     * QUERY_FREE_SPACE_SUMMARY
     *  @responds always
     *  @returns [%d, %d]    <total size> <used size>  sizes are in kB (1024-byte blocks)
     *        or [ 0, 0 ]    if there was any sort of error
     */
    "QUERY_FREE_SPACE_SUMMARY" -> (verifyArgsEmpty, serializeEmpty, handleQueryFreeSpaceSummary),

    /*
     * QUERY_GENPIXMAP2 [] [%s, %p, more?]     TODO %s is a "token", can be the literal "do_not_care"
     *  @responds always?
     *  @returns ["OK", %s]    <filename>
     *       or ?? TODO follow up on successful return indication/other errors from slave pixmap generation
     *       or ["ERROR", "TOO_FEW_PARAMS"]
     *       or ["ERROR", "TOKEN_ABSENT"]
     *       or ["BAD", "NO_PATHNAME"]
     *       or ["ERROR", "FILE_INACCESSIBLE"]
     * Does this follow up later with a message when the pixmap generation is complete?
     */
    "QUERY_GENPIXMAP2" -> (verifyArgsNOP, serializeNOP, handleNOP),

    /*
     * QUERY_GETALLPENDING { %s {, %d}}  { <tmptable> {, <recordid>}}
     *  @responds always
     *  @returns %d %b [%p {, %p}]  <expectedCount> <hasConflicts> <list of ProgramInfo>
     *        or ["0", "0"] if not availble/error?
     *  TODO what is the purpose of the optional tmptable and recordid parameters?
     */
    "QUERY_GETALLPENDING" -> (verifyArgsQueryGetAllPending, serializeQueryGetAllPending, handleQueryGetAllPending),

    /*
     * QUERY_GETALLSCHEDULED
     *  @responds always
     *  @returns %d [%p {, %p}] <expectedCount> <list of ProgramInfo>
     *        or "0" if not availble/error?
     */
    "QUERY_GETALLSCHEDULED" -> (verifyArgsEmpty, serializeEmpty, handleQueryGetAllScheduled),

    /*
     * QUERY_GETCONFLICTING [] [%p]     [<ProgramInfo>]
     *  @responds always
     *  @returns ? [%p {, %p}]  <list of ProgramInfo>  // TODO does this begin with a count?
     *        or "0" if not availble/error?
     */
    "QUERY_GETCONFLICTING" -> (verifyArgsProgramInfo, serializeProgramInfo, handleNOP),

    /*
     * QUERY_GETEXPIRING
     *  @responds always
     *  @returns ? [%p {, %p}]  <list of ProgramInfo>  // TODO does this begin with a count?
     *        or "0" if not availble/error?
     */
    "QUERY_GETEXPIRING" -> (verifyArgsEmpty, serializeEmpty, handleQueryGetExpiring),

    /*
     * QUERY_GUIDEDATATHROUGH
     *  @responds always
     *  @returns: Date/Time as a string in "YYYY-MM-DD hh:mm" format
     *         or "0000-00-00 00:00" in case of error or no data
     */
    "QUERY_GUIDEDATATHROUGH" -> (verifyArgsEmpty, serializeEmpty, handleQueryGuideDataThrough),

    /*
     * QUERY_HOSTNAME
     *  @responds always
     *  @returns %s  <hostname>
     */
    "QUERY_HOSTNAME" -> (verifyArgsEmpty, serializeEmpty, handleQueryHostname),

    /*
     * QUERY_IS_ACTIVE_BACKEND [] [%s]   [<hostname>]
     *  @responds sometimes; only if tokenCount == 1
     *  @returns "TRUE" or "FALSE"
     * TODO may case NPE if hostname is not passed?
     *      what does QtStringList array index out of bounds do?
     */
    "QUERY_IS_ACTIVE_BACKEND" -> (verifyArgsQueryIsActiveBackend, serializeQueryIsActiveBackend, handleQueryIsActiveBackend),

    /*
     * QUERY_ISRECORDING
     *  @responds always
     *  @returns [%d, %d]  <numRecordingsInProgress> <numLiveTVinProgress>
     *                           (liveTV is a subset of recordings)
     */
    "QUERY_ISRECORDING" -> (verifyArgsEmpty, serializeEmpty, handleQueryIsRecording),

    /*
     * QUERY_LOAD
     *  @responds always
     *  @returns [%f, %f, %f]   1-min  5-min  15-min load averages
     *        or ["ERROR", "getloadavg() failed"] in case of error
     */
    "QUERY_LOAD" -> (verifyArgsEmpty, serializeEmpty, handleQueryLoad),

    /*
     * QUERY_MEMSTATS
     *  @responds always
     *  @returns [%d, %d, %d, %d]  <totalMB> <freeMB> <totalVM> <freeVM>
     *        or ["ERROR", "Could not determine memory stats."] on error
     */
    "QUERY_MEMSTATS" -> (verifyArgsEmpty, serializeEmpty, handleQueryMemStats),

    /*
     * QUERY_PIXMAP_GET_IF_MODIFIED [] [%t, %d, %p]  [<modifiedSince> <maxFileSize> <ProgramInfo>]
     *    <cachemodified> can be -1 to ignore, otherwise it is a timestamp
     *  @responds always?
     *  @returns [ %t, %ld, %d, %PIX ]  <lastModifiedTime> <imageFileSize> <crc16checksum> <Base64 encoded image file data>
     *  NB Is lastModifiedTime really the image file or maybe the recording file, or ???
     *        or ["ERROR", "1: Parameter list too short"]
     *        or ["ERROR", "2: Invalid ProgramInfo"]
     *        or ["ERROR", "3: Failed to read preview file..."]
     *        or ["ERROR", "4: Preview file is invalid"]
     *        or ["ERROR", "5: Could not locate mythbackend that made this recording"
     *        or ["WARNING", "2: Could not locate requested file"]
     */
    "QUERY_PIXMAP_GET_IF_MODIFIED" -> (verifyArgsQueryPixmapGetIfModified, serializeQueryPixmapGetIfModified, handleQueryPixmapGetIfModified),

    /*
     * QUERY_PIXMAP_LASTMODIFIED [] [%p]      [<ProgramInfo>]
     *  @responds
     *  @returns %ld    <last modified (timestamp)>
     *        or "BAD"
     */
    "QUERY_PIXMAP_LASTMODIFIED" -> (verifyArgsProgramInfo, serializeProgramInfo, handleQueryPixmapLastModified),

    /*
     * QUERY_RECORDER %d  <recorder#> [    // NB two tokens! recorder# + subcommand list
     *     IS_RECORDING
     *   | GET_FRAMERATE
     *   | GET_FRAMES_WRITTEN
     *   | GET_FILE_POSITION
     *   | GET_MAX_BITRATE
     *   | GET_KEYFRAME_POS [%ld]          [<desiredFrame>]
     *   | FILL_POSITION_MAP [%ld, %ld]    [<start> <end>]
     *   | FILL_DURATION_MAP [%ld, %ld]    [<start> <end>]
     *   | GET_CURRENT_RECORDING
     *   | GET_RECORDING
     *   | FRONTEND_READY
     *   | CANCEL_NEXT_RECORDING [%b]      [<cancel>]
     *   | SPAWN_LIVETV [%s, %d, %s]       [<chainid> ? ? ]
     *   | STOP_LIVETV
     *   | PAUSE
     *   | FINISH_RECORDING
     *   | SET_LIVE_RECORDING [%d]         [<recording>]  what is this param?
     *   | GET_FREE_INPUTS [{%d {, %d}*}*] [{<excludeCardId...>}]
     *   | GET_INPUT
     *   | SET_INPUT [%s]                  [<input>]
     *   | TOGGLE_CHANNEL_FAVORITE [%s]    [<chanGroup>]
     *   | CHANGE_CHANNEL [%d]             [<channelChangeDirection>]
     *   | SET_CHANNEL [%s]                [<channelName>]
     *   | SET_SIGNAL_MONITORING_RATE [%d, %b]  [<rate>, <notifyFrontEnd>>]
     *   | GET_COLOUR
     *   | GET_CONTRAST
     *   | GET_BRIGHTNESS
     *   | GET_HUE
     *   | CHANGE_COLOUR [%d, %b]          [<type> <up>]
     *   | CHANGE_CONTRAST [%d, %b]        [<type> <up>]
     *   | CHANGE_BRIGHTNESS [%d, %b]      [<type> <up>]
     *   | CHANGE_HUE [%d, %d]             [<type> <up>]
     *   | CHECK_CHANNEL [%s]              [<channelName>]
     *   | SHOULD_SWITCH_CARD [%d]         [<ChanId>]
     *   | CHECK_CHANNEL_PREFIX [%s]       [<prefix>]
     *   | GET_NEXT_PROGRAM_INFO [%s, %d, %d, %s]   [<channelName> <ChanId> <BrowseDirection> <starttime>]
     *   | GET_CHANNEL_INFO [%d]           [<ChanId>]
     * ]
     *  TODO what format is starttime in GET_NEXT_PROGRAM_INFO? Gets passed to database as a string, so any valid fmt?
     */
    "QUERY_RECORDER" -> (verifyArgsNOP, serializeNOP, handleNOP),

    /*
     * QUERY_RECORDING BASENAME %s                 <pathname>
     * QUERY_RECORDING TIMESLOT %d %mt             <ChanId> <starttime>
     *  NB starttime is in myth/ISO string format rather than in timestamp
     *  @responds sometimes; only if tokenCount >= 3 (or >= 4 if TIMESLOT is specified)
     *  @returns ["OK", <ProgramInfo>] or "ERROR"
     */
    "QUERY_RECORDING" -> (verifyArgsQueryRecording, serializeQueryRecording, handleQueryRecording),

    /*
     * QUERY_RECORDING_DEVICE
     *   not implemented on backend server
     */
//    "QUERY_RECORDING_DEVICE" -> (verifyArgsNOP, serializeNOP, handleNOP),

    /*
     * QUERY_RECORDING_DEVICES
     *   not implemented on backend server
     */
//    "QUERY_RECORDING_DEVICES" -> (verifyArgsNOP, serializeNOP, handleNOP),

    /*
     * QUERY_RECORDINGS { Ascending | Descending | Unsorted | Recording }
     *  @responds sometimes; only if tokenCount == 2
     *  @returns [ %d, %p {, %p}*]   <expectedCount> list of ProgramInfo records
     */
    "QUERY_RECORDINGS" -> (verifyArgsQueryRecordings, serializeQueryRecordings, handleQueryRecordings),

    /*
     * QUERY_REMOTEENCODER %d [          <encoder#>
     *     GET_STATE
     *   | GET_SLEEPSTATUS
     *   | GET_FLAGS
     *   | IS_BUSY [%d]                  <timeBuffer>
     *   | MATCHES_RECORDING [%p]        <ProgramInfo>
     *   | START_RECORDING [%p]          <ProgramInfo>
     *   | GET_RECORDING_STATUS
     *   | RECORD_PENDING [%d, %d, %p]   <secsLeft> <hasLater> <ProgramInfo>
     *   | CANCEL_NEXT_RECORDING [%b]    <cancel>
     *   | STOP_RECORDING
     *   | GET_MAX_BITRATE
     *   | GET_CURRENT_RECORDING
     *   | GET_FREE_INPUTS [%d {, %d}*]  <excludeCardId...>
     * ]
     */
    "QUERY_REMOTEENCODER" -> (verifyArgsNOP, serializeNOP, handleNOP),

    /*
     * QUERY_SETTING %s %s      <hostname> <settingName>
     *  @responds sometimes; only if tokenCount == 3
     *  @returns %s or "-1" if not found   <settingValue>
     * NB doesn't seem possible to retrieve settings with "global" scope, i.e. hostname IS NULL
     */
    "QUERY_SETTING" -> (verifyArgsQuerySetting, serializeQuerySetting, handleQuerySetting),

    /*
     * QUERY_SG_GETFILELIST [] [%s, %s, %s {, %b}]  <hostName> <storageGroup> <path> { fileNamesOnly> }
     *  @responds always
     *  @returns  list of filenames or list of storage group URLS (?)
     *        or ["EMPTY LIST"]               if wrong number of parameters given or no results
     *        or ["SLAVE UNREACHABLE: ", %s]  if slave specified and unreachable
     * NB: if a non-existent storage group name is specified, it will be replaced with "Default" by
     *     the server and the corresponding results returned
     *  TODO parse formats... they may have file:: or sgdir:: or sgdir:: or such prefixed, or nothing...
     *  TODO are path and fileNamesOnly sort of mutually exclusive?
     */
    "QUERY_SG_GETFILELIST" -> (verifyArgsQuerySGGetFileList, serializeQuerySGGetFileList, handleQuerySGGetFileList),

    /*
     * QUERY_SG_FILEQUERY [] [%s, %s, %s]     <hostName> <storageGroup> <fileName>
     *  @responds always
     *  @returns [%s %t %ld]                  <fullFilePath> <fileTimestamp> <fileSize>
     *        or ["EMPTY LIST"]               if wrong number of parameters given or no file found
     *        or ["SLAVE UNREACHABLE: ", %s]  if slave specified and unreachable
     */
    "QUERY_SG_FILEQUERY" -> (verifyArgsQuerySGFileQuery, serializeQuerySGFileQuery, handleQuerySGFileQuery),

    /*
     * QUERY_TIME_ZONE
     *  @responds always
     *  @returns [%s, %d, %s]  <timezoneName> <offsetSecsFromUtc> <currentTimeUTC>
     *    currentTimeUTC is in the ISO format "YYYY-MM-ddThh:mm:ssZ"
     */
    "QUERY_TIME_ZONE" -> (verifyArgsEmpty, serializeEmpty, handleQueryTimeZone),

    /*
     * QUERY_UPTIME
     *  @responds always
     *  @returns %ld  <uptimeSeconds>
     *        or ["ERROR", "Could not determine uptime."] in case of error
     */
    "QUERY_UPTIME" -> (verifyArgsEmpty, serializeEmpty, handleQueryUptime),

    /*
     * REFRESH_BACKEND
     *  @responds always
     *  @returns "OK"
     *  Seems to be a NOP on the server.
     */
    "REFRESH_BACKEND" -> (verifyArgsEmpty, serializeEmpty, handleRefreshBackend),

    /*
     * RESCHEDULE_RECORDINGS [] [CHECK %d %d %d {Python}, '', '', '', {**any**}]
     * RESCHEDULE_RECORDINGS [] [MATCH %d %d %d {- Python}]
     *   TODO look @ Scheduler::HandleReschedule in programs/mythbackend/scheduler.cpp
     */
    "RESCHEDULE_RECORDINGS" -> (verifyArgsNOP, serializeNOP, handleNOP),

    /*
     * SCAN_VIDEOS
     *  @responds always
     *  @returns "OK" or "ERROR"
     */
    "SCAN_VIDEOS" -> (verifyArgsEmpty, serializeEmpty, handleScanVideos),

    /*
     * SET_BOOKMARK %d %t %ld          <ChanId> <starttime> <frame#position>
     *  @responds sometimes; only if tokenCount == 4
     *  @returns "OK" or "FAILED"
     * NB If the given position is '0' then any existing bookmark will be deleted.
     */
    "SET_BOOKMARK" -> (verifyArgsSetBookmark, serializeSetBookmark, handleSetBookmark),

    /*
     * SET_CHANNEL_INFO [] [%d, %d, %d, %d, %d, %d, %d]
     *                     <ChanId> <sourceid> <oldcnum> <callsign> <channum> <channame> <xmltv>
     *  @responds always
     *  @returns "1" for successful otherwise "0"
     */
    "SET_CHANNEL_INFO" -> (verifyArgsNOP, serializeNOP, handleNOP),

    /*
     * SET_NEXT_LIVETV_DIR %d %s  <encoder#> <dir>
     *  @responds sometimes; only if tokenCount == 3
     *  @returns "OK or "bad" if encoder nor found
     */
    "SET_NEXT_LIVETV_DIR" -> (verifyArgsNOP, serializeNOP, handleNOP),

    /*
     * SET_SETTING %s %s %s       <hostname> <settingname> <value>
     *  @responds sometimes; only if tokenCount == 4
     *  @returns "OK" or "ERROR"
     */
    "SET_SETTING" -> (verifyArgsSetSetting, serializeSetSetting, handleSetSetting),

    /*
     * SHUTDOWN_NOW { %s }        { <haltCommand> }
     *  @responds never
     *  @returns nothing
     */
    "SHUTDOWN_NOW" -> (verifyArgsShutdownNow, serializeShutdownNow, handleNOP),

    /*
     * STOP_RECORDING [] [<ProgramInfo>]
     *  @responds sometimes; only if recording is found
     *  @returns "0" if recording was on a slave backend
     *           "%d" if recording was on a local encoder, <recnum>
     *        or "-1" if not found
     */
    "STOP_RECORDING" -> (verifyArgsProgramInfo, serializeProgramInfo, handleStopRecording),

    /*
     * UNDELETE_RECORDING [] [%d, %mt]       [<ChanId> <starttime>]
     * UNDELETE_RECORDING [] [%p]            [<ProgramInfo>]
     * NB starttime is in myth/ISO string format rather than timestamp
     *  @responds sometimes; if program info has ChanId
     *  @returns "0" on success and "-1" on error
     */
    "UNDELETE_RECORDING" -> (verifyArgsUndeleteRecording, serializeUndeleteRecording, handleUndeleteRecording)
  )


  /**
   * Data types that need serialization to send over the wire:
    *  Program      --> <ProgramInfo>
    *  MythDateTime --> epoch timestamp (or mythformat string?)
    *  String       --> string
    *  ChanId       --> integer string
    *  Int          --> integer string
    *  Long         --> long integer string (up to 64-bits worth)
    *  Boolean      --> is there a consistent serialization format? 0/1 --> , <-- TRUE/FALSE or 1/0 ??
    *  LogLevel / VerboseMask --> what is the format of these strings?
    *  ChannelChanngeDirection --> integer string (from enum?)
    *  BrowseDirection --> integer string from enum?
    *  File STAT structure ... deserialize, c.f. QUERY_FILE_EXISTS
    *  FileHash -> String
    */

  /**
    * Outline for sending protocol commands:
    *
    *  i) lookup command name in table to verify it is a valid and supported command
    *  ii) parse any arguments and attempt to match against valid signatures for the command
    *  iii) serialize the argument and the command in the proper format, should result in a string
    *  iv) send the serialized command over the wire
    *  v) process the result according to rules for the given command signature
    */

  protected def handleNOP(response: BackendResponse): Option[Nothing] = None

  protected def handleAllowShutdown(response: BackendResponse): Option[Boolean] = {
    Some(response.raw == "OK")
  }

  protected def handleBlockShutdown(response: BackendResponse): Option[Boolean] = {
    Some(response.raw == "OK")
  }

  protected def handleCheckRecording(response: BackendResponse): Option[Boolean] = {
    Some(deserialize[Boolean](response.raw))
  }

  protected def handleDeleteFile(response: BackendResponse): Option[Boolean] = {
    Some(deserialize[Boolean](response.raw))
  }

  protected def handleDeleteRecording(response: BackendResponse): Option[Int] = {
    Some(deserialize[Int](response.raw))
  }

  protected def handleForceDeleteRecording(response: BackendResponse): Option[Int] = {
    Some(deserialize[Int](response.raw))
  }

  protected def handleForgetRecording(response: BackendResponse): Option[Int] = {
    Some(deserialize[Int](response.raw))
  }

  protected def handleGetFreeRecorder(response: BackendResponse): Option[RemoteEncoder] = {
    import data.BackendRemoteEncoder  // TODO eliminate import here
    val items = response.split
    val cardId = deserialize[CaptureCardId](items(0))
    val host = items(1)
    val port = deserialize[Int](items(2))
    Some(BackendRemoteEncoder(cardId, host, port))
  }

  protected def handleGetFreeRecorderCount(response: BackendResponse): Option[Int] = {
    Some(deserialize[Int](response.raw))
  }

  protected def handleGetFreeRecorderList(response: BackendResponse): Option[List[CaptureCardId]] = {
    val cards = response.split map deserialize[CaptureCardId]
    Some(cards.toList)
  }

  protected def handleGetNextFreeRecorder(response: BackendResponse): Option[RemoteEncoder] = {
    import data.BackendRemoteEncoder  // TODO eliminate import here
    val items = response.split
    val cardId = deserialize[CaptureCardId](items(0))
    val host = items(1)
    val port = deserialize[Int](items(2))
    Some(BackendRemoteEncoder(cardId, host, port))
  }

  protected def handleGetRecorderNum(response: BackendResponse): Option[RemoteEncoder] = {
    import data.BackendRemoteEncoder  // TODO eliminate import here
    val items = response.split
    val cardId = deserialize[CaptureCardId](items(0))
    val host = items(1)
    val port = deserialize[Int](items(2))
    Some(BackendRemoteEncoder(cardId, host, port))
  }

  protected def handleGoToSleep(response: BackendResponse): Option[Boolean] = {
    Some(response.raw == "OK")
  }

  protected def handleMythProtoVersion(response: BackendResponse): Option[(Boolean, Int)] = {
    val parts = response.split
    assert(parts.length > 1)
    val accepted = parts(0) == "ACCEPT"
    val acceptedVersion = deserialize[Int](parts(1))
    Some(accepted, acceptedVersion)
  }

  protected def handleQueryActiveBackends(response: BackendResponse): Option[List[String]] = {
    val recs = response.split
    val expectedCount = deserialize[Int](recs(0))  // TODO check non-zero
    Some((recs.iterator drop 1).toList)
  }

  protected def handleQueryBookmark(response: BackendResponse): Option[VideoPosition] = {
    val pos = deserialize[Long](response.raw)
    Some(VideoPosition(pos))
  }

  protected def handleQueryCheckFile(response: BackendResponse): Option[String] = {
    val items = response.split
    val exists = deserialize[Boolean](items(0))
    if (exists) Some(items(1))
    else None
  }

  protected def handleQueryCommBreak(response: BackendResponse): Option[List[VideoSegment]] = {
    import data.BackendVideoSegment  // TODO eliminate import here
    val items = response.split
    val count = deserialize[Int](items(0))
    assert(count % 2 == 0)  // TODO FIXME not guaranteed to be true!?

    // we also assume that the number of start/end marks are balanced and in sorted order
    val marks = items.iterator drop 1 grouped 2 withPartial false map deserialize[RecordedMarkup]
    val segments = marks grouped 2 map {
      case Seq(start: RecordedMarkup, end: RecordedMarkup) =>
        assert(start.tag == Markup.MARK_COMM_START)
        assert(end.tag == Markup.MARK_COMM_END)
        BackendVideoSegment(start.position, end.position)
    }
    Some(segments.toList)
  }

  protected def handleQueryCutList(response: BackendResponse): Option[List[VideoSegment]] = {
    import data.BackendVideoSegment  // TODO eliminate import here
    val items = response.split
    val count = deserialize[Int](items(0))
    assert(count % 2 == 0)  // TODO FIXME not guaranteed to be true!?

    // we also assume that the number of start/end marks are balanced and in sorted order
    val marks = items.iterator drop 1 grouped 2 withPartial false map deserialize[RecordedMarkup]
    val segments = marks grouped 2 map {
      case Seq(start: RecordedMarkup, end: RecordedMarkup) =>
        assert(start.tag == Markup.MARK_CUT_START)
        assert(end.tag == Markup.MARK_CUT_END)
        BackendVideoSegment(start.position, end.position)
    }
    Some(segments.toList)
  }

  protected def handleQueryFileExists(response: BackendResponse): Option[(String, FileStats)] = {
    val items = response.split
    val statusCode = deserialize[Int](items(0))
    assert(statusCode > 0)
    val fullName = items(1)
    val stats = deserialize[FileStats](items.view(2, 2 + 13))  // TODO hardcoded size of # file stats fields
    Some((fullName, stats))
  }

  // TODO more specific return type to contain hash value
  protected def handleQueryFileHash(response: BackendResponse): Option[String] = {
    Some(response.raw)
  }

  protected def handleQueryFreeSpace(response: BackendResponse): Option[List[FreeSpace]] = {
    import data.BackendFreeSpace  // TODO eliminate import here
    val items = response.split
    val fieldCount = BackendFreeSpace.FIELD_ORDER.length
    val it = items.iterator grouped fieldCount withPartial false map (BackendFreeSpace(_))
    Some(it.toList)
  }

  protected def handleQueryFreeSpaceList(response: BackendResponse): Option[List[FreeSpace]] = {
    import data.BackendFreeSpace  // TODO eliminate import here
    val items = response.split
    val fieldCount = BackendFreeSpace.FIELD_ORDER.length
    val it = items.iterator grouped fieldCount withPartial false map (BackendFreeSpace(_))
    Some(it.toList)
  }

  protected def handleQueryFreeSpaceSummary(response: BackendResponse): Option[(ByteCount, ByteCount)] = {
    val data = response.split map (n => deserialize[Long](n) * 1024)
    assert(data.length > 1)
    Some((DecimalByteCount(data(0)), DecimalByteCount(data(1))))
  }

  protected def handleQueryGetAllPending(response: BackendResponse): Option[ExpectedCountIterator[Recording]] = {
    import data.BackendProgram  // TODO eliminate import here
    val recs = response.split
    val hasConflicts = deserialize[Boolean](recs(0))  // TODO return this also?
    val expectedCount = deserialize[Int](recs(1))  // TODO check non-zero!
    val fieldCount = BackendProgram.FIELD_ORDER.length
    val it = recs.iterator drop 2 grouped fieldCount withPartial false
    implicit val piser = ProgramInfoSerializerCurrent    // TODO shouldn't have to explicit declare this here...
    Some(new ExpectedCountIterator(expectedCount, it map deserialize[Recording]))
  }

  protected def handleQueryGetAllScheduled(response: BackendResponse): Option[ExpectedCountIterator[Recording]] = {
    import data.BackendProgram  // TODO eliminate import here
    val recs = response.split
    val expectedCount = deserialize[Int](recs(0))  // TODO check non-zero!
    val fieldCount = BackendProgram.FIELD_ORDER.length
    val it = recs.iterator drop 1 grouped fieldCount withPartial false
    Some(new ExpectedCountIterator(expectedCount, it map (BackendProgram(_))))
  }

  protected def handleQueryGetExpiring(response: BackendResponse): Option[ExpectedCountIterator[Recording]] = {
    import data.BackendProgram  // TODO eliminate import here
    val recs = response.split
    val expectedCount = deserialize[Int](recs(0))  // TODO check non-zero!
    val fieldCount = BackendProgram.FIELD_ORDER.length
    val it = recs.iterator drop 1 grouped fieldCount withPartial false
    Some(new ExpectedCountIterator(expectedCount, it map (BackendProgram(_))))
  }

  protected def handleQueryGuideDataThrough(response: BackendResponse): Option[MythDateTime] = {
    val result = deserialize[MythDateTime](response.raw)
    Some(result)
  }

  protected def handleQueryHostname(response: BackendResponse): Option[String] = {
    Some(response.raw)
  }

  protected def handleQueryIsActiveBackend(response: BackendResponse): Option[Boolean] = {
    val result = deserialize[Boolean](response.raw)
    Some(result)
  }

  protected def handleQueryIsRecording(response: BackendResponse): Option[(Int, Int)] = {
    val results = response.split map deserialize[Int]
    assert(results.length > 1)
    Some(results(0), results(1))
  }

  protected def handleQueryLoad(response: BackendResponse): Option[(Double, Double, Double)] = {
    val loads = response.split map deserialize[Double]
    assert(loads.length > 2)
    Some((loads(0), loads(1), loads(2)))
  }

  protected def handleQueryMemStats(response: BackendResponse): Option[(ByteCount, ByteCount, ByteCount, ByteCount)] = {
    val stats = response.split map (n => BinaryByteCount(deserialize[Long](n) * 1024 * 1024))
    assert(stats.length > 3)
    Some(stats(0), stats(1), stats(2), stats(3))
  }

  protected def handleQueryPixmapGetIfModified(response: BackendResponse): Option[(MythDateTime, Option[PixmapInfo])] = {
    val items = response.split
    val lastModified = deserialize[MythDateTime](items(0))
    // TODO check items(0) for error

    if (items.length == 1) Some(lastModified, None)
    else {
      val fileSize = deserialize[Long](items(1))
      val crc16 = deserialize[Int](items(2))
      val base64data = items(3)
      Some((lastModified, Some(PixmapInfo(DecimalByteCount(fileSize), crc16, base64data))))
    }
  }

  protected def handleQueryPixmapLastModified(response: BackendResponse): Option[MythDateTime] = {
    val modified = deserialize[MythDateTime](response.raw)
    Some(modified)
  }

  protected def handleQueryRecording(response: BackendResponse): Option[Recording] = {
    val items = response.split
    // TODO check items(0) for error
    implicit val piser = ProgramInfoSerializerCurrent  // TODO FIXME shouldn't need to delclare here...
    Some(deserialize[Recording](items drop 1))
  }

  protected def handleQueryRecordings(response: BackendResponse): Option[Iterator[Recording]] = {
    import data.BackendProgram  // TODO eliminate import here
    val recs = response.split
    val expectedCount = deserialize[Int](recs(0))  // TODO check non-zero!
    val fieldCount = BackendProgram.FIELD_ORDER.length
    val it = recs.iterator drop 1 grouped fieldCount withPartial false
    Some(new ExpectedCountIterator(expectedCount, it map (BackendProgram(_))))
  }

  protected def handleQuerySGFileQuery(response: BackendResponse): Option[(String, MythDateTime, ByteCount)] = {
    val items = response.split
    val fullPath = items(0)
    val timestamp = deserialize[MythDateTime](items(1))
    val fileSize = deserialize[Long](items(2))
    Some((fullPath, timestamp, DecimalByteCount(fileSize)))
  }

  protected def handleQuerySGGetFileList(response: BackendResponse): Option[List[String]] = {
    Some(response.split.toList)
  }

  protected def handleQuerySetting(response: BackendResponse): Option[String] = {
    Some(response.raw)
  }

  protected def handleQueryTimeZone(response: BackendResponse): Option[(String, ZoneOffset, Instant)] = {
    val items = response.split
    assert(items.length > 2)
    val tzName = items(0)
    val offset = ZoneOffset.ofTotalSeconds(deserialize[Int](items(1)))
    val time = deserialize[Instant](items(2))
    Some((tzName, offset, time))
  }

  protected def handleQueryUptime(response: BackendResponse): Option[Duration] = {
    val seconds = deserialize[Long](response.raw)
    Some(Duration.ofSeconds(seconds))
  }

  protected def handleRefreshBackend(response: BackendResponse): Option[Boolean] = {
    val success = response.raw == "OK"
    Some(success)
  }

  protected def handleScanVideos(response: BackendResponse): Option[Boolean] = {
    val success = response.raw == "OK"
    Some(success)
  }

  protected def handleSetBookmark(response: BackendResponse): Option[Boolean] = {
    val success = response.raw == "OK"
    Some(success)
  }

  protected def handleSetSetting(response: BackendResponse): Option[Boolean] = {
    val success = response.raw == "OK"
    Some(success)
  }

  protected def handleStopRecording(response: BackendResponse): Option[Int] = {
    val result = deserialize[Int](response.raw)
    Some(result)
  }

  protected def handleUndeleteRecording(response: BackendResponse): Option[Boolean] = {
    val success = deserialize[Boolean](response.raw)
    Some(success)
  }
}

private[myth] trait MythProtocolLike75 extends MythProtocolLike {
//  final val PROTO_VERSION = 75
//  final val PROTO_TOKEN = "SweetRock"
}

private[myth] trait MythProtocolLike77 extends MythProtocolLike {
//  final val PROTO_VERSION = 77
//  final val PROTO_TOKEN = "WindMark"
}
