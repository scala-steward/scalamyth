package mythtv
package connection
package myth

import java.time.{ Duration, Instant, LocalDate, ZoneOffset }
import java.util.regex.Pattern

import model.{ ChanId, FreeSpace, Recording }
import util.MythDateTime

trait MythProtocol {
  // TODO move constants to a companion object?
  // TODO can we structure this so that it's possible to support more than one protocol version?
  final val PROTO_VERSION = 77        // "75"
  final val PROTO_TOKEN = "WindMark"  // "SweetRock"
  final val BACKEND_SEP = "[]:[]"

  type CheckArgsFunc = (Seq[Any]) => Boolean

  def commands: Map[String, CheckArgsFunc] = internalMap

  def verifyArgsNOP(args: Seq[Any]): Boolean = true

  def verifyArgsEmpty(args: Seq[Any]): Boolean = args match {
    case Seq() => true
    case _ => false
  }

  def verifyArgsProgramInfo(args: Seq[Any]): Boolean = args match {
    case Seq(x: Recording) => true
    case _ => false
  }

  def verifyArgsChanIdStartTime(args: Seq[Any]): Boolean = args match {
    case Seq(chanId: ChanId, startTime: MythDateTime) => true
    case _ => false
  }

  def verifyArgsPIorChanIdStart(args: Seq[Any]): Boolean = args match {
    case Seq(x: Recording) => true
    case Seq(chanId: ChanId, startTime: MythDateTime) => true
    case _ => false
  }

  /***/

  def verifyArgsDeleteFile(args: Seq[Any]): Boolean = args match {
    case Seq(fileName: String, storageGroup: String) => true
    case _ => false
  }

  def verifyArgsDeleteRecording(args: Seq[Any]): Boolean = args match {
    case Seq(x: Recording) => true
    case Seq(chanId: ChanId, startTime: MythDateTime) => true
    case Seq(chanId: ChanId, startTime: MythDateTime, forceOpt: String) => true
    case Seq(chanId: ChanId, startTime: MythDateTime, forceOpt: String, forgetOpt: String) => true
    case _ => false
  }

  def verifyArgsDownloadFile(args: Seq[Any]): Boolean = args match {
    case Seq(srcURL: String, storageGroup: String, fileName: String) => true
    case _ => false
  }

  def verifyArgsMythProtoVersion(args: Seq[Any]): Boolean = args match {
    case Seq(version: Int, token: String) => true
    case _ => false
  }

  def verifyArgsQueryCheckFile(args: Seq[Any]): Boolean = args match {
    case Seq(checkSlaves: Boolean, rec: Recording) => true
    case _ => false
  }

  def verifyArgsQueryFileExists(args: Seq[Any]): Boolean = args match {
    case Seq(fileName: String, storageGroup: String) => true
    case Seq(fileName: String) => true
    case _ => false
  }

  def verifyArgsQueryFileHash(args: Seq[Any]): Boolean = args match {
    case Seq(fileName: String, storageGroup: String, hostName: String) => true
    case Seq(fileName: String, storageGroup: String) => true
    case _ => false
  }

  def verifyArgsQueryIsActiveBackend(args: Seq[Any]): Boolean = args match {
    case Seq(hostName: String) => true
    case _ => false
  }

  def verifyArgsQueryRecording(args: Seq[Any]): Boolean = args match {
    case Seq(chanId: ChanId, startTime: MythDateTime) => true
    case Seq(basePathName: String) => true
    case _ => false
  }

  def verifyArgsQueryRecordings(args: Seq[Any]): Boolean = args match {
    case Seq(sortOrFilter: String) => true
    case Seq() => true
    case _ => false
  }

  def verifyArgsQuerySetting(args: Seq[Any]): Boolean = args match {
    case Seq(hostName: String, settingName: String) => true
    case _ => false
  }

  def verifyArgsSetSetting(args: Seq[Any]): Boolean = args match {
    case Seq(hostName: String, settingName: String, settingValue: String) => true
    case _ => false
  }

  def verifyArgsShutdownNow(args: Seq[Any]): Boolean = args match {
    case Seq(haltCommand: String) => true
    case Seq() => true
    case _ => false
  }

