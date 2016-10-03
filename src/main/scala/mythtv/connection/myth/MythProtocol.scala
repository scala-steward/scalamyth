package mythtv
package connection
package myth

import java.time.{ Duration, Instant, LocalDate, ZoneOffset }
import java.util.regex.Pattern

import model.{ CaptureCardId, ChanId, FreeSpace, Recording, VideoPosition }
import util.{ ByteCount, ExpectedCountIterator, MythDateTime, MythDateTimeString }

trait MythProtocol {
  // TODO move constants to a companion object?
  import MythProtocol._
  // TODO can we structure this so that it's possible to support more than one protocol version?
  final val PROTO_VERSION = 77        // "75"
  final val PROTO_TOKEN = "WindMark"  // "SweetRock"

  type CheckArgs = (Seq[Any]) => Boolean
  type Serialize = (String, Seq[Any]) => String
  type HandleResponse = (BackendResponse) => Any  // TODO what is result type?

  def commands: Map[String, (CheckArgs, Serialize)] = internalMap

  // TODO move MythProtocolSerializer out of an object and into a trait so we can inherit and
  //      have a simple serialize() method without all the qualified calls to MythProtocolSerializer
  //      NB this all belongs in an implementation (*Like?) trait/class anyway

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
      MythProtocolSerializer.serialize(rec, bldr).toString
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsChanIdStartTime(args: Seq[Any]): Boolean = args match {
    case Seq(chanId: ChanId, startTime: MythDateTime) => true
    case _ => false
  }

