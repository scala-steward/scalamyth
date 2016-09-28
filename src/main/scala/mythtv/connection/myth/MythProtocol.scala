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
    /* ALLOW_SHUTDOWN */
    "ALLOW_SHUTDOWN" -> Nil,

    /* ANN FileTransfer %s %d <%d %d> [] [%s, %s]
     * ANN {Monitor|Playback} %s %d */
    "ANN" -> Nil,

    /* BACKEND_MESSAGE [] [%s ... ] */
    "BACKEND_MESSAGE" -> Nil,

    /* BLOCK_SHUTDOWN */
    "BLOCK_SHUTDOWN" -> Nil,

    /* CHECK_RECORDING [] [<ProgramInfo>] */
    "CHECK_RECORDING" -> Nil,

    /* DELETE_FILE [] [%s, %s]   <filename> <storage group name> */
    "DELETE_FILE" -> Nil,

    /* DELETE_RECORDING %d %d { FORCE { FORGET }}  <chanid> <starttime> { can we specify NOFORCE or NOFORGET? }
     * DELETE_RECORDING [] [<ProgramInfo>] */
    "DELETE_RECORDING" -> Nil,

    /* DONE */
    "DONE" -> Nil,

    /* DOWNLOAD_FILE [] [%s, %s, %s]       <srcURL> <storageGroup> <fileName> */
    "DOWNLOAD_FILE" -> Nil,

    /* DOWNLOAD_FILE_NOW [] [%s, %s, %s]   <srcURL> <storageGroup> <fileName>  (this one sets synchronous = true) */
    "DOWNLOAD_FILE_NOW" -> Nil,

    /* FILL_PROGRAM_INFO [] [%s, <ProgramInfo>]     <playback host> <programinfo> */
    "FILL_PROGRAM_INFO" -> Nil,

    /* FORCE_DELETE_RECORDING [] [<ProgramInfo>] */
    "FORCE_DELETE_RECORDING" -> Nil,

    /* FORGET_RECORDING [] [<ProgramInfo>] */
    "FORGET_RECORDING" -> Nil,

    /* FREE_TUNER %d        <cardId> */
    "FREE_TUNER" -> Nil,

    /* GET_FREE_RECORDER */
    "GET_FREE_RECORDER" -> Nil,

    /* GET_FREE_RECORDER_COUNT */
    "GET_FREE_RECORDER_COUNT" -> Nil,

    /* GET_FREE_RECORDER_LIST */
    "GET_FREE_RECORDER_LIST" -> Nil,

    /* GET_NEXT_FREE_RECORDER [] [%d]  <currentRecorder#> */
    "GET_NEXT_FREE_RECORDER" -> Nil,

    /* GET_RECORDER_FROM_NUM [] [%d]   <recorder#> */
    "GET_RECORDER_FROM_NUM" -> Nil,

    /* GET_RECORDER_NUM [] [<ProgramInfo>] */
    "GET_RECORDER_NUM" -> Nil,

    /* GO_TO_SLEEP                  (only for slaves, but no checking?! Looks @ CoreContext "SleepCommand" setting) */
    "GO_TO_SLEEP" -> Nil,

    /* LOCK_TUNER                   (implicitly passes -1 as tuner id, what does this accomplish? first available local tuner?)
     * LOCK_TUNER %d  <cardId> */
    "LOCK_TUNER" -> Nil,

    /* MESSAGE [] %s    // TODO flesh out */
    "MESSAGE" -> Nil,

    /* MYTH_PROTO_VERSION %s %s    <version> <protocolToken> */
    "MYTH_PROTO_VERSION" -> Nil,

    /* QUERY_ACTIVE_BACKENDS        --> count SEP hostname(s) */
    "QUERY_ACTIVE_BACKENDS" -> Nil,

    /* QUERY_BOOKMARK %d %d            <chanid> <starttime>  NB starttime is timestamp format */
    "QUERY_BOOKMARK" -> Nil,

    /* QUERY_CHECKFILE [] [%d, <ProgramInfo>]     <bool:checkSlaves> <programInfo> */
    "QUERY_CHECKFILE" -> Nil,

    /* QUERY_COMMBREAK %d %d           <chanid> <starttime>  NB starttime is timestamp format */
    "QUERY_COMMBREAK" -> Nil,

    /* QUERY_CUTLIST %d %d             <chanid> <starttime>  NB starttime is timestamp format */
    "QUERY_CUTLIST" -> Nil,

    /* QUERY_FILE_EXISTS [] [%s, %s]   <filename> <storageGroup> */
    "QUERY_FILE_EXISTS" -> Nil,

    /* QUERY_FILE_HASH [] [%s, %s {, %s}]     <filename> <storageGroup> {<hostname>} */
    "QUERY_FILE_HASH" -> Nil,

    /*
     * QUERY_FILETRANSFER [%d, DONE]                 // TODO flesh out all QUERY_FILETRANSFER...
     * QUERY_FILETRANSFER [%d, REQUEST_BLOCK, %d]
     * QUERY_FILETRANSFER [%d, WRITE_BLOCK, %d]
     * QUERY_FILETRANSFER [%d, SEEK, %d, %d, %d]
     */
    "QUERY_FILETRANSFER" -> Nil,

    /* QUERY_FREE_SPACE */
    "QUERY_FREE_SPACE" -> Nil,

    /* QUERY_FREE_SPACE_LIST */
    "QUERY_FREE_SPACE_LIST" -> Nil,

    /* QUERY_FREE_SPACE_SUMMARY */
    "QUERY_FREE_SPACE_SUMMARY" -> Nil,

    /* QUERY_GENPIXMAP2 [] [%s, <ProgramInfo>, more?]     %s is a "token", can be the literal "do_not_care" */
    "QUERY_GENPIXMAP2" -> Nil,

    /* QUERY_GETALLPENDING */
    "QUERY_GETALLPENDING" -> Nil,

    /* QUERY_GETALLSCHEDULED */
    "QUERY_GETALLSCHEDULED" -> Nil,

    /* QUERY_GETCONFLICTING [] [<ProgramInfo>] */
    "QUERY_GETCONFLICTING" -> Nil,

    /* QUERY_GETEXPIRING */
    "QUERY_GETEXPIRING" -> Nil,

    /* QUERY_GUIDEDATATHROUGH */
    "QUERY_GUIDEDATATHROUGH" -> Nil,

    /* QUERY_HOSTNAME */
    "QUERY_HOSTNAME" -> Nil,

    /* QUERY_IS_ACTIVE_BACKEND [] [%s]   <hostname> */
    "QUERY_IS_ACTIVE_BACKEND" -> Nil,

    /* QUERY_ISRECORDING */
    "QUERY_ISRECORDING" -> Nil,

    /* QUERY_LOAD */
    "QUERY_LOAD" -> Nil,

    /* QUERY_MEMSTATS */
    "QUERY_MEMSTATS" -> Nil,

    /* QUERY_PIXMAP_GET_IF_MODIFIED [] [%d, %d, <ProgramInfo>]  <time:cachemodified> <maxFileSize> */
    "QUERY_PIXMAP_GET_IF_MODIFIED" -> Nil,

    /* QUERY_PIXMAP_LASTMODIFIED [] [<ProgramInfo>] */
    "QUERY_PIXMAP_LASTMODIFIED" -> Nil,

    /*
     * QUERY_RECORDER %d  <recorder#> [    // NB two tokens! recorder# + subcommand list
     *     IS_RECORDING
     *   | GET_FRAMERATE
     *   | GET_FRAMES_WRITTEN
     *   | GET_FILE_POSITION
     *   | GET_MAX_BITRATE
     *   | GET_KEYFRAME_POS %lld
     *   | FILL_POSITION_MAP %lld %lld
     *   | FILL_DURATION_MAP %lld %lld
     *   | GET_CURRENT_RECORDING
     *   | GET_RECORDING
     *   | FRONTEND_READY
     *   | CANCEL_NEXT_RECORDING %s
     *   | SPAWN_LIVETV %s %d %s
     *   | STOP_LIVETV
     *   | PAUSE
     *   | FINISH_RECORDING
     *   | SET_LIVE_RECORDING %d
     *   | GET_FREE_INPUTS %d ...
     *   | GET_INPUT
     *   | SET_INPUT %s
     *   | TOGGLE_CHANNEL_FAVORITE %s
     *   | CHANGE_CHANNEL %d
     *   | SET_CHANNEL %s
     *   | SET_SIGNAL_MONITORING_RATE %d %d
     *   | GET_COLOUR
     *   | GET_CONTRAST
     *   | GET_BRIGHTNESS
     *   | GET_HUE
     *   | CHANGE_COLOUR %d %d
     *   | CHANGE_CONTRAST %d %d
     *   | CHANGE_BRIGHTNESS %d %d
     *   | CHANGE_HUE %d %d
     *   | CHECK_CHANNEL %s
     *   | SHOULD_SWITCH_CARD %s
     *   | CHECK_CHANNEL_PREFIX %s
     *   | GET_NEXT_PROGRAM_INFO %s %d %d %s
     *   | GET_CHANNEL_INFO %d
     * ]
     */
    "QUERY_RECORDER" -> Nil,

    /*
     * QUERY_RECORDING BASENAME %s                  <pathname>
     * QUERY_RECORDING TIMESLOT %d %s               <chanid> <startTime>
     */
    "QUERY_RECORDING" -> Nil,

    /* QUERY_RECORDING_DEVICE                          // not implemented? */
    "QUERY_RECORDING_DEVICE" -> Nil,

    /* QUERY_RECORDING_DEVICES                         // not implemented? */
    "QUERY_RECORDING_DEVICES" -> Nil,

    /* QUERY_RECORDINGS { Ascending | Descending | Unsorted | Recording } */
    "QUERY_RECORDINGS" -> Nil,

    /*
     * QUERY_REMOTEENCODER %d [
     *     GET_STATE
     *   | GET_SLEEPSTATUS
     *   | GET_FLAGS
     *   | IS_BUSY <params>
     *   | MATCHES_RECORDING <params>
     *   | START_RECORDING <params>
     *   | GET_RECORDING_STATUS
     *   | RECORD_PENDING <params>
     *   | CANCEL_NEXT_RECORDING <params>
     *   | STOP_RECORDING
     *   | GET_MAX_BITRATE
     *   | GET_CURRENT_RECORDING
     *   | GET_FREE_INPUTS <params>
     * ]
     */
    "QUERY_REMOTEENCODER" -> Nil,

    /* QUERY_SETTING %s %s                              (hostname, settingName) */
    "QUERY_SETTING" -> Nil,

    /* QUERY_SG_GETFILELIST [] [%s, %s, %s {, %s}]  <wantHost> <groupname> <path> { <bool:fileNamesOnly> } */
    "QUERY_SG_GETFILELIST" -> Nil,

    /* QUERY_SG_FILEQUERY [] [%s, %s, %s]           <wantHost> <groupName> <filename> */
    "QUERY_SG_FILEQUERY" -> Nil,

    /* QUERY_TIME_ZONE */
    "QUERY_TIME_ZONE" -> Nil,

    /* QUERY_UPTIME */
    "QUERY_UPTIME" -> Nil,

    /* REFRESH_BACKEND */
    "REFRESH_BACKEND" -> Nil,

    /*
     * RESCHEDULE_RECORDINGS [] [CHECK %d %d %d {Python}, '', '', '', {**any**}]
     * RESCHEDULE_RECORDINGS [] [MATCH %d %d %d {- Python}]
     *   TODO look @ Scheduler::HandleReschedule in programs/mythbackend/scheduler.cpp
     */
    "RESCHEDULE_RECORDINGS" -> Nil,

    /* SCAN_VIDEOS */
    "SCAN_VIDEOS" -> Nil,

    /* SET_BOOKMARK %d %d %lld          <chanid> <starttime> <position> (64-bits, frame number) */
    "SET_BOOKMARK" -> Nil,

    /* SET_CHANNEL_INFO [] [%d, %d, %d, %d, %d, %d, %d] <chanid> <sourceid> <oldcnum> <callsign> <channum> <channame> <xmltv> */
    "SET_CHANNEL_INFO" -> Nil,

    /* SET_NEXT_LIVETV_DIR %d %s  <encoder#> <dir> */
    "SET_NEXT_LIVETV_DIR" -> Nil,

    /* SET_SETTING %s %s %s       <hostname> <settingname> <value> */
    "SET_SETTING" -> Nil,

    /* SHUTDOWN_NOW { %s }        { <haltCommand> } */
    "SHUTDOWN_NOW" -> Nil,

    /* STOP_RECORDING [] [<ProgramInfo>] */
    "STOP_RECORDING" -> Nil,

    /* UNDELETE_RECORDING [] [%d, %s] or [<ProgramInfo>]   <chanid> <starttime> OR [<ProgramInfo>] */
    "UNDELETE_RECORDING" -> Nil
  )


  /**
   * Data types that need serialization to send over the wire:
    *  Program      --> <ProgramInfo>
    *  MythDateTime --> epoch timestamp
    *  String       --> string
    *  ChanId       --> integer string
    *  Int          --> integer string
    *  Boolean      --> is there a consistent serialization format?
    *  Frame #      --> long integer string (up to 64-bits worth)
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