  /**
    * Myth protocol commands: (from programs/mythbackend/mainserver.cpp)
    */
  private val internalMap = Map[String, CheckArgsFunc](
    /*
     * ALLOW_SHUTDOWN
     *  @responds sometime; only if tokenCount == 1
     *  @returns "OK"
     */
    "ALLOW_SHUTDOWN" -> verifyArgsEmpty,

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
     */
    "ANN" -> verifyArgsNOP,

    /*
     * BACKEND_MESSAGE [] [%s {, %s}* ]   [<message> <extra...>]
     *  @responds never
     *  @returns nothing
     */
    "BACKEND_MESSAGE" -> verifyArgsNOP,

    /*
     * BLOCK_SHUTDOWN
     *  responds sometimes; only if tokenCount == 1
     *  @returns "OK"
     */
    "BLOCK_SHUTDOWN" -> verifyArgsEmpty,

    /*
     * CHECK_RECORDING [] [%p]     [<ProgramInfo>]
     *  @responds always
     *  @returns boolean 0/1 as to whether the recording is currently taking place
     */
    "CHECK_RECORDING" -> verifyArgsProgramInfo,

    /*
     * DELETE_FILE [] [%s, %s]   [<filename> <storage group name>]
     *  @responds sometime; only if slistCount >= 3
     *  @returns Boolean "0" on error, "1" on succesful file deletion
     */
    "DELETE_FILE" -> verifyArgsDeleteFile,

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
    "DELETE_RECORDING" -> verifyArgsDeleteRecording,

    /*
     * DONE
     *  @responds never
     *  @returns nothing, closes the client's socket
     */
    "DONE" -> verifyArgsEmpty,

    /*
     * DOWNLOAD_FILE [] [%s, %s, %s]       [<srcURL> <storageGroup> <fileName>]
     *  @responds sometimes; only if slistCount == 4
     *  @returns result token:
     *       downloadfile_directory_not_found
     *       downloadfile_filename_dangerous
     *       OK <storagegroup> <filename>      ??
     *       ERROR                             ?? only if synchronous?
     */
    "DOWNLOAD_FILE" -> verifyArgsDownloadFile,

    /*
     * DOWNLOAD_FILE_NOW [] [%s, %s, %s]   [<srcURL> <storageGroup> <fileName>]
     *   (this command sets synchronous = true as opposed to DOWNLOAD_FILE)
     *  @responds sometimes; only if slistCount == 4
     *  @returns see DOWNLOAD_FILE
     */
    "DOWNLOAD_FILE_NOW" -> verifyArgsDownloadFile,

    /*
     * FILL_PROGRAM_INFO [] [%s, %p]     [<playback host> <ProgramInfo>]
     *  @responds always
     *  @returns ProgramInfo structure, populated
     *           (if already contained pathname, otherwise unchanged)
     */
    "FILL_PROGRAM_INFO" -> verifyArgsNOP,

    /*
     * FORCE_DELETE_RECORDING [] [%p]   [<ProgramInfo>]
     *  @responds sometimes; only if ChanId in program info
     *  @returns see DELETE_RECORDING
     */
    "FORCE_DELETE_RECORDING" -> verifyArgsProgramInfo,

    /*
     * FORGET_RECORDING [] [%p]    [<ProgramInfo>]
     *  @responds always
     *  @returns "0"
     */
    "FORGET_RECORDING" -> verifyArgsProgramInfo,

    /*
     * FREE_TUNER %d        <cardId>
     *  @responds sometimes; only if tokens == 2
     *  @returns "OK" or "FAILED"
     */
    "FREE_TUNER" -> verifyArgsNOP,

    /*
     * GET_FREE_RECORDER
     *  @responds always
     *  @returns [%d, %s, %d] = <best free encoder id> <host or IP> <port>
     *        or [-1, "nohost", -1] if no suitable encoder found
     */
    "GET_FREE_RECORDER" -> verifyArgsEmpty,

    /*
     * GET_FREE_RECORDER_COUNT
     *  @responds always
     *  @returns Int: number of available encoders
     */
    "GET_FREE_RECORDER_COUNT" -> verifyArgsEmpty,

    /*
     * GET_FREE_RECORDER_LIST
     *  @responds always
     *  @returns [%d, {, %d}] = list of available encoder ids, or "0" if none
     */
    "GET_FREE_RECORDER_LIST" -> verifyArgsEmpty,

    /*
     * GET_NEXT_FREE_RECORDER [] [%d]  [<currentRecorder#>]
     *  @responds always
     *  @returns [%d, %s, %d] = <next free encoder id> <host or IP> <port>
     *        or [-1, "nohost", -1] if no suitable encoder found
     */
    "GET_NEXT_FREE_RECORDER" -> verifyArgsNOP,

    /*
     * GET_RECORDER_FROM_NUM [] [%d]   [<recorder#>]
     *  @responds always
     *  @returns [%s, %d] = <host or IP> <port>
     *        or ["nohost", -1] if no matching recorder found
     */
    "GET_RECORDER_FROM_NUM" -> verifyArgsNOP,

    /*
     * GET_RECORDER_NUM [] [%p]        [<ProgramInfo>]
     *  @responds always
     *  @returns [%s, %d] = <host or IP> <port>
     *        or ["nohost", -1] if no matching recorder found
     */
    "GET_RECORDER_NUM" -> verifyArgsProgramInfo,

    /*
     * GO_TO_SLEEP
     *  @responds always
     *  @returns "OK" or "ERROR: SleepCommand is empty"
     * (only for slaves, but no checking?! Looks @ CoreContext "SleepCommand" setting)
     */
    "GO_TO_SLEEP" -> verifyArgsEmpty,

    /*
     * LOCK_TUNER  (implicitly passes -1 as tuner id, what does this accomplish? first available local tuner?)
     * LOCK_TUNER %d  <cardId>
     *  @responds sometimes; only if tokenCount in { 1, 2 }
     *  @returns [%d, %s, %s, %s]  <cardid> <videodevice> <audiodevice> <vbidevice> (from capturecard table)
     *       or  [-2, "", "", ""]  if tuner is already locked
     *       or  [-1, "", "", ""]  if no tuner found to lock
     */
    "LOCK_TUNER" -> verifyArgsNOP,

    /*
     * MESSAGE [] [ %s {, %s }* ]        [<message> <extra...>]
     * MESSAGE [] [ SET_VERBOSE %s ]     [<verboseMask>]
     * MESSAGE [] [ SET_LOG_LEVEL %s ]   [<logLevel>]
     *  @responds sometimes; if SET_xxx then always, otherwise if slistCount >= 2
     *  @returns          "OK"
     *     SET_VERBOSE:   "OK" or "Failed"
     *     SET_LOG_LEVEL: "OK" or "Failed"
     */
    "MESSAGE" -> verifyArgsNOP,

    /*
     * MYTH_PROTO_VERSION %s %s    <version> <protocolToken>
     *  @responds sometimes; only if tokenCount >= 2
     *  @returns "REJECT %d" or "ACCEPT %d" where %d is MYTH_PROTO_VERSION
     */
    "MYTH_PROTO_VERSION" -> verifyArgsMythProtoVersion,

    /*
     * QUERY_ACTIVE_BACKENDS
     *  @responds always
     *  @returns %d [] [ %s {, %s }* ]  <count> [ hostName, ... ]
     */
    "QUERY_ACTIVE_BACKENDS" -> verifyArgsEmpty,

    /*
     * QUERY_BOOKMARK %d %t   <ChanId> <starttime>
     *  @responds sometimes, only if tokenCount == 3
     *  @returns %ld   <bookmarkPos> (frame number)
     */
    "QUERY_BOOKMARK" -> verifyArgsChanIdStartTime,

    /*
     * QUERY_CHECKFILE [] [%b, %p]     <checkSlaves> <ProgramInfo>
     *  @responds always
     *  @returns %d %s      <exists:0/1?>  <playbackURL>
     *    note playback url will be "" if file does not exist
     */
    "QUERY_CHECKFILE" -> verifyArgsQueryCheckFile,

    /*
     * QUERY_COMMBREAK %d %t           <ChanId> <starttime>
     *  @responds sometimes; only if tokenCount == 3
     *  @returns TODO some sort of IntList?
     */
    "QUERY_COMMBREAK" -> verifyArgsChanIdStartTime,

    /*
     * QUERY_CUTLIST %d %t             <ChanId> <starttime>
     *  @responds sometimes; only if tokenCount == 3
     *  @returns TODO some sort of IntList?
     */
    "QUERY_CUTLIST" -> verifyArgsChanIdStartTime,

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
    "QUERY_FILE_EXISTS" -> verifyArgsQueryFileExists,

    /*
     * QUERY_FILE_HASH [] [%s, %s {, %s}]     <filename> <storageGroup> {<hostname>}
     *  @responds sometimes; only if slistCount >= 3
     *  @returns
     *      ""  on error checking for file, invalid input
     *      %s  hash of the file (currently 64-bit, so 16 hex characters)
     */
    "QUERY_FILE_HASH" -> verifyArgsQueryFileHash,

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
    "QUERY_FILETRANSFER" -> verifyArgsNOP,

    /*
     * QUERY_FREE_SPACE
     *  @responds always
     *  @returns  TODO
     */
    "QUERY_FREE_SPACE" -> verifyArgsEmpty,

    /*
     * QUERY_FREE_SPACE_LIST
     *  @responds always
     *  @returns TODO
     *
     * Like QUERY_FREE_SPACE but returns free space on all hosts, each directory
     * is reported as a URL, and a TotalDiskSpace is appended.
     */
    "QUERY_FREE_SPACE_LIST" -> verifyArgsEmpty,

    /*
     * QUERY_FREE_SPACE_SUMMARY
     *  @responds always
     *  @returns [%d, %d]    <total size> <used size>  sizes are in kB (1024-byte blocks)
     *        or [ 0, 0 ]    if there was any sort of error
     */
    "QUERY_FREE_SPACE_SUMMARY" -> verifyArgsEmpty,

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
    "QUERY_GENPIXMAP2" -> verifyArgsNOP,

    /*
     * QUERY_GETALLPENDING { %s {, %d}}  { <tmptable> {, <recordid>}}
     *  @responds always
     *  @returns ? [%p {, %p}]  <list of ProgramInfo>  // TODO does this begin with a count?
     *        or ["0", "0"] if not availble/error?
     *  TODO what is the purpose of the optional tmptable and recordid parameters?
     */
    "QUERY_GETALLPENDING" -> verifyArgsNOP,

    /*
     * QUERY_GETALLSCHEDULED
     *  @responds always
     *  @returns ? [%p {, %p}]  <list of ProgramInfo>  // TODO does this begin with a count?
     *        or "0" if not availble/error?
     */
    "QUERY_GETALLSCHEDULED" -> verifyArgsEmpty,

    /*
     * QUERY_GETCONFLICTING [] [%p]     [<ProgramInfo>]
     *  @responds always
     *  @returns ? [%p {, %p}]  <list of ProgramInfo>  // TODO does this begin with a count?
     *        or "0" if not availble/error?
     */
    "QUERY_GETCONFLICTING" -> verifyArgsProgramInfo,

    /*
     * QUERY_GETEXPIRING
     *  @responds always
     *  @returns ? [%p {, %p}]  <list of ProgramInfo>  // TODO does this begin with a count?
     *        or "0" if not availble/error?
     */
    "QUERY_GETEXPIRING" -> verifyArgsEmpty,

    /*
     * QUERY_GUIDEDATATHROUGH
     *  @responds always
     *  @returns: Date/Time as a string in "YYYY-MM-DD hh:mm" format
     *         or "0000-00-00 00:00" in case of error or no data
     */
    "QUERY_GUIDEDATATHROUGH" -> verifyArgsEmpty,

    /*
     * QUERY_HOSTNAME
     *  @responds always
     *  @returns %s  <hostname>
     */
    "QUERY_HOSTNAME" -> verifyArgsEmpty,

    /*
     * QUERY_IS_ACTIVE_BACKEND [] [%s]   [<hostname>]
     *  @responds sometimes; only if tokenCount == 1
     *  @returns "TRUE" or "FALSE"
     * TODO may case NPE if hostname is not passed?
     *      what does QtStringList array index out of bounds do?
     */
    "QUERY_IS_ACTIVE_BACKEND" -> verifyArgsQueryIsActiveBackend,

    /*
     * QUERY_ISRECORDING
     *  @responds always
     *  @returns [%d, %d]  <numRecordingsInProgress> <numLiveTVinProgress>
     *                           (liveTV is a subset of recordings)
     */
    "QUERY_ISRECORDING" -> verifyArgsEmpty,

    /*
     * QUERY_LOAD
     *  @responds always
     *  @returns [%f, %f, %f]   1-min  5-min  15-min load averages
     *        or ["ERROR", "getloadavg() failed"] in case of error
     */
    "QUERY_LOAD" -> verifyArgsEmpty,

    /*
     * QUERY_MEMSTATS
     *  @responds always
     *  @returns [%d, %d, %d, %d]  <totalMB> <freeMB> <totalVM> <freeVM>
     *        or ["ERROR", "Could not determine memory stats."] on error
     */
    "QUERY_MEMSTATS" -> verifyArgsEmpty,

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
    "QUERY_PIXMAP_GET_IF_MODIFIED" -> verifyArgsNOP,

    /*
     * QUERY_PIXMAP_LASTMODIFIED [] [%p]      [<ProgramInfo>]
     *  @responds
     *  @returns %ld    <last modified (timestamp?)>
     *        or "BAD"
     */
    "QUERY_PIXMAP_LASTMODIFIED" -> verifyArgsProgramInfo,

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
    "QUERY_RECORDER" -> verifyArgsNOP,

    /*
     * QUERY_RECORDING BASENAME, %s                  <pathname>
     * QUERY_RECORDING TIMESLOT, %d, %mt             <ChanId> <starttime>
     *  NB starttime is in myth/ISO string format rather than in timestamp
     *  @responds sometimes; only if tokenCount >= 3 (or >= 4 if TIMESLOT is specified)
     *  @returns ["OK", <ProgramInfo>] or "ERROR"
     */
    "QUERY_RECORDING" -> verifyArgsQueryRecording,

    /*
     * QUERY_RECORDING_DEVICE
     *   not implemented on backend server
     */
    "QUERY_RECORDING_DEVICE" -> verifyArgsNOP,

    /*
     * QUERY_RECORDING_DEVICES
     *   not implemented on backend server
     */
    "QUERY_RECORDING_DEVICES" -> verifyArgsNOP,

    /*
     * QUERY_RECORDINGS { Ascending | Descending | Unsorted | Recording }
     *  @responds sometimes; only if tokenCount == 2
     *  @returns [ %p {, %p}*]   list of ProgramInfo records
     */
    "QUERY_RECORDINGS" -> verifyArgsQueryRecordings,

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
    "QUERY_REMOTEENCODER" -> verifyArgsNOP,

    /*
     * QUERY_SETTING %s %s      <hostname> <settingName>
     *  @responds sometimes; only if tokenCount == 3
     *  @returns %s or "-1" if not found   <settingValue>
     */
    "QUERY_SETTING" -> verifyArgsQuerySetting,

    /* QUERY_SG_GETFILELIST [] [%s, %s, %s {, %b}]  <wantHost> <groupname> <path> { fileNamesOnly> } */
    "QUERY_SG_GETFILELIST" -> verifyArgsNOP,

    /* QUERY_SG_FILEQUERY [] [%s, %s, %s]           <wantHost> <groupName> <filename> */
    "QUERY_SG_FILEQUERY" -> verifyArgsNOP,

    /*
     * QUERY_TIME_ZONE
     *  @responds always
     *  @returns [%s, %d, %s]  <timezoneName> <offsetSecsFromUtc> <currentTimeUTC>
     *    currentTimeUTC is in the ISO format "YYYY-MM-ddThh:mm:ssZ"
     */
    "QUERY_TIME_ZONE" -> verifyArgsEmpty,

    /*
     * QUERY_UPTIME
     *  @responds always
     *  @returns %ld  <uptimeSeconds>
     *        or ["ERROR", "Could not determine uptime."] in case of error
     */
    "QUERY_UPTIME" -> verifyArgsEmpty,

    /*
     * REFRESH_BACKEND
     *  @responds always
     *  @returns "OK"
     *  Seems to be a NOP on the server.
     */
    "REFRESH_BACKEND" -> verifyArgsEmpty,

    /*
     * RESCHEDULE_RECORDINGS [] [CHECK %d %d %d {Python}, '', '', '', {**any**}]
     * RESCHEDULE_RECORDINGS [] [MATCH %d %d %d {- Python}]
     *   TODO look @ Scheduler::HandleReschedule in programs/mythbackend/scheduler.cpp
     */
    "RESCHEDULE_RECORDINGS" -> verifyArgsNOP,

    /*
     * SCAN_VIDEOS
     *  @responds always
     *  @returns "OK" or "ERROR"
     */
    "SCAN_VIDEOS" -> verifyArgsEmpty,

    /*
     * SET_BOOKMARK %d %t %ld          <ChanId> <starttime> <frame#position>
     *  @responds sometimes; only if tokenCount == 4
     *  @returns "OK" or "FAILED"
     */
    "SET_BOOKMARK" -> verifyArgsNOP,

    /*
     * SET_CHANNEL_INFO [] [%d, %d, %d, %d, %d, %d, %d]
     *                     <ChanId> <sourceid> <oldcnum> <callsign> <channum> <channame> <xmltv>
     *  @responds always
     *  @returns "1" for successful otherwise "0"
     */
    "SET_CHANNEL_INFO" -> verifyArgsNOP,

    /*
     * SET_NEXT_LIVETV_DIR %d %s  <encoder#> <dir>
     *  @responds sometimes; only if tokenCount == 3
     *  @returns "OK or "bad" if encoder nor found
     */
    "SET_NEXT_LIVETV_DIR" -> verifyArgsNOP,

    /*
     * SET_SETTING %s %s %s       <hostname> <settingname> <value>
     *  @responds sometimes; only if tokenCount == 4
     *  @returns "OK" or "ERROR"
     */
    "SET_SETTING" -> verifyArgsSetSetting,

    /*
     * SHUTDOWN_NOW { %s }        { <haltCommand> }
     *  @responds never
     *  @returns nothing
     */
    "SHUTDOWN_NOW" -> verifyArgsShutdownNow,

    /*
     * STOP_RECORDING [] [<ProgramInfo>]
     *  @responds sometimes; only if recording is found
     *  @returns "0" if recording was on a slave backend
     *           "%d" if recording was on a local encoder, <recnum>
     *        or "-1" if not found
     */
    "STOP_RECORDING" -> verifyArgsProgramInfo,

    /*
     * UNDELETE_RECORDING [] [%d, %mt]       [<ChanId> <starttime>]
     * UNDELETE_RECORDING [] [%p]            [<ProgramInfo>]
     * NB starttime is in myth/ISO string format rather than timestamp
     *  @responds sometimes; if program info has ChanId
     *  @returns "0" on success and "-1" on error
     */
    "UNDELETE_RECORDING" -> verifyArgsPIorChanIdStart
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
  final val SPLIT_PATTERN: String = Pattern.quote(BACKEND_SEP)

  def verify(command: String, args: Any*): Boolean = {
    if (commands contains command) {
      val check = commands(command)
      check(args)
    } else {
      println(s"invalid command $command")
      false
    }
  }
}

private[myth] trait MythProtocol77 extends MythProtocol {
//  final val PROTO_VERSION = 77        // "75"
//  final val PROTO_TOKEN = "WindMark"  // "SweetRock"

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
  def queryBookmark(chanId: ChanId, startTime: MythDateTime): Long   // frame number/position
  def queryCommBreak(chanId: ChanId, startTime: MythDateTime): Long  // frame number/position
  def queryCutList(chanId: ChanId, startTime: MythDateTime): Long    // frame number/position
  def queryFileExists(fileName: String, storageGroup: String): (String, FileStats)
  def queryFileHash(fileName: String, storageGroup: String, hostName: String = ""): String
  def queryFreeSpace: FreeSpace
  def queryFreeSpaceList: Any  // ?
  def queryFreeSpaceSummary: Any // ?
  def queryGetAllPending: Iterable[Recording]  // TODO expected count iterator?
  def queryGetAllScheduled: Iterable[Recording]
  def queryGetConflicting: Iterable[Recording]
  def queryGetExpiring: Iterable[Recording]
  def queryGuideDataThrough: LocalDate  // or Instant?
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
  def setBookmark(chanId: ChanId, startTime: MythDateTime, pos: Long): Boolean
  def setSetting(hostName: String, settingName: String, value: String): Boolean
  def shutdownNow(haltCommand: String = ""): Unit
  def stopRecording(rec: Recording): Int  // TODO better encapsulate return codes
  def undeleteRecording(rec: Recording): Boolean
  def undeleteRecording(chanId: ChanId, startTime: MythDateTime): Boolean
  // TODO more methods
}
