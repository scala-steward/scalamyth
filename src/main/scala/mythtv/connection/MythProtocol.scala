package mythtv
package connection

trait MythProtocol {
  final val PROTO_VERSION = 77        // "75"
  final val PROTO_TOKEN = "WindMark"  // "SweetRock"
  final val BACKEND_SEP = "[]:[]"

  /**
    * Myth protocol commands: (from .../programs/mythbackend/mainserver.cpp)
    *
    * ALLOW_SHUTDOWN
    * ANN FileTransfer %s %d <%d %d> [] [%s, %s]
    * ANN {Monitor|Playback} %s %d
    * BACKEND_MESSAGE ???
    * BLOCK_SHUTDOWN
    * CHECK_RECORDING ???
    * DELETE_FILE [] [%s, %s]
    * DELETE_RECORDING [] [%s]
    * DONE
    * DOWNLOAD_FILE [] [%s, %s, %s]
    * DOWNLOAD_FILE_NOW ???
    * FILL_PROGRAM_INFO ???
    * FORCE_DELETE_RECORDING [] [%s]
    * FORGET_RECORDING [] [%s]
    * FREE_TUNER %d
    * GET_FREE_RECORDER ???
    * GET_FREE_RECORDER_COUNT ???
    * GET_FREE_RECORDER_LIST
    * GET_NEXT_FREE_RECORDER ???
    * GET_RECORDER_FROM_NUM ???
    * GET_RECORDER_NUM ???
    * GO_TO_SLEEP ???
    * LOCK_TUNER %d
    * MESSAGE [] %s
    * MYTH_PROTO_VERSION %s %s
    * QUERY_ACTIVE_BACKENDS ???
    * QUERY_BOOKMARK ???
    * QUERY_CHECKFILE [] [%d, %s]
    * QUERY_COMMBREAK ???
    * QUERY_CUTLIST ???
    * QUERY_FILE_EXISTS [] [%s, %s]
    * QUERY_FILE_HASH [] [%s, %s]
    * QUERY_FILETRANSFER [%d, DONE]
    * QUERY_FILETRANSFER [%d, REQUEST_BLOCK, %d]
    * QUERY_FILETRANSFER [%d, WRITE_BLOCK, %d]
    * QUERY_FILETRANSFER [%d, SEEK, %d, %d, %d]
    * QUERY_FREE_SPACE
    * QUERY_FREE_SPACE_LIST
    * QUERY_FREE_SPACE_SUMMARY
    * QUERY_GENPIXMAP2 ???
    * QUERY_GETALLPENDING
    * QUERY_GETALLSCHEDULED
    * QUERY_GETCONFLICTING ???
    * QUERY_GETEXPIRING
    * QUERY_GUIDEDATATHROUGH
    * QUERY_HOSTNAME
    * QUERY_IS_ACTIVE_BACKEND [] [%s]
    * QUERY_ISRECORDING ???
    * QUERY_LOAD
    * QUERY_MEMSTATS ???
    * QUERY_PIXMAP_GET_IF_MODIFIED ???
    * QUERY_PIXMAP_LASTMODIFIED ???
    * QUERY_RECORDER [%d, GET_CURRENT_RECORDING]
    * QUERY_RECORDER [%d, IS_RECORDING]
    * QUERY_RECORDING TIMESLOT %d %s
    * QUERY_RECORDING_DEVICE ???
    * QUERY_RECORDING_DEVICES ???
    * QUERY_RECORDINGS { Ascending | Descending | Unsorted | Recording }
    * QUERY_REMOTEENCODER ???
    * QUERY_SETTING ???
    * QUERY_SG_GETFILELIST [] [%s, %s, %s, %s]
    * QUERY_SG_FILEQUERY [] [%s, %s, %s]
    * QUERY_TIME_ZONE ???
    * QUERY_UPTIME
    * REFRESH_BACKEND ???
    * RESCHEDULE_RECORDINGS [] [CHECK %d %d %d {Python}, '', '', '', {**any**}]
    * RESCHEDULE_RECORDINGS [] [MATCH %d %d %d {- Python}]
    * SCAN_VIDEOS
    * SET_BOOKMARK ???
    * SET_CHANNEL_INFO ???
    * SET_NEXT_LIVETV_DIR ???
    * SET_SETTING ???
    * SHUTDOWN_NOW ???
    * STOP_RECORDING [] [%s]
    * UNDELETE_RECORDING ???
    */

}
