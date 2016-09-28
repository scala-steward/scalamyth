package mythtv
package connection
package myth

trait MythProtocol {
  // TODO move constants to a companion object?
  // TODO can we structure this so that it's possible to support more than one protocol version?
  final val PROTO_VERSION = 77        // "75"
  final val PROTO_TOKEN = "WindMark"  // "SweetRock"
  final val BACKEND_SEP = "[]:[]"

  /**
    * Myth protocol commands: (from .../programs/mythbackend/mainserver.cpp)
    *
    * ALLOW_SHUTDOWN
    * ANN FileTransfer %s %d <%d %d> [] [%s, %s]
    * ANN {Monitor|Playback} %s %d
    * BACKEND_MESSAGE [] [%s ... ]
    * BLOCK_SHUTDOWN
    * CHECK_RECORDING [] [<ProgramInfo>]
    * DELETE_FILE [] [%s, %s]   <filename> <storage group name>
    * DELETE_RECORDING %d %d { FORCE { FORGET }}  <chanid> <starttime> { can we specify NOFORCE or NOFORGET? }
    * DELETE_RECORDING [] [<ProgramInfo>]
    * DONE
    * DOWNLOAD_FILE [] [%s, %s, %s]       <srcURL> <storageGroup> <fileName>
    * DOWNLOAD_FILE_NOW [] [%s, %s, %s]   <srcURL> <storageGroup> <fileName>  (this one sets synchronous = true)
    * FILL_PROGRAM_INFO [] [%s, <ProgramInfo>]     <playback host> <programinfo>
    * FORCE_DELETE_RECORDING [] [<ProgramInfo>]
    * FORGET_RECORDING [] [<ProgramInfo>]
    * FREE_TUNER %d        <cardId>
    * GET_FREE_RECORDER
    * GET_FREE_RECORDER_COUNT
    * GET_FREE_RECORDER_LIST
    * GET_NEXT_FREE_RECORDER [] [%d]  <currentRecorder#>
    * GET_RECORDER_FROM_NUM [] [%d]   <recorder#>
    * GET_RECORDER_NUM [] [<ProgramInfo>]
    * GO_TO_SLEEP                  (only for slaves, but no checking?! Looks @ CoreContext "SleepCommand" setting)
    * LOCK_TUNER                   (implicitly passes -1 as tuner id, what does this accomplish? first available local tuner?)
    * LOCK_TUNER %d  <cardId>
    * MESSAGE [] %s    // TODO flesh out
    * MYTH_PROTO_VERSION %s %s    <version> <protocolToken>
    * QUERY_ACTIVE_BACKENDS        --> count SEP hostname(s)
    * QUERY_BOOKMARK %d %d            <chanid> <starttime>  NB starttime is timestamp format
    * QUERY_CHECKFILE [] [%d, <ProgramInfo>]     <bool:checkSlaves> <programInfo>
    * QUERY_COMMBREAK %d %d           <chanid> <starttime>  NB starttime is timestamp format
    * QUERY_CUTLIST %d %d             <chanid> <starttime>  NB starttime is timestamp format
    * QUERY_FILE_EXISTS [] [%s, %s]   <filename> <storageGroup>
    * QUERY_FILE_HASH [] [%s, %s {, %s}]     <filename> <storageGroup> {<hostname>}
    * QUERY_FILETRANSFER [%d, DONE]                 // TODO flesh out all QUERY_FILETRANSFER...
    * QUERY_FILETRANSFER [%d, REQUEST_BLOCK, %d]
    * QUERY_FILETRANSFER [%d, WRITE_BLOCK, %d]
    * QUERY_FILETRANSFER [%d, SEEK, %d, %d, %d]
    * QUERY_FREE_SPACE
    * QUERY_FREE_SPACE_LIST
    * QUERY_FREE_SPACE_SUMMARY
    * QUERY_GENPIXMAP2 [] [%s, <ProgramInfo>, more?]     %s is a "token", can be the literal "do_not_care"
    * QUERY_GETALLPENDING
    * QUERY_GETALLSCHEDULED
    * QUERY_GETCONFLICTING [] [<ProgramInfo>]
    * QUERY_GETEXPIRING
    * QUERY_GUIDEDATATHROUGH
    * QUERY_HOSTNAME
    * QUERY_IS_ACTIVE_BACKEND [] [%s]   <hostname>
    * QUERY_ISRECORDING
    * QUERY_LOAD
    * QUERY_MEMSTATS
    * QUERY_PIXMAP_GET_IF_MODIFIED [] [%d, %d, <ProgramInfo>]  <time:cachemodified> <maxFileSize>
    * QUERY_PIXMAP_LASTMODIFIED [] [<ProgramInfo>]
    * QUERY_RECORDER %d          <recorder#>               // NB two tokens!
    *                   [ GET_CURRENT_RECORDING | IS_RECORDING | GET_FRAMERATE | GET_FRAMES_WRITTEN | GET_FILE_POSITION
    *                   | GET_MAX_BITRATE | GET_KEYFRAME_POS %lld | FILL_POSITION_MAP %lld %lld | FILL_DURATION_MAP %lld %lld
    *                   | GET_RECORDING | FRONTEND_READY | CANCEL_NEXT_RECORDING %s | SPAWN_LIVETV %s %d %s
    *                   | STOP_LIVETV | PAUSE | FINISH_RECORDING | SET_LIVE_RECORDING %d | GET_FREE_INPUTS %d ...
    *                   | GET_INPUT | SET_INPUT %s | TOGGLE_CHANNEL_FAVORITE %s | CHANGE_CHANNEL %d | SET_CHANNEL %s
    *                   | SET_SIGNAL_MONITORING_RATE %d %d | GET_COLOUR | GET_CONTRAST | GET_BRIGHTNESS | GET_HUE
    *                   | CHANGE_COLOUR %d %d | CHANGE_CONTRAST %d %d | CHANGE_BRIGHTNESS %d %d | CHANGE_HUE %d %d
    *                   | CHECK_CHANNEL %s | SHOULD_SWITCH_CARD %s | CHECK_CHANNEL_PREFIX %s | GET_NEXT_PROGRAM_INFO %s %d %d %s
    *                   | GET_CHANNEL_INFO %d ]
    * QUERY_RECORDING BASENAME %s                  <pathname>
    * QUERY_RECORDING TIMESLOT %d %s               <chanid> <startTime>
    * QUERY_RECORDING_DEVICE                          // not implemented?
    * QUERY_RECORDING_DEVICES                         // not implemented?
    * QUERY_RECORDINGS { Ascending | Descending | Unsorted | Recording }
    * QUERY_REMOTEENCODER %d {   GET_STATE | GET_SLEEPSTATUS | GET_FLAGS | IS_BUSY <params> | MATCHES_RECORDING <params>
    *                          | START_RECORDING <params> | GET_RECORDING_STATUS | RECORD_PENDING <params>
                               | CANCEL_NEXT_RECORDING <params> | STOP_RECORDING | GET_MAX_BITRATE | GET_CURRENT_RECORDING
                               | GET_FREE_INPUTS <params> }
    * QUERY_SETTING %s %s                              (hostname, settingName)
    * QUERY_SG_GETFILELIST [] [%s, %s, %s {, %s}]  <wantHost> <groupname> <path> { <bool:fileNamesOnly> }
    * QUERY_SG_FILEQUERY [] [%s, %s, %s]           <wantHost> <groupName> <filename>
    * QUERY_TIME_ZONE
    * QUERY_UPTIME
    * REFRESH_BACKEND
    * RESCHEDULE_RECORDINGS [] [CHECK %d %d %d {Python}, '', '', '', {**any**}]  // TODO look @ Scheduler::HandleReschedule in programs/mythbackend/scheduler.cpp
    * RESCHEDULE_RECORDINGS [] [MATCH %d %d %d {- Python}]
    * SCAN_VIDEOS
    * SET_BOOKMARK %d %d %lld          <chanid> <starttime> <position> (64-bits, frame number)
    * SET_CHANNEL_INFO [] [%d, %d, %d, %d, %d, %d, %d] <chanid> <sourceid> <oldcnum> <callsign> <channum> <channame> <xmltv>
    * SET_NEXT_LIVETV_DIR %d %s  <encoder#> <dir>
    * SET_SETTING %s %s %s       <hostname> <settingname> <value>
    * SHUTDOWN_NOW { %s }        { <haltCommand> }
    * STOP_RECORDING [] [<ProgramInfo>]
    * UNDELETE_RECORDING [] [%d, %s] or [<ProgramInfo>]   <chanid> <starttime> OR [<ProgramInfo>]
    */

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
