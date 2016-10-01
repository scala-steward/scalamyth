package mythtv
package connection
package myth

import java.util.regex.Pattern

trait MythProtocol {
  // TODO move constants to a companion object?
  // TODO can we structure this so that it's possible to support more than one protocol version?
  final val PROTO_VERSION = 77        // "75"
  final val PROTO_TOKEN = "WindMark"  // "SweetRock"
  final val BACKEND_SEP = "[]:[]"

  def commands: Map[String, Any] = internalMap

  /**
    * Myth protocol commands: (from programs/mythbackend/mainserver.cpp)
    */
  private val internalMap = Map(
    /*
     * ALLOW_SHUTDOWN
     *  @responds sometime; only if tokenCount == 1
     *  @returns "OK"
     */
    "ALLOW_SHUTDOWN" -> Nil,

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
    "ANN" -> Nil,

    /*
     * BACKEND_MESSAGE [] [%s {, %s}* ]   [<message> <extra...>]
     *  @responds never
     *  @returns nothing
     */
    "BACKEND_MESSAGE" -> Nil,

    /*
     * BLOCK_SHUTDOWN
     *  responds sometimes; only if tokenCount == 1
     *  @returns "OK"
     */
    "BLOCK_SHUTDOWN" -> Nil,

    /*
     * CHECK_RECORDING [] [%p]     [<ProgramInfo>]
     *  @responds always
     *  @returns TODO, # result from elink->MatchesRecording()
     */
    "CHECK_RECORDING" -> Nil,

    /*
     * DELETE_FILE [] [%s, %s]   [<filename> <storage group name>]
     *  @responds sometime; only if slistCount >= 3
     *  @returns Boolean "0" on error, "1" on succesful file deletion
     */
    "DELETE_FILE" -> Nil,

    /*
     * DELETE_RECORDING %d %t { FORCE { FORGET }}  <ChanId> <starttime> { can we specify NOFORCE or NOFORGET? }
     * DELETE_RECORDING [] [%p]   [<ProgramInfo>]
     *  @responds sometimes; only if ChanId in program info
     *  @returns Int result code:
     *     0 Successful (expiration only?)
     *    -1 Unspecified error? Or deletion pending in background?
     *    -2 Error deleting file
     *  TODO needs more investigation
     */
    "DELETE_RECORDING" -> Nil,

    /*
     * DONE
     *  @responds never
     *  @returns nothing, closes the client's socket
     */
    "DONE" -> Nil,

    /*
     * DOWNLOAD_FILE [] [%s, %s, %s]       [<srcURL> <storageGroup> <fileName>]
     *  @responds sometimes; only if slistCount == 4
     *  @returns result token:
     *       downloadfile_directory_not_found
     *       downloadfile_filename_dangerous
     *       OK <storagegroup> <filename>      ??
     *       ERROR                             ?? only if synchronous?
     */
    "DOWNLOAD_FILE" -> Nil,

    /*
     * DOWNLOAD_FILE_NOW [] [%s, %s, %s]   [<srcURL> <storageGroup> <fileName>]
     *   (this command sets synchronous = true as opposed to DOWNLOAD_FILE)
     *  @responds sometimes; only if slistCount == 4
     *  @returns see DOWNLOAD_FILE
     */
    "DOWNLOAD_FILE_NOW" -> Nil,

    /*
     * FILL_PROGRAM_INFO [] [%s, %p]     [<playback host> <ProgramInfo>]
     *  @responds always
     *  @returns ProgramInfo structure, populated
     *           (if already contained pathname, otherwise unchanged)
     */
    "FILL_PROGRAM_INFO" -> Nil,

    /*
     * FORCE_DELETE_RECORDING [] [%p]   [<ProgramInfo>]
     *  @responds sometimes; only if ChanId in program info
     *  @returns see DELETE_RECORDING
     */
    "FORCE_DELETE_RECORDING" -> Nil,

    /*
     * FORGET_RECORDING [] [%p]    [<ProgramInfo>]
     *  @responds always
     *  @returns "0"
     */
    "FORGET_RECORDING" -> Nil,

    /*
     * FREE_TUNER %d        <cardId>
     *  @responds sometimes; only if tokens == 2
     *  @returns "OK" or "FAILED"
     */
    "FREE_TUNER" -> Nil,

    /*
     * GET_FREE_RECORDER
     *  @responds always
     *  @returns [%d, %s, %d] = <best free encoder id> <host or IP> <port>
     *        or [-1, "nohost", -1] if no suitable encoder found
     */
    "GET_FREE_RECORDER" -> Nil,

    /*
     * GET_FREE_RECORDER_COUNT
     *  @responds always
     *  @returns Int: number of available encoders
     */
    "GET_FREE_RECORDER_COUNT" -> Nil,

    /*
     * GET_FREE_RECORDER_LIST
     *  @responds always
     *  @returns [%d, {, %d}] = list of available encoder ids, or "0" if none
     */
    "GET_FREE_RECORDER_LIST" -> Nil,

    /*
     * GET_NEXT_FREE_RECORDER [] [%d]  [<currentRecorder#>]
     *  @responds always
     *  @returns [%d, %s, %d] = <next free encoder id> <host or IP> <port>
     *        or [-1, "nohost", -1] if no suitable encoder found
     */
    "GET_NEXT_FREE_RECORDER" -> Nil,

    /*
     * GET_RECORDER_FROM_NUM [] [%d]   [<recorder#>]
     *  @responds always
     *  @returns [%s, %d] = <host or IP> <port>
     *        or ["nohost", -1] if no matching recorder found
     */
    "GET_RECORDER_FROM_NUM" -> Nil,

    /*
     * GET_RECORDER_NUM [] [%p]        [<ProgramInfo>]
     *  @responds always
     *  @returns [%s, %d] = <host or IP> <port>
     *        or ["nohost", -1] if no matching recorder found
     */
    "GET_RECORDER_NUM" -> Nil,

    /*
     * GO_TO_SLEEP
     *  @responds always
     *  @returns "OK" or "ERROR: SleepCommand is empty"
     * (only for slaves, but no checking?! Looks @ CoreContext "SleepCommand" setting)
     */
    "GO_TO_SLEEP" -> Nil,

    /*
     * LOCK_TUNER  (implicitly passes -1 as tuner id, what does this accomplish? first available local tuner?)
     * LOCK_TUNER %d  <cardId>
     *  @responds sometimes; only if tokenCount in { 1, 2 }
     *  @returns [%d, %s, %s, %s]  <cardid> <videodevice> <audiodevice> <vbidevice> (from capturecard table)
     *       or  [-2, "", "", ""]  if tuner is already locked
     *       or  [-1, "", "", ""]  if no tuner found to lock
     */
    "LOCK_TUNER" -> Nil,

    /*
     * MESSAGE [] [ %s {, %s }* ]        [<message> <extra...>]
     * MESSAGE [] [ SET_VERBOSE %s ]     [<verboseMask>]
     * MESSAGE [] [ SET_LOG_LEVEL %s ]   [<logLevel>]
     *  @responds sometimes; if SET_xxx then always, otherwise if slistCount >= 2
     *  @returns          "OK"
     *     SET_VERBOSE:   "OK" or "Failed"
     *     SET_LOG_LEVEL: "OK" or "Failed"
     */
    "MESSAGE" -> Nil,

    /*
     * MYTH_PROTO_VERSION %s %s    <version> <protocolToken>
     *  @responds sometimes; only if tokenCount >= 2
     *  @returns "REJECT %d" or "ACCEPT %d" where %d is MYTH_PROTO_VERSION
     */
    "MYTH_PROTO_VERSION" -> Nil,

    /*
     * QUERY_ACTIVE_BACKENDS
     *  @responds always
     *  @returns %d [] [ %s {, %s }* ]  <count> [ hostName, ... ]
     */
    "QUERY_ACTIVE_BACKENDS" -> Nil,

    /*
     * QUERY_BOOKMARK %d %t   <ChanId> <starttime>
     *  @responds sometimes, only if tokenCount == 3
     *  @returns %ld   <bookmarkPos> (frame number)
     */
    "QUERY_BOOKMARK" -> Nil,

    /*
     * QUERY_CHECKFILE [] [%b, %p]     <checkSlaves> <ProgramInfo>
     *  @responds always
     *  @returns %d %s      <exists:0/1?>  <playbackURL>
     *    note playback url will be "" if file does not exist
     */
    "QUERY_CHECKFILE" -> Nil,

    /*
     * QUERY_COMMBREAK %d %t           <ChanId> <starttime>
     *  @responds sometimes; only if tokenCount == 3
     *  @returns TODO some sort of IntList?
     */
    "QUERY_COMMBREAK" -> Nil,

    /*
     * QUERY_CUTLIST %d %t             <ChanId> <starttime>
     *  @responds sometimes; only if tokenCount == 3
     *  @returns TODO some sort of IntList?
     */
    "QUERY_CUTLIST" -> Nil,

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
    "QUERY_FILE_EXISTS" -> Nil,

    /*
     * QUERY_FILE_HASH [] [%s, %s {, %s}]     <filename> <storageGroup> {<hostname>}
     *  @responds sometimes; only if slistCount >= 3
     *  @returns
     *      ""  on error checking for file, invalid input
     *      %s  hash of the file (currently 64-bit, so 16 hex characters)
     */
    "QUERY_FILE_HASH" -> Nil,

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
    "QUERY_FILETRANSFER" -> Nil,

    /*
     * QUERY_FREE_SPACE
     *  @responds always
     *  @returns  TODO
     */
    "QUERY_FREE_SPACE" -> Nil,

    /*
     * QUERY_FREE_SPACE_LIST
     *  @responds always
     *  @returns TODO
     *
     * Like QUERY_FREE_SPACE but returns free space on all hosts, each directory
     * is reported as a URL, and a TotalDiskSpace is appended.
     */
    "QUERY_FREE_SPACE_LIST" -> Nil,

    /*
     * QUERY_FREE_SPACE_SUMMARY
     *  @responds always
     *  @returns [%d, %d]    <total size> <used size>  sizes are in kB (1024-byte blocks)
     *        or [ 0, 0 ]    if there was any sort of error
     */
    "QUERY_FREE_SPACE_SUMMARY" -> Nil,

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
    "QUERY_GENPIXMAP2" -> Nil,

    /*
     * QUERY_GETALLPENDING { %s {, %d}}  { <tmptable> {, <recordid>}}
     *  @responds always
     *  @returns ? [%p {, %p}]  <list of ProgramInfo>  // TODO does this begin with a count?
     *        or ["0", "0"] if not availble/error?
     *  TODO what is the purpose of the optional tmptable and recordid parameters?
     */
    "QUERY_GETALLPENDING" -> Nil,

    /*
     * QUERY_GETALLSCHEDULED
     *  @responds always
     *  @returns ? [%p {, %p}]  <list of ProgramInfo>  // TODO does this begin with a count?
     *        or "0" if not availble/error?
     */
    "QUERY_GETALLSCHEDULED" -> Nil,

    /*
     * QUERY_GETCONFLICTING [] [%p]     [<ProgramInfo>]
     *  @responds always
     *  @returns ? [%p {, %p}]  <list of ProgramInfo>  // TODO does this begin with a count?
     *        or "0" if not availble/error?
     */
    "QUERY_GETCONFLICTING" -> Nil,

    /*
     * QUERY_GETEXPIRING
     *  @responds always
     *  @returns ? [%p {, %p}]  <list of ProgramInfo>  // TODO does this begin with a count?
     *        or "0" if not availble/error?
     */
    "QUERY_GETEXPIRING" -> Nil,

    /*
     * QUERY_GUIDEDATATHROUGH
     *  @responds always
     *  @returns: Date/Time as a string in YYYY-MM-DD hh:mm" format
     *         or 0000-00-00 00:00 in case of error or no data
     */
    "QUERY_GUIDEDATATHROUGH" -> Nil,

    /*
     * QUERY_HOSTNAME
     *  @responds always
     *  @returns %s  <hostname>
     */
    "QUERY_HOSTNAME" -> Nil,

    /*
     * QUERY_IS_ACTIVE_BACKEND [] [%s]   [<hostname>]
     *  @responds sometimes; only if tokenCount == 1
     *  @returns "TRUE" or "FALSE"
     * TODO may case NPE if hostname is not passed?
     *      what does QtStringList array index out of bounds do?
     */
    "QUERY_IS_ACTIVE_BACKEND" -> Nil,

    /*
     * QUERY_ISRECORDING
     *  @responds always
     *  @returns [%d, %d]  <numRecordingsInProgress> <numLiveTVinProgress>
     *                           (liveTV is a subset of recordings)
     */
    "QUERY_ISRECORDING" -> Nil,

    /*
     * QUERY_LOAD
     *  @responds always
     *  @returns [%f, %f, %f]   <1-min> <5-min> <15-min> load averages
     *        or ["ERROR", "getloadavg() failed"] in case of error
     */
    "QUERY_LOAD" -> Nil,

    /*
     * QUERY_MEMSTATS
     *  @responds always
     *  @returns [%d, %d, %d, %d]  <totalMB> <freeMB> <totalVM> <freeVM>
     *        or ["ERROR", "Could not determine memory stats."] on error
     */
    "QUERY_MEMSTATS" -> Nil,

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
    "QUERY_PIXMAP_GET_IF_MODIFIED" -> Nil,

    /*
     * QUERY_PIXMAP_LASTMODIFIED [] [%p]      [<ProgramInfo>]
     *  @responds
     *  @returns %ld    <last modified (timestamp?)>
     *        or "BAD"
     */
    "QUERY_PIXMAP_LASTMODIFIED" -> Nil,

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
    "QUERY_RECORDER" -> Nil,

    /*
     * QUERY_RECORDING BASENAME %s                  <pathname>
     * QUERY_RECORDING TIMESLOT %d %t               <ChanId> <starttime>
     */
    "QUERY_RECORDING" -> Nil,

    /*
     * QUERY_RECORDING_DEVICE
     *   not implemented on backend server
     */
    "QUERY_RECORDING_DEVICE" -> Nil,

    /*
     * QUERY_RECORDING_DEVICES
     *   not implemented on backend server
     */
    "QUERY_RECORDING_DEVICES" -> Nil,

    /*
     * QUERY_RECORDINGS { Ascending | Descending | Unsorted | Recording }
     *  @responds sometimes; only if tokenCount == 2
     *  @returns [ %p {, %p}*]   list of ProgramInfo records
     */
    "QUERY_RECORDINGS" -> Nil,

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
    "QUERY_REMOTEENCODER" -> Nil,

    /*
     * QUERY_SETTING %s %s      <hostname> <settingName>
     *  @responds sometimes; only if tokenCount == 3
     *  @returns %s or "-1" if not found   <settingValue>
     */
    "QUERY_SETTING" -> Nil,

    /* QUERY_SG_GETFILELIST [] [%s, %s, %s {, %b}]  <wantHost> <groupname> <path> { fileNamesOnly> } */
    "QUERY_SG_GETFILELIST" -> Nil,

    /* QUERY_SG_FILEQUERY [] [%s, %s, %s]           <wantHost> <groupName> <filename> */
    "QUERY_SG_FILEQUERY" -> Nil,

    /*
     * QUERY_TIME_ZONE
     *  @responds always
     *  @returns [%s, %d, %s]  <timezoneName> <offsetSecsFromUtc> <currentTimeUTC>
     *    currentTimeUTC is in the ISO format YYYY-MM-ddThh:mm:ssZ
     */
    "QUERY_TIME_ZONE" -> Nil,

    /*
     * QUERY_UPTIME
     *  @responds always
     *  @returns %ld  <uptimeSeconds>
     *        or ["ERROR", "Could not determine uptime."] in case of error
     */
    "QUERY_UPTIME" -> Nil,

    /*
     * REFRESH_BACKEND
     *  @responds always
     *  @returns "OK"
     *  Seems to be a NOP on the server.
     */
    "REFRESH_BACKEND" -> Nil,

    /*
     * RESCHEDULE_RECORDINGS [] [CHECK %d %d %d {Python}, '', '', '', {**any**}]
     * RESCHEDULE_RECORDINGS [] [MATCH %d %d %d {- Python}]
     *   TODO look @ Scheduler::HandleReschedule in programs/mythbackend/scheduler.cpp
     */
    "RESCHEDULE_RECORDINGS" -> Nil,

    /*
     * SCAN_VIDEOS
     *  @responds always
     *  @returns "OK" or "ERROR"
     */
    "SCAN_VIDEOS" -> Nil,

    /*
     * SET_BOOKMARK %d %t %ld          <ChanId> <starttime> <frame#position>
     *  @responds sometimes; only if tokenCount == 4
     *  @returns "OK" or "FAILED"
     */
    "SET_BOOKMARK" -> Nil,

    /*
     * SET_CHANNEL_INFO [] [%d, %d, %d, %d, %d, %d, %d]
     *                     <ChanId> <sourceid> <oldcnum> <callsign> <channum> <channame> <xmltv>
     *  @responds always
     *  @returns "1" for successful otherwise "0"
     */
    "SET_CHANNEL_INFO" -> Nil,

    /*
     * SET_NEXT_LIVETV_DIR %d %s  <encoder#> <dir>
     *  @responds sometimes; only if tokenCount == 3
     *  @returns "OK or "bad" if encoder nor found
     */
    "SET_NEXT_LIVETV_DIR" -> Nil,

    /*
     * SET_SETTING %s %s %s       <hostname> <settingname> <value>
     *  @responds sometimes; only if tokenCount == 4
     *  @returns "OK" or "ERROR"
     */
    "SET_SETTING" -> Nil,

    /*
     * SHUTDOWN_NOW { %s }        { <haltCommand> }
     *  @responds never
     *  @returns nothing
     */
    "SHUTDOWN_NOW" -> Nil,

    /*
     * STOP_RECORDING [] [<ProgramInfo>]
     *  @responds sometimes; only if recording is found
     *  @returns "0" if recording was on a slave backend
     *           "%d" if recording was on a local encoder, <recnum>
     *        or "-1" if not found
     */
    "STOP_RECORDING" -> Nil,

    /*
     * UNDELETE_RECORDING [] [%d, %t]        [<ChanId> <starttime>]
     * UNDELETE_RECORDING [] [%p]            [<ProgramInfo>]
     *  @responds sometimes; if program info has ChanId
     *  @returns "0" on success and "-1" on error
     */
    "UNDELETE_RECORDING" -> Nil
  )


  /**
   * Data types that need serialization to send over the wire:
    *  Program      --> <ProgramInfo>
    *  MythDateTime --> epoch timestamp
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
}

private[myth] trait MythProtocol77 extends MythProtocol {
//  final val PROTO_VERSION = 77        // "75"
//  final val PROTO_TOKEN = "WindMark"  // "SweetRock"
}