  protected def serializeChanIdStartTime(command: String, args: Seq[Any]): String = args match {
    case Seq(chanId: ChanId, startTime: MythDateTime) =>
      val elems = List(
        command,
        MythProtocolSerializer.serialize(chanId),
        MythProtocolSerializer.serialize(startTime)
      )
      elems mkString " "
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsCaptureCard(args: Seq[Any]): Boolean = args match {
    case Seq(cardId: CaptureCardId) => true
    case _ => false
  }

  protected def serializeCaptureCard(command: String, args: Seq[Any]): String = args match {
    case Seq(cardId: CaptureCardId) =>
      val elems = List(command, MythProtocolSerializer.serialize(cardId))
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
      val elems = List(command, mode, clientHostName, MythProtocolSerializer.serialize(eventsMode))
      elems mkString " "
    case Seq(mode @ "Playback", clientHostName: String, eventsMode: Int) =>
      val elems = List(command, mode, clientHostName, MythProtocolSerializer.serialize(eventsMode))
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
      MythProtocolSerializer.serialize(chanId, builder.append(' '))
      MythProtocolSerializer.serialize(time, builder.append(' '))
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
      val elems = List(command, MythProtocolSerializer.serialize(cardId))
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
      val elems = List(command, MythProtocolSerializer.serialize(cardId))
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
      val elems = List(command, MythProtocolSerializer.serialize(version), token)
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
      val elems = List(command, MythProtocolSerializer.serialize(checkSlaves), MythProtocolSerializer.serialize(rec))
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

  protected def verifyArgsQueryRecording(args: Seq[Any]): Boolean = args match {
    case Seq("TIMESLOT", chanId: ChanId, startTime: MythDateTime) => true
    case Seq("BASENAME", basePathName: String) => true
    case _ => false
  }

  protected def serializeQueryRecording(command: String, args: Seq[Any]): String = args match {
    case Seq(sub @ "TIMESLOT", chanId: ChanId, startTime: MythDateTime) =>
      val time: MythDateTimeString = startTime
      val elems = List(command, sub, MythProtocolSerializer.serialize(chanId), MythProtocolSerializer.serialize(time))
      elems mkString " "
    case Seq(sub @ "BASENAME", basePathName: String) =>
      val elems = List(command, sub, basePathName)
      elems mkString " "
    case _ => throw new IllegalArgumentException
  }

  protected def verifyArgsQueryRecordings(args: Seq[Any]): Boolean = args match {
    case Seq(sortOrFilter: String) => true
    case Seq() => true
    case _ => false
  }

  protected def serializeQueryRecordings(command: String, args: Seq[Any]): String = args match {
    case Seq(sortOrFilter: String) =>
      val elems = List(command, sortOrFilter)
      elems mkString " "
    case Seq() => command
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
      val elems = List(
        command,
        MythProtocolSerializer.serialize(chanId),
        MythProtocolSerializer.serialize(startTime),
        MythProtocolSerializer.serialize(position)
      )
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
      val elems = List(command, MythProtocolSerializer.serialize(chanId), MythProtocolSerializer.serialize(start))
      elems mkString BACKEND_SEP
    case _ => throw new IllegalArgumentException

  }

  /**
    * Myth protocol commands: (from programs/mythbackend/mainserver.cpp)
    */
  private val internalMap = Map[String, (CheckArgs, Serialize)](
    /*
     * ALLOW_SHUTDOWN
     *  @responds sometime; only if tokenCount == 1
     *  @returns "OK"
     */
    "ALLOW_SHUTDOWN" -> (verifyArgsEmpty, serializeEmpty),

    /*
     * ANN Monitor %s %d                <clientHostName> <eventsMode>
     * ANN Playback %s %d               <clientHostName> <eventsMode>
     * ANN MediaServer %s               <clientHostName>
     * ANN SlaveBackend %s %s { %p }*   <slaveHostName> <slaveIPAddr?> [<ProgramInfo>]*
     * ANN FileTransfer %s { %d { %d { %d }}} [%s %s {, %s}*]
     *                    <clientHostName> { writeMode {, useReadAhead {, timeoutMS }}}
     *                    [ url, wantgroup, checkfile {, ...} ]
     *  @responds
     *  @returns
     *      Monitor:
     *      Playback:
     *      MediaServer:
     *      SlaveBackend:
     *      FileTransfer:
     */
    "ANN" -> (verifyArgsAnnounce, serializeAnnounce),

    /*
     * BACKEND_MESSAGE [] [%s {, %s}* ]   [<message> <extra...>]
     *  @responds never
     *  @returns nothing
     */
    "BACKEND_MESSAGE" -> (verifyArgsNOP, serializeNOP),

    /*
     * BLOCK_SHUTDOWN
     *  responds sometimes; only if tokenCount == 1
     *  @returns "OK"
     */
    "BLOCK_SHUTDOWN" -> (verifyArgsEmpty, serializeEmpty),

    /*
     * CHECK_RECORDING [] [%p]     [<ProgramInfo>]
     *  @responds always
     *  @returns boolean 0/1 as to whether the recording is currently taking place
     */
    "CHECK_RECORDING" -> (verifyArgsProgramInfo, serializeProgramInfo),

    /*
     * DELETE_FILE [] [%s, %s]   [<filename> <storage group name>]
     *  @responds sometime; only if slistCount >= 3
     *  @returns Boolean "0" on error, "1" on succesful file deletion
     */
    "DELETE_FILE" -> (verifyArgsDeleteFile, serializeDeleteFile),

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
    "DELETE_RECORDING" -> (verifyArgsDeleteRecording, serializeDeleteRecording),

    /*
     * DONE
     *  @responds never
     *  @returns nothing, closes the client's socket
     */
    "DONE" -> (verifyArgsEmpty, serializeEmpty),

    /*
     * DOWNLOAD_FILE [] [%s, %s, %s]       [<srcURL> <storageGroup> <fileName>]
     *  @responds sometimes; only if slistCount == 4
     *  @returns result token:
     *       downloadfile_directory_not_found
     *       downloadfile_filename_dangerous
     *       OK <storagegroup> <filename>      ??
     *       ERROR                             ?? only if synchronous?
     */
    "DOWNLOAD_FILE" -> (verifyArgsDownloadFile, serializeDownloadFile),

    /*
     * DOWNLOAD_FILE_NOW [] [%s, %s, %s]   [<srcURL> <storageGroup> <fileName>]
     *   (this command sets synchronous = true as opposed to DOWNLOAD_FILE)
     *  @responds sometimes; only if slistCount == 4
     *  @returns see DOWNLOAD_FILE
     */
    "DOWNLOAD_FILE_NOW" -> (verifyArgsDownloadFile, serializeDownloadFile),

    /*
     * FILL_PROGRAM_INFO [] [%s, %p]     [<playback host> <ProgramInfo>]
     *  @responds always
     *  @returns ProgramInfo structure, populated
     *           (if already contained pathname, otherwise unchanged)
     */
    "FILL_PROGRAM_INFO" -> (verifyArgsNOP, serializeNOP),

    /*
     * FORCE_DELETE_RECORDING [] [%p]   [<ProgramInfo>]
     *  @responds sometimes; only if ChanId in program info
     *  @returns see DELETE_RECORDING
     */
    "FORCE_DELETE_RECORDING" -> (verifyArgsProgramInfo, serializeProgramInfo),

    /*
     * FORGET_RECORDING [] [%p]    [<ProgramInfo>]
     *  @responds always
     *  @returns "0"
     */
    "FORGET_RECORDING" -> (verifyArgsProgramInfo, serializeProgramInfo),

    /*
     * FREE_TUNER %d        <cardId>
     *  @responds sometimes; only if tokens == 2
     *  @returns "OK" or "FAILED"
     */
    "FREE_TUNER" -> (verifyArgsFreeTuner, serializeFreeTuner),

    /*
     * GET_FREE_RECORDER
     *  @responds always
     *  @returns [%d, %s, %d] = <best free encoder id> <host or IP> <port>
     *        or [-1, "nohost", -1] if no suitable encoder found
     */
    "GET_FREE_RECORDER" -> (verifyArgsEmpty, serializeEmpty),

    /*
     * GET_FREE_RECORDER_COUNT
     *  @responds always
     *  @returns Int: number of available encoders
     */
    "GET_FREE_RECORDER_COUNT" -> (verifyArgsEmpty, serializeEmpty),

    /*
     * GET_FREE_RECORDER_LIST
     *  @responds always
     *  @returns [%d, {, %d}] = list of available encoder ids, or "0" if none
     */
    "GET_FREE_RECORDER_LIST" -> (verifyArgsEmpty, serializeEmpty),

    /*
     * GET_NEXT_FREE_RECORDER [] [%d]  [<currentRecorder#>]
     *  @responds always
     *  @returns [%d, %s, %d] = <next free encoder id> <host or IP> <port>
     *        or [-1, "nohost", -1] if no suitable encoder found
     */
    "GET_NEXT_FREE_RECORDER" -> (verifyArgsCaptureCard, serializeCaptureCard),

    /*
     * GET_RECORDER_FROM_NUM [] [%d]   [<recorder#>]
     *  @responds always
     *  @returns [%s, %d] = <host or IP> <port>
     *        or ["nohost", -1] if no matching recorder found
     */
    "GET_RECORDER_FROM_NUM" -> (verifyArgsCaptureCard, serializeCaptureCard),

    /*
     * GET_RECORDER_NUM [] [%p]        [<ProgramInfo>]
     *  @responds always
     *  @returns [%d, %s, %d] =   <encoder#> <host or IP> <port>
     *        or [-1, "nohost", -1] if no matching recorder found
     */
    "GET_RECORDER_NUM" -> (verifyArgsProgramInfo, serializeProgramInfo),

    /*
     * GO_TO_SLEEP
     *  @responds always
     *  @returns "OK" or "ERROR: SleepCommand is empty"
     * (only for slaves, but no checking?! Looks @ CoreContext "SleepCommand" setting)
     */
    "GO_TO_SLEEP" -> (verifyArgsEmpty, serializeEmpty),

    /*
     * LOCK_TUNER  (implicitly passes -1 as tuner id, what does this accomplish? first available local tuner?)
     * LOCK_TUNER %d  <cardId>
     *  @responds sometimes; only if tokenCount in { 1, 2 }
     *  @returns [%d, %s, %s, %s]  <cardid> <videodevice> <audiodevice> <vbidevice> (from capturecard table)
     *       or  [-2, "", "", ""]  if tuner is already locked
     *       or  [-1, "", "", ""]  if no tuner found to lock
     */
    "LOCK_TUNER" -> (verifyArgsLockTuner, serializeLockTuner),

    /*
     * MESSAGE [] [ %s {, %s }* ]        [<message> <extra...>]
     * MESSAGE [] [ SET_VERBOSE %s ]     [<verboseMask>]
     * MESSAGE [] [ SET_LOG_LEVEL %s ]   [<logLevel>]
     *  @responds sometimes; if SET_xxx then always, otherwise if slistCount >= 2
     *  @returns          "OK"
     *     SET_VERBOSE:   "OK" or "Failed"
     *     SET_LOG_LEVEL: "OK" or "Failed"
     */
    "MESSAGE" -> (verifyArgsNOP, serializeNOP),

    /*
     * MYTH_PROTO_VERSION %s %s    <version> <protocolToken>
     *  @responds sometimes; only if tokenCount >= 2
     *  @returns "REJECT %d" or "ACCEPT %d" where %d is MYTH_PROTO_VERSION
     */
    "MYTH_PROTO_VERSION" -> (verifyArgsMythProtoVersion, serializeMythProtoVersion),

    /*
     * QUERY_ACTIVE_BACKENDS
     *  @responds always
     *  @returns %d [] [ %s {, %s }* ]  <count> [ hostName, ... ]
     */
    "QUERY_ACTIVE_BACKENDS" -> (verifyArgsEmpty, serializeEmpty),

    /*
     * QUERY_BOOKMARK %d %t   <ChanId> <starttime>
     *  @responds sometimes, only if tokenCount == 3
     *  @returns %ld   <bookmarkPos> (frame number)
     */
    "QUERY_BOOKMARK" -> (verifyArgsChanIdStartTime, serializeChanIdStartTime),

    /*
     * QUERY_CHECKFILE [] [%b, %p]     <checkSlaves> <ProgramInfo>
     *  @responds always
     *  @returns %d %s      <exists:0/1?>  <playbackURL>
     *    note playback url will be "" if file does not exist
     */
    "QUERY_CHECKFILE" -> (verifyArgsQueryCheckFile, serializeQueryCheckFile),

    /*
     * QUERY_COMMBREAK %d %t           <ChanId> <starttime>
     *  @responds sometimes; only if tokenCount == 3
     *  @returns %d {[ %d %d ]* }
     *              first integer is count of tuples (-1 if none found?)
     *              tuples are (mark type, mark pos) from recordedmarkup
     *          gather result into a tuple of two lists (start/end of a commbreak)
     *          of course it is possible for one side to be missing? what do we do then?
     *
     */
    "QUERY_COMMBREAK" -> (verifyArgsChanIdStartTime, serializeChanIdStartTime),

    /*
     * QUERY_CUTLIST %d %t             <ChanId> <starttime>
     *  @responds sometimes; only if tokenCount == 3
     *  @returns TODO some sort of IntList? see QUERY_COMMBREAK for thoughts
     */
    "QUERY_CUTLIST" -> (verifyArgsChanIdStartTime, serializeChanIdStartTime),

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
    "QUERY_FILE_EXISTS" -> (verifyArgsQueryFileExists, serializeQueryFileExists),

    /*
     * QUERY_FILE_HASH [] [%s, %s {, %s}]     <filename> <storageGroup> {<hostname>}
     *  @responds sometimes; only if slistCount >= 3
     *  @returns
     *      ""  on error checking for file, invalid input
     *      %s  hash of the file (currently 64-bit, so 16 hex characters)
     */
    "QUERY_FILE_HASH" -> (verifyArgsQueryFileHash, serializeQueryFileHash),

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
    "QUERY_FILETRANSFER" -> (verifyArgsNOP, serializeNOP),

    /*
     * QUERY_FREE_SPACE
     *  @responds always
     *  @returns  TODO
     */
    "QUERY_FREE_SPACE" -> (verifyArgsEmpty, serializeEmpty),

    /*
     * QUERY_FREE_SPACE_LIST
     *  @responds always
     *  @returns TODO
     *
     * Like QUERY_FREE_SPACE but returns free space on all hosts, each directory
     * is reported as a URL, and a TotalDiskSpace is appended.
     */
    "QUERY_FREE_SPACE_LIST" -> (verifyArgsEmpty, serializeEmpty),

    /*
     * QUERY_FREE_SPACE_SUMMARY
     *  @responds always
     *  @returns [%d, %d]    <total size> <used size>  sizes are in kB (1024-byte blocks)
     *        or [ 0, 0 ]    if there was any sort of error
     */
    "QUERY_FREE_SPACE_SUMMARY" -> (verifyArgsEmpty, serializeEmpty),

    /*
     * QUERY_GENPIXMAP2 [] [%s, %p, more?]     TODO %s is a "token", can be the literal "do_not_care"
     *  @responds always?
     *  @returns ["OK", %s]    <filename>
     *       or ?? TODO follow up on successful return indication/other errors from slave pixmap generation
     *       or ["ERROR", "TOO_FEW_PARAMS"]
     *       or ["ERROR", "TOKEN_ABSENT"]
     *       or ["BAD", "NO_PATHNAME"]
     *       or ["ERROR", "FILE_INACCESSIBLE"]
     */
    "QUERY_GENPIXMAP2" -> (verifyArgsNOP, serializeNOP),

    /*
     * QUERY_GETALLPENDING { %s {, %d}}  { <tmptable> {, <recordid>}}
     *  @responds always
     *  @returns ? [%p {, %p}]  <list of ProgramInfo>  // TODO does this begin with a count?
     *        or ["0", "0"] if not availble/error?
     *  TODO what is the purpose of the optional tmptable and recordid parameters?
     */
    "QUERY_GETALLPENDING" -> (verifyArgsQueryGetAllPending, serializeQueryGetAllPending),

    /*
     * QUERY_GETALLSCHEDULED
     *  @responds always
     *  @returns ? [%p {, %p}]  <list of ProgramInfo>  // TODO does this begin with a count?
     *        or "0" if not availble/error?
     */
    "QUERY_GETALLSCHEDULED" -> (verifyArgsEmpty, serializeEmpty),

    /*
     * QUERY_GETCONFLICTING [] [%p]     [<ProgramInfo>]
     *  @responds always
     *  @returns ? [%p {, %p}]  <list of ProgramInfo>  // TODO does this begin with a count?
     *        or "0" if not availble/error?
     */
    "QUERY_GETCONFLICTING" -> (verifyArgsProgramInfo, serializeProgramInfo),

    /*
     * QUERY_GETEXPIRING
     *  @responds always
     *  @returns ? [%p {, %p}]  <list of ProgramInfo>  // TODO does this begin with a count?
     *        or "0" if not availble/error?
     */
    "QUERY_GETEXPIRING" -> (verifyArgsEmpty, serializeEmpty),

    /*
     * QUERY_GUIDEDATATHROUGH
     *  @responds always
     *  @returns: Date/Time as a string in "YYYY-MM-DD hh:mm" format
     *         or "0000-00-00 00:00" in case of error or no data
     */
    "QUERY_GUIDEDATATHROUGH" -> (verifyArgsEmpty, serializeEmpty),

    /*
     * QUERY_HOSTNAME
     *  @responds always
     *  @returns %s  <hostname>
     */
    "QUERY_HOSTNAME" -> (verifyArgsEmpty, serializeEmpty),

    /*
     * QUERY_IS_ACTIVE_BACKEND [] [%s]   [<hostname>]
     *  @responds sometimes; only if tokenCount == 1
     *  @returns "TRUE" or "FALSE"
     * TODO may case NPE if hostname is not passed?
     *      what does QtStringList array index out of bounds do?
     */
    "QUERY_IS_ACTIVE_BACKEND" -> (verifyArgsQueryIsActiveBackend, serializeQueryIsActiveBackend),

    /*
     * QUERY_ISRECORDING
     *  @responds always
     *  @returns [%d, %d]  <numRecordingsInProgress> <numLiveTVinProgress>
     *                           (liveTV is a subset of recordings)
     */
    "QUERY_ISRECORDING" -> (verifyArgsEmpty, serializeEmpty),

    /*
     * QUERY_LOAD
     *  @responds always
     *  @returns [%f, %f, %f]   1-min  5-min  15-min load averages
     *        or ["ERROR", "getloadavg() failed"] in case of error
     */
    "QUERY_LOAD" -> (verifyArgsEmpty, serializeEmpty),

    /*
     * QUERY_MEMSTATS
     *  @responds always
     *  @returns [%d, %d, %d, %d]  <totalMB> <freeMB> <totalVM> <freeVM>
     *        or ["ERROR", "Could not determine memory stats."] on error
     */
    "QUERY_MEMSTATS" -> (verifyArgsEmpty, serializeEmpty),

    /*
     * QUERY_PIXMAP_GET_IF_MODIFIED [] [%d, %d, %p]  [<time:cachemodified> <maxFileSize> <ProgramInfo>]
     *  @responds always?
     *  @returns TODO figure out what get returned when it succeeds!
     *        or ["ERROR", "1: Parameter list too short"]
     *        or ["ERROR", "2: Invalid ProgramInfo"]
     *        or ["ERROR", "3: Failed to read preview file..."]
     *        or ["ERROR", "4: Preview file is invalid"]
     *        or ["ERROR", "5: Could not locate mythbackend that made this recording"
     *        or ["WARNING", "2: Could not locate requested file"]
     */
    "QUERY_PIXMAP_GET_IF_MODIFIED" -> (verifyArgsNOP, serializeNOP),

    /*
     * QUERY_PIXMAP_LASTMODIFIED [] [%p]      [<ProgramInfo>]
     *  @responds
     *  @returns %ld    <last modified (timestamp?)>
     *        or "BAD"
     */
    "QUERY_PIXMAP_LASTMODIFIED" -> (verifyArgsProgramInfo, serializeProgramInfo),

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
    "QUERY_RECORDER" -> (verifyArgsNOP, serializeNOP),

    /*
     * QUERY_RECORDING BASENAME %s                  <pathname>
     * QUERY_RECORDING TIMESLOT %d %mt             <ChanId> <starttime>
     *  NB starttime is in myth/ISO string format rather than in timestamp
     *  @responds sometimes; only if tokenCount >= 3 (or >= 4 if TIMESLOT is specified)
     *  @returns ["OK", <ProgramInfo>] or "ERROR"
     */
    "QUERY_RECORDING" -> (verifyArgsQueryRecording, serializeQueryRecording),

    /*
     * QUERY_RECORDING_DEVICE
     *   not implemented on backend server
     */
    "QUERY_RECORDING_DEVICE" -> (verifyArgsNOP, serializeNOP),

    /*
     * QUERY_RECORDING_DEVICES
     *   not implemented on backend server
     */
    "QUERY_RECORDING_DEVICES" -> (verifyArgsNOP, serializeNOP),

    /*
     * QUERY_RECORDINGS { Ascending | Descending | Unsorted | Recording }
     *  @responds sometimes; only if tokenCount == 2
     *  @returns [ %p {, %p}*]   list of ProgramInfo records
     */
    "QUERY_RECORDINGS" -> (verifyArgsQueryRecordings, serializeQueryRecordings),

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
    "QUERY_REMOTEENCODER" -> (verifyArgsNOP, serializeNOP),

    /*
     * QUERY_SETTING %s %s      <hostname> <settingName>
     *  @responds sometimes; only if tokenCount == 3
     *  @returns %s or "-1" if not found   <settingValue>
     */
    "QUERY_SETTING" -> (verifyArgsQuerySetting, serializeQuerySetting),

    /* QUERY_SG_GETFILELIST [] [%s, %s, %s {, %b}]  <wantHost> <groupname> <path> { fileNamesOnly> } */
    "QUERY_SG_GETFILELIST" -> (verifyArgsNOP, serializeNOP),

    /* QUERY_SG_FILEQUERY [] [%s, %s, %s]           <wantHost> <groupName> <filename> */
    "QUERY_SG_FILEQUERY" -> (verifyArgsNOP, serializeNOP),

    /*
     * QUERY_TIME_ZONE
     *  @responds always
     *  @returns [%s, %d, %s]  <timezoneName> <offsetSecsFromUtc> <currentTimeUTC>
     *    currentTimeUTC is in the ISO format "YYYY-MM-ddThh:mm:ssZ"
     */
    "QUERY_TIME_ZONE" -> (verifyArgsEmpty, serializeEmpty),

    /*
     * QUERY_UPTIME
     *  @responds always
     *  @returns %ld  <uptimeSeconds>
     *        or ["ERROR", "Could not determine uptime."] in case of error
     */
    "QUERY_UPTIME" -> (verifyArgsEmpty, serializeEmpty),

    /*
     * REFRESH_BACKEND
     *  @responds always
     *  @returns "OK"
     *  Seems to be a NOP on the server.
     */
    "REFRESH_BACKEND" -> (verifyArgsEmpty, serializeEmpty),

    /*
     * RESCHEDULE_RECORDINGS [] [CHECK %d %d %d {Python}, '', '', '', {**any**}]
     * RESCHEDULE_RECORDINGS [] [MATCH %d %d %d {- Python}]
     *   TODO look @ Scheduler::HandleReschedule in programs/mythbackend/scheduler.cpp
     */
    "RESCHEDULE_RECORDINGS" -> (verifyArgsNOP, serializeNOP),

    /*
     * SCAN_VIDEOS
     *  @responds always
     *  @returns "OK" or "ERROR"
     */
    "SCAN_VIDEOS" -> (verifyArgsEmpty, serializeEmpty),

    /*
     * SET_BOOKMARK %d %t %ld          <ChanId> <starttime> <frame#position>
     *  @responds sometimes; only if tokenCount == 4
     *  @returns "OK" or "FAILED"
     */
    "SET_BOOKMARK" -> (verifyArgsSetBookmark, serializeSetBookmark),

    /*
     * SET_CHANNEL_INFO [] [%d, %d, %d, %d, %d, %d, %d]
     *                     <ChanId> <sourceid> <oldcnum> <callsign> <channum> <channame> <xmltv>
     *  @responds always
     *  @returns "1" for successful otherwise "0"
     */
    "SET_CHANNEL_INFO" -> (verifyArgsNOP, serializeNOP),

    /*
     * SET_NEXT_LIVETV_DIR %d %s  <encoder#> <dir>
     *  @responds sometimes; only if tokenCount == 3
     *  @returns "OK or "bad" if encoder nor found
     */
    "SET_NEXT_LIVETV_DIR" -> (verifyArgsNOP, serializeNOP),

    /*
     * SET_SETTING %s %s %s       <hostname> <settingname> <value>
     *  @responds sometimes; only if tokenCount == 4
     *  @returns "OK" or "ERROR"
     */
    "SET_SETTING" -> (verifyArgsSetSetting, serializeSetSetting),

    /*
     * SHUTDOWN_NOW { %s }        { <haltCommand> }
     *  @responds never
     *  @returns nothing
     */
    "SHUTDOWN_NOW" -> (verifyArgsShutdownNow, serializeShutdownNow),

    /*
     * STOP_RECORDING [] [<ProgramInfo>]
     *  @responds sometimes; only if recording is found
     *  @returns "0" if recording was on a slave backend
     *           "%d" if recording was on a local encoder, <recnum>
     *        or "-1" if not found
     */
    "STOP_RECORDING" -> (verifyArgsProgramInfo, serializeProgramInfo),

    /*
     * UNDELETE_RECORDING [] [%d, %mt]       [<ChanId> <starttime>]
     * UNDELETE_RECORDING [] [%p]            [<ProgramInfo>]
     * NB starttime is in myth/ISO string format rather than timestamp
     *  @responds sometimes; if program info has ChanId
     *  @returns "0" on success and "-1" on error
     */
    "UNDELETE_RECORDING" -> (verifyArgsUndeleteRecording, serializeUndeleteRecording)
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
}

object MythProtocol extends MythProtocol {
  final val BACKEND_SEP: String = "[]:[]"
  final val SPLIT_PATTERN: String = Pattern.quote(BACKEND_SEP)

  // TODO this is just for testing
  def verify(command: String, args: Any*): Option[String] = {
    if (commands contains command) {
      val (check, serialize) = commands(command)
      if (check(args))
        Some(serialize(command, args))
      else {
        println("failed argument type check")
        None
      }
    } else {
      println(s"invalid command $command")
      None
    }
  }
}

trait MythProtocolAPI {
  def allowShutdown(): Boolean
  def blockShutdown(): Boolean
  def checkRecording(rec: Recording): Any
  def done(): Unit
  def fillProgramInfo(playbackHost: String, p: Recording): Recording
  def forceDeleteRecording(rec: Recording)
  def forgetRecording(rec: Recording): Int   // TODO something better to indicate success/failure; Either?
  def getFreeRecorder: Any // need encoding of "Encoder" -> ID, host/IP, port
  def getFreeRecorderCount: Int
  def getFreeRecorderList: List[Any]  // TODO see getFreeRecorder for return type
  def getNextFreeRecorder(encoderId: Int): Any // see above for return type
  def getRecorderFromNum(encoderId: Int): Any  // see above for return type
  def getRecorderNum(rec: Recording): Any      // see above for return type
  def goToSleep(): Boolean  // TODO a way to return error message if any
  def lockTuner(): Any // TODO capture the appropriate return type
  def lockTuner(cardId: Int): Any // see above for return type
  def protocolVersion(ver: Int, token: String): (Boolean, Int)
  def queryActiveBackends: List[String]
  def queryBookmark(chanId: ChanId, startTime: MythDateTime): VideoPosition
  def queryCommBreak(chanId: ChanId, startTime: MythDateTime): Long  // TODO List? frame number/position
  def queryCutList(chanId: ChanId, startTime: MythDateTime): Long    // TODO List? frame number/position
  def queryFileExists(fileName: String, storageGroup: String): (String, FileStats)
  def queryFileHash(fileName: String, storageGroup: String, hostName: String = ""): String
  def queryFreeSpace: List[FreeSpace]
  def queryFreeSpaceList: List[FreeSpace]
  def queryFreeSpaceSummary: (ByteCount, ByteCount)
  def queryGetAllPending: ExpectedCountIterator[Recording]
  def queryGetAllScheduled: ExpectedCountIterator[Recording]
  def queryGetConflicting: Iterable[Recording]  // TODO expected count iterator?
  def queryGetExpiring: ExpectedCountIterator[Recording]
  def queryGuideDataThrough: MythDateTime
  def queryHostname: String
  def queryIsActiveBackend: Boolean
  def queryIsRecording: (Int, Int)
  def queryLoad: (Double, Double, Double)
  def queryMemStats: (Int, Int, Int, Int)
  def queryPixmapLastModified(rec: Recording): MythDateTime
  def queryRecording(pathName: String): Recording
  def queryRecording(chanId: ChanId, startTime: MythDateTime): Recording
  def querySetting(hostName: String, settingName: String): Option[String]
  def queryTimeZone: (String, ZoneOffset, Instant)
  def queryUptime: Duration
  def refreshBackend: Boolean
  def scanVideos: Boolean
  def setBookmark(chanId: ChanId, startTime: MythDateTime, pos: VideoPosition): Boolean
  def setSetting(hostName: String, settingName: String, value: String): Boolean
  def shutdownNow(haltCommand: String = ""): Unit
  def stopRecording(rec: Recording): Int  // TODO better encapsulate return codes
  def undeleteRecording(rec: Recording): Boolean
  def undeleteRecording(chanId: ChanId, startTime: MythDateTime): Boolean
  // TODO more methods
}

private[myth] trait MythProtocol77 extends MythProtocol {
//  final val PROTO_VERSION = 77        // "75"
//  final val PROTO_TOKEN = "WindMark"  // "SweetRock"
}
