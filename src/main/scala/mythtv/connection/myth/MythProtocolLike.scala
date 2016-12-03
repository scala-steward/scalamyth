package mythtv
package connection
package myth

import java.net.{ InetAddress, URI }
import java.time.{ Duration, Instant, ZoneOffset }

import scala.util.{ Try, Success, Failure }

import data._
import util._
import model._
import model.EnumTypes._
import EnumTypes.{ MythProtocolEventMode, SeekWhence }
import MythProtocol._
import MythProtocolFailure._

private[myth] trait MythProtocolLike extends MythProtocolSerializer {
  protected type SerializeRequest = (String, Seq[Any]) => String
  protected type HandleResponse = (BackendRequest, BackendResponse) => MythProtocolResult[_]

  protected def commands: Map[String, (SerializeRequest, HandleResponse)] = Map.empty

  def sendCommand(command: String, args: Any*): MythProtocolResult[_]

  private[myth] def postCommand(command: String, args: Any*): Unit

  def supports(command: String): Boolean = commands contains command

  def supports(command: String, args: Any*): Boolean = {
    if (commands contains command) {
      val (serialize, _) = commands(command)
      try {
        val _ = serialize(command, args)
        true
      } catch {
        case _: MythProtocolArgumentException => false
      }
    }
    else false
  }
}

final case class MythProtocolArgumentException(command: String, message: String)
    extends IllegalArgumentException("for " + command + ", expecting " + message) {
  def this(command: String) = this(command, "valid argument list")
}

object MythProtocolEventMode extends Enumeration {
  type MythProtocolEventMode = Value
  val None       = Value(0)
  val Normal     = Value(1)
  val NonSystem  = Value(2)
  val SystemOnly = Value(3)
}

private[myth] trait MythProtocolLikeRef extends MythProtocolLike {
  import MythProtocol.Separator

  override def commands = commandMap

  // override as necessary in versioned traits to get proper serialization
  protected implicit val programInfoSerializer     = ProgramInfoSerializerGeneric
  protected implicit val freeSpaceSerializer       = FreeSpaceSerializerGeneric
  protected implicit val cardInputSerializer       = CardInputSerializerGeneric
  protected implicit val channelSerializer         = ChannelSerializerGeneric
  protected implicit val upcomingProgramSerializer = UpcomingProgramSerializerGeneric

  /**
    * Myth protocol commands: (from programs/mythbackend/mainserver.cpp)
    */

  // Extra parens on every map entry right hand side to keep auto-tupling at bay
  private val commandMap = Map[String, (SerializeRequest, HandleResponse)](
    /*
     * ALLOW_SHUTDOWN
     *  @responds sometime; only if tokenCount == 1
     *  @returns "OK"
     */
    "ALLOW_SHUTDOWN" -> ((serializeEmpty, handleAllowShutdown)),

    /*
     * ANN Monitor %s %d                <clientHostName> <eventsMode>
     * ANN Playback %s %d               <clientHostName> <eventsMode>
     * ANN MediaServer %s               <hostName>
     * ANN SlaveBackend %s %s { %p }+   <slaveHostName> <slaveIPAddr> <ProgramInfo>+
     * ANN FileTransfer %s { %b { %b { %d }}} [%s %s %s {, %s}*]
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
    "ANN" -> ((serializeAnnounce, handleAnnounce)),

    /*
     * BACKEND_MESSAGE [] [%s {, %s}* ]   [<message> <extra...>]
     *  @responds never
     *  @returns nothing
     *
     * Just dispatches the passed message
     */
    "BACKEND_MESSAGE" -> ((serializeBackendMessage, handleNOP)),

    /*
     * BLOCK_SHUTDOWN
     *  responds sometimes; only if tokenCount == 1
     *  @returns "OK"
     */
    "BLOCK_SHUTDOWN" -> ((serializeEmpty, handleBlockShutdown)),

    /*
     * CHECK_RECORDING [] [%p]     [<ProgramInfo>]
     *  @responds always
     *  @returns boolean 0/1 as to whether the recording is currently taking place
     */
    "CHECK_RECORDING" -> ((serializeProgramInfo, handleCheckRecording)),

    /*
     * DELETE_FILE [] [%s, %s]   [<filename> <storage group name>]
     *  @responds sometime; only if slistCount >= 3
     *  @returns Boolean "0" on error, "1" on succesful file deletion
     */
    "DELETE_FILE" -> ((serializeDeleteFile, handleDeleteFile)),

    /*
     * DELETE_RECORDING %d %mt { FORCE { FORGET }}  <ChanId> <starttime> { can we specify NOFORCE or NOFORGET? }
     * DELETE_RECORDING [] [%p]   [<ProgramInfo>]
     * NB starttime is in myth/ISO string format rather than timestamp
     *  @responds sometimes; only if ChanId in program info
     *  @returns Int result code:
     *     0 Successful (expiration (move to Deleted recgroup) only?)
     *    -1 Recording moved to Deleted recgroup; deletion pending
     *    -2 Error deleting file
     */
    "DELETE_RECORDING" -> ((serializeDeleteRecording, handleDeleteRecording)),

    /*
     * DONE
     *  @responds never
     *  @returns nothing, closes the client's socket
     */
    "DONE" -> ((serializeEmpty, handleNOP)),

    /*
     * DOWNLOAD_FILE [] [%s, %s, %s]       [<srcURL> <storageGroup> <fileName>]
     *  @responds sometimes; only if slistCount == 4
     *  @returns
     *     result token for some errors:
     *       downloadfile_directory_not_found
     *       downloadfile_filename_dangerous
     *     OK       targetURI  (myth://...)
     *     ERROR                             only if synchronous & download method fails
     */
    "DOWNLOAD_FILE" -> ((serializeDownloadFile, handleDownloadFile)),

    /*
     * DOWNLOAD_FILE_NOW [] [%s, %s, %s]   [<srcURL> <storageGroup> <fileName>]
     *   (this command sets synchronous = true as opposed to DOWNLOAD_FILE)
     *  @responds sometimes; only if slistCount == 4
     *  @returns see DOWNLOAD_FILE
     */
    "DOWNLOAD_FILE_NOW" -> ((serializeDownloadFile, handleDownloadFileNow)),

    /*
     * FILL_PROGRAM_INFO [] [%s, %p]     [<playback host> <ProgramInfo>]
     *  @responds always
     *  @returns ProgramInfo structure populated with updated filename and fileSize
     *           (if already contained pathname, otherwise unchanged)
     *  To get useful results, the passed programinfo needs to contain valid: filename, chanId, recStartTS
     */
    "FILL_PROGRAM_INFO" -> ((serializeFillProgramInfo, handleFillProgramInfo)),

    /*
     * FORCE_DELETE_RECORDING [] [%p]   [<ProgramInfo>]
     *  @responds sometimes; only if ChanId in program info
     *  @returns see DELETE_RECORDING
     *
     *  forces deletion of recording metadata even if the recording file cannot be found on disk
     */
    "FORCE_DELETE_RECORDING" -> ((serializeProgramInfo, handleForceDeleteRecording)),

    /*
     * FORGET_RECORDING [] [%p]    [<ProgramInfo>]
     *  @responds always
     *  @returns "0"
     */
    "FORGET_RECORDING" -> ((serializeProgramInfo, handleForgetRecording)),

    /*
     * FREE_TUNER %d        <cardId>
     *  @responds sometimes; only if tokens == 2
     *  @returns "OK" or "FAILED"
     */
    "FREE_TUNER" -> ((serializeFreeTuner, handleFreeTuner)),

    /*
     * GET_FREE_RECORDER
     *  @responds always
     *  @returns [%d, %s, %d] = <best free encoder id> <host or IP> <port>
     *        or [-1, "nohost", -1] if no suitable encoder found
     */
    "GET_FREE_RECORDER" -> ((serializeEmpty, handleGetFreeRecorder)),

    /*
     * GET_FREE_RECORDER_COUNT
     *  @responds always
     *  @returns Int: number of available encoders
     */
    "GET_FREE_RECORDER_COUNT" -> ((serializeEmpty, handleGetFreeRecorderCount)),

    /*
     * GET_FREE_RECORDER_LIST
     *  @responds always
     *  @returns [%d, {, %d}] = list of available encoder ids, or "0" if none
     */
    "GET_FREE_RECORDER_LIST" -> ((serializeEmpty, handleGetFreeRecorderList)),

    /*
     * GET_NEXT_FREE_RECORDER [] [%d]  [<currentRecorder#>]
     *  @responds always
     *  @returns [%d, %s, %d] = <next free encoder id> <host or IP> <port>
     *        or [-1, "nohost", -1] if no suitable encoder found
     */
    "GET_NEXT_FREE_RECORDER" -> ((serializeCaptureCard, handleGetNextFreeRecorder)),

    /*
     * GET_RECORDER_FROM_NUM [] [%d]   [<recorder#>]
     *  @responds always
     *  @returns [%s, %d] = <host or IP> <port>
     *        or ["nohost", -1] if no matching recorder found
     */
    "GET_RECORDER_FROM_NUM" -> ((serializeCaptureCard, handleGetRecorderFromNum)),

    /*
     * GET_RECORDER_NUM [] [%p]        [<ProgramInfo>]
     *  @responds always
     *  @returns [%d, %s, %d] =   <encoder#> <host or IP> <port>
     *        or [-1, "nohost", -1] if no matching recorder found
     */
    "GET_RECORDER_NUM" -> ((serializeProgramInfo, handleGetRecorderNum)),

    /*
     * GO_TO_SLEEP
     *  @responds always
     *  @returns "OK" or "ERROR: SleepCommand is empty"
     * (only for slaves, but no checking?! Looks @ CoreContext "SleepCommand" setting)
     */
    "GO_TO_SLEEP" -> ((serializeEmpty, handleGoToSleep)),

    /*
     * LOCK_TUNER  (implicitly passes -1 as tuner id, what does this accomplish? first available local tuner?)
     * LOCK_TUNER %d  <cardId>
     *  @responds sometimes; only if tokenCount in { 1, 2 }
     *  @returns [%d, %s, %s, %s]  <cardid> <videodevice> <audiodevice> <vbidevice> (from capturecard table)
     *       or  [-2, "", "", ""]  if tuner is already locked
     *       or  [-1, "", "", ""]  if no tuner found to lock
     */
    "LOCK_TUNER" -> ((serializeLockTuner, handleLockTuner)),

    /*
     * MESSAGE [] [ %s {, %s }* ]        [<message> <extra...>]
     * MESSAGE [] [ SET_VERBOSE %s ]     [<verboseMask>]
     * MESSAGE [] [ SET_LOG_LEVEL %s ]   [<logLevel>]
     *  @responds sometimes; if SET_xxx then always, otherwise if slistCount >= 2
     *  @returns          "OK"
     *     SET_VERBOSE:   "OK" or "Failed"
     *     SET_LOG_LEVEL: "OK" or "Failed"
     */
    "MESSAGE" -> ((serializeMessage, handleMessage)),

    /*
     * MYTH_PROTO_VERSION %s %s    <version> <protocolToken>
     *  @responds sometimes; only if tokenCount >= 2
     *  @returns ["REJECT, %d"] or ["ACCEPT, %d"] where %d is MYTH_PROTO_VERSION
     */
    "MYTH_PROTO_VERSION" -> ((serializeMythProtoVersion, handleMythProtoVersion)),

    /*
     * QUERY_ACTIVE_BACKENDS
     *  @responds always
     *  @returns %d [] [ %s {, %s }* ]  <count> [ hostName, ... ]
     */
    "QUERY_ACTIVE_BACKENDS" -> ((serializeEmpty, handleQueryActiveBackends)),

    /*
     * QUERY_BOOKMARK %d %t   <ChanId> <starttime>
     *  @responds sometimes, only if tokenCount == 3
     *  @returns %ld   <bookmarkPos> (frame number)
     */
    "QUERY_BOOKMARK" -> ((serializeChanIdStartTime, handleQueryBookmark)),

    /*
     * QUERY_CHECKFILE [] [%b, %p]     <checkSlaves> <ProgramInfo>
     *  @responds always
     *  @returns %d %s      <exists:0/1?>  <playbackURL>
     *    note playback url will be "" if file does not exist
     */
    "QUERY_CHECKFILE" -> ((serializeQueryCheckFile, handleQueryCheckFile)),

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
    "QUERY_COMMBREAK" -> ((serializeChanIdStartTime, handleQueryCommBreak)),

    /*
     * QUERY_CUTLIST %d %t             <ChanId> <starttime>
     *  @responds sometimes; only if tokenCount == 3
     *  @returns see QUERY_COMMBREAK
     */
    "QUERY_CUTLIST" -> ((serializeChanIdStartTime, handleQueryCutList)),

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
    "QUERY_FILE_EXISTS" -> ((serializeQueryFileExists, handleQueryFileExists)),

    /*
     * QUERY_FILE_HASH [] [%s, %s {, %s}]     <filename> <storageGroup> {<hostname>}
     *  @responds sometimes; only if slistCount >= 3
     *  @returns
     *      ""  on error checking for file, invalid input
     *          ----> NB cannot reproduce this; seems to be a bug/limitation in the backend code
     *                   as when I trigger this condition, the server throws this log entry:
     *                E MythSocketThread(56) mythsocket.cpp:694 (WriteStringListReal) MythSocket(27f1540:56):
     *                      WriteStringList: Error, joined null string.
     *           ----> Same problem as above if pass wrong number of arguments
     *           FIXME UPSTREAM File bug/pull request with MythTV upstream
     *      %s  hash of the file (currently 64-bit, so 16 hex characters)
     *     "NULL" if file was zero-length or did not exist (any other conditions?)
     * NB storageGroup parameter seems to be a hint at most, specifying a non-existing or
     *    incorrect storageGroup does not prevent the proper hash being returned
     */
    "QUERY_FILE_HASH" -> ((serializeQueryFileHash, handleQueryFileHash)),

    /*
     * QUERY_FILETRANSFER %d [DONE]                 <ftID>
     * QUERY_FILETRANSFER %d [REQUEST_BLOCK, %d]    <ftID> [ blockSize ]
     * QUERY_FILETRANSFER %d [WRITE_BLOCK, %d]      <ftID> [ blockSize ]
     * QUERY_FILETRANSFER %d [SEEK, %ld, %d, %ld]   <ftID> [ pos, whence, curPos ]
     * QUERY_FILETRANSFER %d [IS_OPEN]              <ftID>
     * QUERY_FILETRANSFER %d [REOPEN %s]            <ftID> [ newFilename ]
     * QUERY_FILETRANSFER %d [SET_TIMEOUT %b]       <ftID> [ fast ]
     * QUERY_FILETRANSFER %d [REQUEST_SIZE]         <ftID>
     *  @responds sometimes, only if tokenCount == 2
     *  @returns
     *       "ERROR: ......."           if ftID not found
     *       "ERROR", "invalid call"    if unknown command
     *      DONE          -> "OK"
     *      REQUEST_BLOCK ->  %d        bytes sent, -1 on error  [does this block until transfer is complete?]
     *      WRITE_BLOCK   ->  %d        bytes received, -1 on error  [ " ]
     *      SEEK          ->  %ld       new file position?, -1 on error
     *      IS_OPEN       ->  %b        boolean result of ft->isOpen
     *      REOPEN        ->  %b        boolean result of ft->ReOpen  [ false if not writemode ]
     *      SET_TIMEOUT   -> "OK"
     *      REQUEST_SIZE  -> [%ld, %b]  ft->GetFileSize, !gCoreContext->IsRegisteredFileForWrite(ft->GetFileName())
     */
    "QUERY_FILETRANSFER" -> ((serializeQueryFileTransfer, handleQueryFileTransfer)),

    /*
     * QUERY_FREE_SPACE
     *  @responds always
     *  @returns  one or more "FreeSpace" objects comprised of the following fields:
     *      %s   host name
     *      %s   path name
     *      %b   is local
     *      %d   disk number
     *      %d   StorageGroupId
     *      %ld  block size
     *      %ld  total space
     *      %ld  used space
     */
    "QUERY_FREE_SPACE" -> ((serializeEmpty, handleQueryFreeSpace)),

    /*
     * QUERY_FREE_SPACE_LIST
     *  @responds always
     *  @returns see return value for QUERY_FREE_SPACE
     *
     * Like QUERY_FREE_SPACE but returns free space on all hosts, each directory
     * is reported as a URL, and a TotalDiskSpace is appended.
     */
    "QUERY_FREE_SPACE_LIST" -> ((serializeEmpty, handleQueryFreeSpaceList)),

    /*
     * QUERY_FREE_SPACE_SUMMARY
     *  @responds always
     *  @returns [%d, %d]    <total size> <used size>  sizes are in kB (1024-byte blocks)
     *        or [ 0, 0 ]    if there was any sort of error
     */
    "QUERY_FREE_SPACE_SUMMARY" -> ((serializeEmpty, handleQueryFreeSpaceSummary)),

    /*
     * QUERY_GENPIXMAP2 []
     *     [%s, %p]                       <token> <ProgramInfo>
     *     [%s, %p, %s, %ld, %s, %d, %d]  <token> <ProgramInfo> <timeFmt:sORf> <time> <outputFile> <width> <height>
     *   token can be the literal "do_not_care" if we want one randomly assigned
     *   outputFile may be "<EMPTY>"
     *   time may be -1 (only in combination with "s" format?)
     *   width may be 0
     *   height may be 0
     *  @responds always?
     *  @returns
     *    ["OK"]
     *    or ["OK", %s]    <filename>   (if outputFile was given originally in request?)
     *       or ?? TODO follow up on successful return indication/other errors from slave pixmap generation
     *       or ["ERROR", "TOO_FEW_PARAMS"]
     *       or ["ERROR", "TOKEN_ABSENT"]
     *       or ["ERROR", "FILE_INACCESSIBLE"]
     *       or ["BAD", "NO_PATHNAME"]
     * Does this follow up later with a message when the pixmap generation is complete?
     */
    "QUERY_GENPIXMAP2" -> ((serializeGenPixmap2, handleGenPixmap2)),

    /*
     * QUERY_GETALLPENDING { %s {, %d}}  { <tmptable> {, <recordid>}}
     *  @responds always
     *  @returns %d %b [%p {, %p}]  <expectedCount> <hasConflicts> <list of ProgramInfo>
     *        or ["0", "0"] if not availble/error?
     *  TODO what is the purpose of the optional tmptable and recordid parameters?
     */
    "QUERY_GETALLPENDING" -> ((serializeQueryGetAllPending, handleQueryGetAllPending)),

    /*
     * QUERY_GETALLSCHEDULED
     *  @responds always
     *  @returns %d [%p {, %p}] <expectedCount> <list of ProgramInfo>
     *        or "0" if not availble/error?
     */
    "QUERY_GETALLSCHEDULED" -> ((serializeEmpty, handleQueryGetAllScheduled)),

    /*
     * QUERY_GETCONFLICTING [] [%p]     [<ProgramInfo>]
     *  @responds always
     *  @returns %d [%p {, %p}]  <expectedCount> <list of ProgramInfo>
     *        or "0" if not availble/error?
     */
    "QUERY_GETCONFLICTING" -> ((serializeProgramInfo, handleQueryGetConflicting)),

    /*
     * QUERY_GETEXPIRING
     *  @responds always
     *  @returns %d [%p {, %p}]  <list of ProgramInfo>
     *        or "0" if not availble/error?
     */
    "QUERY_GETEXPIRING" -> ((serializeEmpty, handleQueryGetExpiring)),

    /*
     * QUERY_GUIDEDATATHROUGH
     *  @responds always
     *  @returns: Date/Time as a string in "YYYY-MM-DD hh:mm" format
     *         or "0000-00-00 00:00" in case of error or no data
     */
    "QUERY_GUIDEDATATHROUGH" -> ((serializeEmpty, handleQueryGuideDataThrough)),

    /*
     * QUERY_HOSTNAME
     *  @responds always
     *  @returns %s  <hostname>
     */
    "QUERY_HOSTNAME" -> ((serializeEmpty, handleQueryHostname)),

    /*
     * QUERY_IS_ACTIVE_BACKEND [] [%s]   [<hostname>]
     *  @responds sometimes; only if tokenCount == 1
     *  @returns "TRUE" or "FALSE"
     * FIXME UPSTREAM causes NPE on backend if hostname is not passed
     */
    "QUERY_IS_ACTIVE_BACKEND" -> ((serializeQueryIsActiveBackend, handleQueryIsActiveBackend)),

    /*
     * QUERY_ISRECORDING
     *  @responds always
     *  @returns [%d, %d]  <numRecordingsInProgress> <numLiveTVinProgress>
     *                           (liveTV is a subset of recordings)
     */
    "QUERY_ISRECORDING" -> ((serializeEmpty, handleQueryIsRecording)),

    /*
     * QUERY_LOAD
     *  @responds always
     *  @returns [%f, %f, %f]   1-min  5-min  15-min load averages
     *        or ["ERROR", "getloadavg() failed"] in case of error
     */
    "QUERY_LOAD" -> ((serializeEmpty, handleQueryLoad)),

    /*
     * QUERY_MEMSTATS
     *  @responds always
     *  @returns [%d, %d, %d, %d]  <totalMB> <freeMB> <totalVM> <freeVM>
     *        or ["ERROR", "Could not determine memory stats."] on error
     */
    "QUERY_MEMSTATS" -> ((serializeEmpty, handleQueryMemStats)),

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
    "QUERY_PIXMAP_GET_IF_MODIFIED" -> ((serializeQueryPixmapGetIfModified, handleQueryPixmapGetIfModified)),

    /*
     * QUERY_PIXMAP_LASTMODIFIED [] [%p]      [<ProgramInfo>]
     *  @responds
     *  @returns %ld    <last modified (timestamp)>
     *        or "BAD"
     */
    "QUERY_PIXMAP_LASTMODIFIED" -> ((serializeProgramInfo, handleQueryPixmapLastModified)),

    /*
     * QUERY_RECORDER [ %d                 <recorder#>     // NB two tokens! recorder# + subcommand list
     *     IS_RECORDING                                                        -> Boolean
     *   | GET_FRAMERATE                                                       -> Double
     *   | GET_FRAMES_WRITTEN                                                  -> Long
     *   | GET_FILE_POSITION                                                   -> Long
     *   | GET_MAX_BITRATE                                                     -> Long
     *   | GET_KEYFRAME_POS [%ld]          [<desiredFrame>]                    -> Long
     *   | FILL_POSITION_MAP [%ld, %ld]    [<start> <end>]                     -> Map<VideoPositionFrame,Long> or "OK" or "error"
     *   | FILL_DURATION_MAP [%ld, %ld]    [<start> <end>]                     -> Map<VideoPositionFrame,Long> or "OK" or "error"
     *   | GET_CURRENT_RECORDING                                               -> Recording
     *   | GET_RECORDING                                                       -> Recording
     *   | FRONTEND_READY                                                      -> "OK"
     *   | CANCEL_NEXT_RECORDING [%b]      [<cancel>]                          -> "OK"
     *   | SPAWN_LIVETV [%s, %b, %s]       [<chainId> <pip> <channumStart>]    -> "OK"
     *   | STOP_LIVETV                                                         -> "OK"
     *   | PAUSE                                                               -> "OK"
     *   | FINISH_RECORDING                                                    -> "OK"
     *   | SET_LIVE_RECORDING [%d]         [<recordingState>]                  -> "OK"
     *   | GET_FREE_INPUTS [{%d {, %d}*}*] [{<excludeCardId...>}]              -> List[CardInput] or "EMPTY_LIST"
     *   | GET_INPUT                                                           -> String (input name?) or "UNKNOWN"
     *   | SET_INPUT [%s]                  [<input>]                           -> String (input name?) or "UNKNOWN"
     *   | TOGGLE_CHANNEL_FAVORITE [%s]    [<channelGroup>]                    -> "OK"
     *   | CHANGE_CHANNEL [%d]             [<channelChangeDirection>]          -> "OK"
     *   | SET_CHANNEL [%s]                [<channum>]                         -> "OK"
     *   | SET_SIGNAL_MONITORING_RATE [%d, %b]  [<rate>, <notifyFrontEnd>]     -> 1 if turned on (-1 on error)
     *   | GET_COLOUR                                                          -> Int (-1 on error)
     *   | GET_CONTRAST                                                        -> Int      "
     *   | GET_BRIGHTNESS                                                      -> Int      "
     *   | GET_HUE                                                             -> Int      "
     *   | CHANGE_COLOUR [%d, %b]          [<type> <up>]                       -> Int (-1 on error)
     *   | CHANGE_CONTRAST [%d, %b]        [<type> <up>]                       -> Int      "
     *   | CHANGE_BRIGHTNESS [%d, %b]      [<type> <up>]                       -> Int      "
     *   | CHANGE_HUE [%d, %b]             [<type> <up>]                       -> Int      "
     *   | CHECK_CHANNEL [%s]              [<channum>]                         -> Boolean
     *   | SHOULD_SWITCH_CARD [%d]         [<ChanId>]                          -> Boolean
     *   | CHECK_CHANNEL_PREFIX [%s]       [<channumPrefix>]                   -> (Boolean, Option[CaptureCardId], Boolean, String)
     *   | GET_CHANNEL_INFO [%d]           [<ChanId>]                          -> Channel
     *   | GET_NEXT_PROGRAM_INFO [%s, %d, %d, %s]                              -> UpcomingProgram
     *                           [<channum> <ChanId> <BrowseDirection> <starttime>]
     * ]
     * result may be "bad" for unknown or unconnected encoder
     *  NB for SET_SIGNAL_MONITORING_RATE, the actual rate is ignored by the backend, only check is > 0
     *                                     also, notifyFrontend is no longer implemented and ignored
     *  NB for SET_LIVE_RECORDING, the recordingState parameter is ignored by the backend implementation
     */
    "QUERY_RECORDER" -> ((serializeQueryRecorder, handleQueryRecorder)),

    /*
     * QUERY_RECORDING BASENAME %s                 <pathname>
     * QUERY_RECORDING TIMESLOT %d %mt             <ChanId> <starttime>
     *  NB starttime is in myth/ISO string format rather than in timestamp
     *  @responds sometimes; only if tokenCount >= 3 (or >= 4 if TIMESLOT is specified)
     *  @returns ["OK", <ProgramInfo>] or "ERROR"
     */
    "QUERY_RECORDING" -> ((serializeQueryRecording, handleQueryRecording)),

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
    "QUERY_RECORDINGS" -> ((serializeQueryRecordings, handleQueryRecordings)),

    /*
     * QUERY_REMOTEENCODER [ %d          <encoder#>
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
     *  @responds Sometimes, only if tokenCount == 2
     `  @returns  "-1" on encoder not found error, but this can be a legitimate value for some other queries
     */
    "QUERY_REMOTEENCODER" -> ((serializeQueryRemoteEncoder, handleQueryRemoteEncoder)),

    /*
     * QUERY_SETTING %s %s      <hostname> <settingName>
     *  @responds sometimes; only if tokenCount == 3
     *  @returns %s or "-1" if not found   <settingValue>
     * NB doesn't seem possible to retrieve settings with "global" scope, i.e. hostname IS NULL
     */
    "QUERY_SETTING" -> ((serializeQuerySetting, handleQuerySetting)),

    /*
     * QUERY_SG_FILEQUERY [] [%s, %s, %s]     <hostName> <storageGroup> <fileName>
     *  @responds always
     *  @returns [%s %t %ld]                  <fullFilePath> <fileTimestamp> <fileSize>
     *        or ["EMPTY LIST"]               if wrong number of parameters given or no file found
     *        or ["SLAVE UNREACHABLE: ", %s]  if slave specified and unreachable
     */
    "QUERY_SG_FILEQUERY" -> ((serializeQuerySGFileQuery, handleQuerySGFileQuery)),

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
    "QUERY_SG_GETFILELIST" -> ((serializeQuerySGGetFileList, handleQuerySGGetFileList)),

    /*
     * QUERY_TIME_ZONE
     *  @responds always
     *  @returns [%s, %d, %s]  <timezoneName> <offsetSecsFromUtc> <currentTimeUTC>
     *    currentTimeUTC is in the ISO format "YYYY-MM-ddThh:mm:ssZ"
     */
    "QUERY_TIME_ZONE" -> ((serializeEmpty, handleQueryTimeZone)),

    /*
     * QUERY_UPTIME
     *  @responds always
     *  @returns %ld  <uptimeSeconds>
     *        or ["ERROR", "Could not determine uptime."] in case of error
     */
    "QUERY_UPTIME" -> ((serializeEmpty, handleQueryUptime)),

    /*
     * REFRESH_BACKEND
     *  @responds always
     *  @returns "OK"
     *  Seems to be a NOP on the server.
     */
    "REFRESH_BACKEND" -> ((serializeEmpty, handleRefreshBackend)),

    /*
     * RESCHEDULE_RECORDINGS
     *   [ MATCH %d %d %d %T %s ]              <recordId> <sourceId> <mplexId> <maxStartTime> { reason }
     *   [ CHECK %d %d %d %s ] [ %s %s %s %s]  <recstatus> <recordId> <findId> { reason }
     *                                         <title> <subtitle> <descrip> <programId>
     *   [ PLACE %s ]                          { reason }
     *  @responds always
     *  @returns [%b]      Boolean as to whether the reschedule request was processed
     *
     *  Some example calls from the Python bindings:
     *   RESCHEDULE_RECORDINGS [CHECK 0 0 0 Python] ['', '', '', '**any**']
     *   RESCHEDULE_RECORDINGS [MATCH %d 0 0 - Python]
     *
     *  If any extra string are appended to the first argument group, they will be logged
     *  but have no behavioral effect.
     *
     *  Looks like some special sentinel values may be used:
     *       -      for maxStartTime =  QDateTime::fromString("-", Qt::ISODate) ? invalid?
     *    **any**   for programId =  special value set in ProgLister::DeleteOldSeries()
     *
     *  All parameters to MATCH are optional, used if nonzero (or date is valid)
     *  All parameters to CHECK are optional, used if nonzero (or nonempty)
     *    CHECK recordId findId only used if programId != **all**, findId is nonzero
     *                   findId => recordId
     *
     *  See log entry for git commit cbb8eb1ee32a658a519d2d5fb751ace114f63bf9 in mythtv tree
     *  for lots of good info on the RESCHEDULE_RECORDINGS command parameters
     */
    "RESCHEDULE_RECORDINGS" -> ((serializeRescheduleRecordings, handleRescheduleRecordings)),

    /*
     * SCAN_VIDEOS
     *  @responds always
     *  @returns "OK" or "ERROR"
     */
    "SCAN_VIDEOS" -> ((serializeEmpty, handleScanVideos)),

    /*
     * SET_BOOKMARK %d %t %ld          <ChanId> <starttime> <frame#position>
     *  @responds sometimes; only if tokenCount == 4
     *  @returns "OK" or "FAILED"
     * NB If the given position is '0' then any existing bookmark will be deleted.
     */
    "SET_BOOKMARK" -> ((serializeSetBookmark, handleSetBookmark)),

    /*
     * SET_CHANNEL_INFO [] [%d, %d, %d, %d, %d, %d, %d]
     *                     <ChanId> <sourceid> <oldcnum> <callsign> <channum> <channame> <xmltv>
     *  @responds always
     *  @returns "1" for all recorders successfully handled; otherwise "0"
     *           will return "0" if there are any non-local recorders configured, so not too useful.
     *
     *  NB Implementation iterates over all (local) recorders
     *  Writes channel info to the channel table in databae!
     *   Updates { callsign, channum, name, xmltvId } keyed by (chanId, sourceId)
     *  Used by OSD channel editor (tv_play.cpp)
     */
    "SET_CHANNEL_INFO" -> ((serializeSetChannelInfo, handleSetChannelInfo)),

    /*
     * SET_NEXT_LIVETV_DIR %d %s  <encoder#> <dir>
     *  @responds sometimes; only if tokenCount == 3
     *  @returns "OK or "bad" if encoder not found
     */
    "SET_NEXT_LIVETV_DIR" -> ((serializeNOP, handleNOP)),

    /*
     * SET_SETTING %s %s %s       <hostname> <settingname> <value>
     *  @responds sometimes; only if tokenCount == 4
     *  @returns "OK" or "ERROR"
     */
    "SET_SETTING" -> ((serializeSetSetting, handleSetSetting)),

    /*
     * SHUTDOWN_NOW { %s }        { <haltCommand> }
     *  @responds never
     *  @returns nothing
     */
    "SHUTDOWN_NOW" -> ((serializeShutdownNow, handleNOP)),

    /*
     * STOP_RECORDING [] [<ProgramInfo>]
     *  @responds sometimes; only if recording is found
     *  @returns "0" if recording was on a slave backend
     *           "%d" if recording was on a local encoder, <recnum>
     *        or "-1" if not found
     */
    "STOP_RECORDING" -> ((serializeProgramInfo, handleStopRecording)),

    /*
     * UNDELETE_RECORDING [] [%d, %mt]       [<ChanId> <starttime>]
     * UNDELETE_RECORDING [] [%p]            [<ProgramInfo>]
     * NB starttime is in myth/ISO string format rather than timestamp
     *  @responds sometimes; if program info has ChanId
     *  @returns "0" on success and "-1" on error
     */
    "UNDELETE_RECORDING" -> ((serializeUndeleteRecording, handleUndeleteRecording))
  )


  /*
    * Outline for sending protocol commands:
    *
    *  i) lookup command name in table to verify it is a valid and supported command
    *  ii) parse any arguments and attempt to match against valid signatures for the command
    *  iii) serialize the argument and the command in the proper format, should result in a string
    *  iv) send the serialized command over the wire
    *  v) process the result according to rules for the given command signature
    */

  /*
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

  /*
   * Request serialization
   */

  protected def throwArgumentException(command: String, message: String) =
    throw MythProtocolArgumentException(command, message)

  protected def throwArgumentExceptionType(command: String, typeName: String) =
    throw MythProtocolArgumentException(command, s"a `$typeName` argument")

  protected def throwArgumentExceptionSignature(command: String, signature: String) =
    throw MythProtocolArgumentException(command, "the following argument signature:\n "
      + signature.trim.stripMargin)

  protected def throwArgumentExceptionMultipleSig(command: String, signatures: String) =
    throw MythProtocolArgumentException(command, "one of the following argument signatures:"
      + signatures.stripMargin)


  protected def serializeNOP(command: String, args: Seq[Any]) = ""

  protected def serializeEmpty(command: String, args: Seq[Any]): String = args match {
    case Seq() => command
    case _ => throwArgumentException(command, "empty argument list")
  }

  protected def serializeProgramInfo(command: String, args: Seq[Any]): String = args match {
    case Seq(rec: Recording) =>
      val bldr = new StringBuilder(command).append(Separator)
      serialize(rec, bldr).toString
    case _ => throwArgumentExceptionType(command, "Recording")
  }

  protected def serializeChanIdStartTime(command: String, args: Seq[Any]): String = args match {
    case Seq(chanId: ChanId, startTime: MythDateTime) =>
      val elems = List(command, serialize(chanId), serialize(startTime))
      elems mkString " "
    case _ => throwArgumentException(command, "`ChanId` and `MythDateTime` arguments")
  }

  protected def serializeCaptureCard(command: String, args: Seq[Any]): String = args match {
    case Seq(cardId: CaptureCardId) =>
      val elems = List(command, serialize(cardId))
      elems mkString Separator
    case _ => throwArgumentExceptionType(command, "CaptureCardId")
  }

  /* --- */

  protected def serializeAnnounce(command: String, args: Seq[Any]): String = args match {
    case Seq(mode @ "Monitor", clientHostName: String, eventsMode: MythProtocolEventMode) =>
      val elems = List(command, mode, clientHostName, serialize(eventsMode.id))
      elems mkString " "
    case Seq(mode @ "Playback", clientHostName: String, eventsMode: MythProtocolEventMode) =>
      val elems = List(command, mode, clientHostName, serialize(eventsMode.id))
      elems mkString " "
    case Seq(mode @ "MediaServer", clientHostName: String) =>
      val elems = List(command, mode, clientHostName)
      elems mkString " "
    case Seq(mode @ "SlaveBackend", slaveHostName: String, slaveIpAddr: InetAddress, currentlyRecording: Seq[_]) =>
      val base = List(command, mode, slaveHostName, slaveIpAddr.getHostAddress)
      val recs = currentlyRecording collect { case r: Recording => r } map (serialize(_))
      val elems = List(base mkString " ") ++ recs
      elems mkString Separator
    case Seq(mode @ "FileTransfer", clientHostName: String, fileName: String, storageGroup: String, checkFiles @ _*) =>
      val base = List(command, mode, clientHostName)
      val check = checkFiles map (_.toString)
      val elems = List(base mkString " ", fileName, storageGroup) ++ check
      elems mkString Separator
    case Seq(mode @ "FileTransfer", clientHostName: String, writeMode: Boolean, fileName: String, storageGroup: String, checkFiles @ _*) =>
      val base = List(command, mode, clientHostName, serialize(writeMode))
      val check = checkFiles map (_.toString)
      val elems = List(base mkString " ", fileName, storageGroup) ++ check
      elems mkString Separator
    case Seq(mode @ "FileTransfer", clientHostName: String, writeMode: Boolean, useReadAhead: Boolean,
      fileName: String, storageGroup: String, checkFiles @ _*) =>
      val base = List(command, mode, clientHostName, serialize(writeMode), serialize(useReadAhead))
      val check = checkFiles map (_.toString)
      val elems = List(base mkString " ", fileName, storageGroup) ++ check
      elems mkString Separator
    case Seq(mode @ "FileTransfer", clientHostName: String, writeMode: Boolean, useReadAhead: Boolean, timeout: Duration,
      fileName: String, storageGroup: String, checkFiles @ _*) =>
      val base = List(command, mode, clientHostName, serialize(writeMode), serialize(useReadAhead), serialize(timeout.toMillis))
      val check = checkFiles map (_.toString)
      val elems = List(base mkString " ", fileName, storageGroup) ++ check
      elems mkString Separator
    case _ => throwArgumentExceptionMultipleSig(command, """
      | "Monitor", clientHostName: String, eventsMode: MythProtocolEventMode
      | "Playback", clientHostName: String, eventsMode: MythProtocolEventMode
      | "MediaServer", clientHostName: String
      | "SlaveBackend", slaveHostName: String, slaveIpAddr: InetAddress, currentlyRecording: Seq[Recording]
      | "FileTransfer", clientHostName: String, fileName: String, storageGroup: String, checkFiles @ _*
      | "FileTransfer", clientHostName: String, writeMode: Boolean, fileName: String, storageGroup: String, checkFiles @ _*
      | "FileTransfer", clientHostName: String, writeMode: Boolean, useReadAhead: Boolean, fileName: String, storageGroup: String, checkFiles @ _*
      | "FileTransfer", clientHostName: String, writeMode: Boolean, useReadAhead: Boolean, timeout: Duration, fileName: String, storageGroup: String, checkFiles @ _*""")
  }

  protected def serializeBackendMessage(command: String, args: Seq[Any]): String = args match {
    case Seq(message: String, extra @ _*) =>
      val extras = extra map (_.toString)
      val elems = List(command, message) ++ extras
      elems mkString Separator
    case _ => throwArgumentExceptionSignature(command, "message: String, extra @ _*")
  }

  protected def serializeDeleteFile(command: String, args: Seq[Any]): String = args match {
    case Seq(fileName: String, storageGroup: String) =>
      val elems = List(command, fileName, storageGroup)
      elems mkString Separator
    case _ => throwArgumentExceptionSignature(command, "fileName: String, storageGroup: String")
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
      case Seq(_: Recording) => serializeProgramInfo(command, args)
      case Seq(chanId: ChanId, startTime: MythDateTime) => ser(chanId, startTime, None, None)
      case Seq(chanId: ChanId, startTime: MythDateTime, forceOpt: String) => ser(chanId, startTime, Some(forceOpt), None)
      case Seq(chanId: ChanId, startTime: MythDateTime, forceOpt: String, forgetOpt: String) =>
        ser(chanId, startTime, Some(forceOpt), Some(forgetOpt))
      case _ => throwArgumentExceptionMultipleSig(command, """
        | rec: Recording
        | chanId: ChanId, startTime: MythDateTime
        | chanId: ChanId, startTime: MythDateTime, forceOpt: String
        | chanId: ChanId, startTime: MythDateTime, forceOpt: String, forgetOpt: String""")
    }
  }

  protected def serializeDownloadFile(command: String, args: Seq[Any]): String = args match {
    case Seq(srcUrl: URI, storageGroup: String, fileName: String) =>
      val elems = List(command, srcUrl, storageGroup, fileName)
      elems mkString Separator
    case _ => throwArgumentExceptionSignature(command, """
      | srcUrl: URI, storageGroup: String, fileName: String""")
  }

  protected def serializeFillProgramInfo(command: String, args: Seq[Any]): String = args match {
    case Seq(playbackHost: String, rec: Recording) =>
      val elems = List(command, playbackHost, serialize(rec))
      elems mkString Separator
    case _ => throwArgumentExceptionSignature(command, """
      | playbackHost: String, rec: Recording""")
  }

  protected def serializeFreeTuner(command: String, args: Seq[Any]): String = args match {
    case Seq(cardId: CaptureCardId) =>
      val elems = List(command, serialize(cardId))
      elems mkString " "
    case _ => throwArgumentExceptionType(command, "CaptureCardId")
  }

  protected def serializeGenPixmap2(command: String, args: Seq[Any]): String = args match {
    case Seq(token: String, rec: Recording) =>
      val elems = List(command, token, serialize(rec))
      elems mkString Separator
    case Seq(token: String, rec: Recording, timeFmt @ "s", time: VideoPositionSeconds, outputFile: String, width: Int, height: Int) =>
      val elems = List(command, token, serialize(rec), timeFmt, serialize(time), outputFile, serialize(width), serialize(height))
      elems mkString Separator
    case Seq(token: String, rec: Recording, timeFmt @ "f", time: VideoPositionFrame, outputFile: String, width: Int, height: Int) =>
      val elems = List(command, token, serialize(rec), timeFmt, serialize(time), outputFile, serialize(width), serialize(height))
      elems mkString Separator
    case _ => throwArgumentExceptionMultipleSig(command, """
      | token: String, rec: Recording
      | token: String, rec: Recording, timeFmt @ "s", time: VideoPositionSeconds, outputFile: String, width: Int, height: Int
      | token: String, rec: Recording, timeFmt @ "f", time: VideoPositionFrame, outputFile: String, width: Int, height: Int""")
  }

  protected def serializeLockTuner(command: String, args: Seq[Any]): String = args match {
    case Seq(cardId: CaptureCardId) =>
      val elems = List(command, serialize(cardId))
      elems mkString " "
    case Seq() => command
    case _ => throwArgumentExceptionMultipleSig(command, """
      | cardId: CaptureCardId
      | - empty -""")
  }

  protected def serializeMessage(command: String, args: Seq[Any]): String = args match {
    case Seq(sub @ "SET_VERBOSE", verboseMask: String) =>
      val args = List(sub, verboseMask)
      val elems = List(command, args mkString " ")
      elems mkString Separator
    case Seq(sub @ "SET_LOG_LEVEL", logLevel: MythLogLevel) =>
      val args = List(sub, logLevel.toString.toLowerCase)
      val elems = List(command, args mkString " ")
      elems mkString Separator
    case Seq(message: String, extra @ _*) =>
      val extras = extra map (_.toString)
      val elems = List(command, message) ++ extras
      elems mkString Separator
    case _ => throwArgumentExceptionMultipleSig(command, """
      | "SET_VERBOSE", verboseMask: String
      | "SET_LOG_LEVEL", logLevel: MythLogLevel
      | message: String, extra @ _*""")
  }

  protected def serializeMythProtoVersion(command: String, args: Seq[Any]): String = args match {
    case Seq(version: Int, token: String) =>
      val elems = List(command, serialize(version), token)
      elems mkString " "
    case _ => throwArgumentExceptionSignature(command, "version: Int, token: String")
  }

  protected def serializeQueryCheckFile(command: String, args: Seq[Any]): String = args match {
    case Seq(checkSlaves: Boolean, rec: Recording) =>
      val elems = List(command, serialize(checkSlaves), serialize(rec))
      elems mkString Separator
    case _ => throwArgumentExceptionSignature(command, "checkSlaves: Boolean, rec: Recording")
  }

  protected def serializeQueryFileExists(command: String, args: Seq[Any]): String = args match {
    case Seq(fileName: String, storageGroup: String) =>
      val elems = List(command, fileName, storageGroup)
      elems mkString Separator
    case Seq(fileName: String) =>
      val elems = List(command, fileName)
      elems mkString Separator
    case _ => throwArgumentExceptionMultipleSig(command, """
      | fileName: String, storageGroup: String
      | fileName: String""")
  }

  protected def serializeQueryFileHash(command: String, args: Seq[Any]): String = args match {
    case Seq(fileName: String, storageGroup: String, hostName: String) =>
      val elems = List(command, fileName, storageGroup, hostName)
      elems mkString Separator
    case Seq(fileName: String, storageGroup: String) =>
      val elems = List(command, fileName, storageGroup)
      elems mkString Separator
    case _ => throwArgumentExceptionMultipleSig(command, """
      | fileName: String, storageGroup: String, hostName: String
      | fileName: String, storageGroup: String""")
  }

  protected def serializeQueryFileTransfer(command: String, args: Seq[Any]): String = args match {
    case Seq(ftId: FileTransferId, sub @ ("DONE" | "IS_OPEN" | "REQUEST_SIZE")) =>
      val prefix = List(command, serialize(ftId)) mkString " "
      val elems = List(prefix, sub)
      elems mkString Separator
    case Seq(ftId: FileTransferId, sub @ ("REQUEST_BLOCK" | "WRITE_BLOCK"), blockSize: Int) =>
      val prefix = List(command, serialize(ftId)) mkString " "
      val elems = List(prefix, sub, serialize(blockSize))
      elems mkString Separator
    case Seq(ftId: FileTransferId, sub @ "SEEK", pos: Long, whence: SeekWhence, curPos: Long) =>
      val prefix = List(command, serialize(ftId)) mkString " "
      val elems = List(prefix, sub, serialize(pos), serialize(whence), serialize(curPos))
      elems mkString Separator
    case Seq(ftId: FileTransferId, sub @ "REOPEN", newFileName: String) =>
      val prefix = List(command, serialize(ftId)) mkString " "
      val elems = List(prefix, sub, newFileName)
      elems mkString Separator
    case Seq(ftId: FileTransferId, sub @ "SET_TIMEOUT", fast: Boolean) =>
      val prefix = List(command, serialize(ftId)) mkString " "
      val elems = List(prefix, sub, serialize(fast))
      elems mkString Separator
    case _ => throwArgumentExceptionMultipleSig(command, """
      | ftId: FileTransferId, sub @ ("DONE" | "IS_OPEN" | "REQUEST_SIZE")
      | ftId: FileTransferId, sub @ ("REQUEST_BLOCK" | "WRITE_BLOCK"), blockSize: Int
      | ftId: FileTransferId, sub @ "SEEK", pos: Long, whence: SeekWhence, curPos: Long
      | ftId: FileTransferId, sub @ "REOPEN", newFileName: String
      | ftId: FileTransferId, sub @ "SET_TIMEOUT", fast: Boolean""")
  }

  protected def serializeQueryGetAllPending(command: String, args: Seq[Any]): String = args match {
    case Seq() => command
    // TODO: case with optional arguments; rarely used?
    case _ => throwArgumentExceptionMultipleSig(command, """
      | - empty -""")
  }

  protected def serializeQueryIsActiveBackend(command: String, args: Seq[Any]): String = args match {
    case Seq(hostName: String) =>
      val elems = List(command, hostName)
      elems mkString Separator
    case _ => throwArgumentExceptionSignature(command, "hostName: String")
  }

  protected def serializeQueryPixmapGetIfModified(command: String, args: Seq[Any]): String = args match {
    case Seq(modifiedSince: MythDateTime, maxFileSize: Long, rec: Recording) =>
      val elems = List(command, serialize(modifiedSince), serialize(maxFileSize), serialize(rec))
      elems mkString Separator
    case Seq(maxFileSize: Long, rec: Recording) =>
      val elems = List(command, "-1", serialize(maxFileSize), serialize(rec))
      elems mkString Separator
    case _ => throwArgumentExceptionMultipleSig(command, """
      | modifiedSince: MythDateTime, maxFileSize: Long, rec: Recording
      | maxFileSize: Long, rec: Recording""")
  }

  protected def serializeQueryRecorder(command: String, args: Seq[Any]): String = args match {
    case Seq(
      cardId: CaptureCardId,
      sub @
        ( "IS_RECORDING"
        | "GET_FRAMERATE"
        | "GET_FRAMES_WRITTEN"
        | "GET_FILE_POSITION"
        | "GET_MAX_BITRATE"
        | "GET_CURRENT_RECORDING"
        | "GET_RECORDING"
        | "FRONTEND_READY"
        | "STOP_LIVETV"
        | "PAUSE"
        | "FINISH_RECORDING"
        | "GET_INPUT"
        | "GET_COLOUR"
        | "GET_CONTRAST"
        | "GET_BRIGHTNESS"
        | "GET_HUE"
        )) =>
      val args = List(serialize(cardId), sub)
      val elems = List(command, args mkString Separator)
      elems mkString " "
    case Seq(cardId: CaptureCardId, sub @ "GET_KEYFRAME_POS", desiredPos: VideoPositionFrame) =>
      val args = List(serialize(cardId), sub, serialize(desiredPos))
      val elems = List(command, args mkString Separator)
      elems mkString " "
    case Seq(cardId: CaptureCardId, sub @ ("FILL_POSITION_MAP" | "FILL_DURATION_MAP"),
      start: VideoPositionFrame, end: VideoPositionFrame) =>
      val args = List(serialize(cardId), sub, serialize(start), serialize(end))
      val elems = List(command, args mkString Separator)
      elems mkString " "
    case Seq(cardId: CaptureCardId, sub @ "CANCEL_NEXT_RECORDING", cancel: Boolean) =>
      val args = List(serialize(cardId), sub, serialize(cancel))
      val elems = List(command, args mkString Separator)
      elems mkString " "
    case Seq(cardId: CaptureCardId, sub @ ("SET_CHANNEL" | "CHECK_CHANNEL"), channum: ChannelNumber) =>
      val args = List(serialize(cardId), sub, serialize(channum))
      val elems = List(command, args mkString Separator)
      elems mkString " "
    case Seq(cardId: CaptureCardId, sub @ "CHECK_CHANNEL_PREFIX", channumPrefix: ChannelNumber) =>
      val args = List(serialize(cardId), sub, serialize(channumPrefix))
      val elems = List(command, args mkString Separator)
      elems mkString " "
    case Seq(cardId: CaptureCardId, sub @ "SET_INPUT", inputName: String) =>
      val args = List(serialize(cardId), sub, inputName)
      val elems = List(command, args mkString Separator)
      elems mkString " "
    case Seq(cardId: CaptureCardId, sub @ "GET_FREE_INPUTS", excludedCardIds @ _*) =>
      val excludedIds = excludedCardIds collect { case c: CaptureCardId => c } map serialize[CaptureCardId]
      val args = List(serialize(cardId), sub) ++ excludedIds
      val elems = List(command, args mkString Separator)
      elems mkString " "
    case Seq(cardId: CaptureCardId, sub @ ("GET_CHANNEL_INFO" | "SHOULD_SWITCH_CARD"), chanId: ChanId) =>
      val args = List(serialize(cardId), sub, serialize(chanId))
      val elems = List(command, args mkString Separator)
      elems mkString " "
    case Seq(cardId: CaptureCardId, sub @ "CHANGE_CHANNEL", dir: ChannelChangeDirection) =>
      val args = List(serialize(cardId), sub, serialize(dir.id))
      val elems = List(command, args mkString Separator)
      elems mkString " "
    case Seq(cardId: CaptureCardId, sub @ "TOGGLE_CHANNEL_FAVORITE", channelGroup: String) =>
      val args = List(serialize(cardId), sub, channelGroup)
      val elems = List(command, args mkString Separator)
      elems mkString " "
    case Seq(cardId: CaptureCardId, sub @ ("CHANGE_COLOUR" | "CHANGE_CONTRAST" | "CHANGE_BRIGHTNESS" | "CHANGE_HUE"),
      adjType: PictureAdjustType, up: Boolean) =>
      val args = List(serialize(cardId), sub, serialize(adjType.id), serialize(up))
      val elems = List(command, args mkString Separator)
      elems mkString " "
    case Seq(cardId: CaptureCardId, sub @ "GET_NEXT_PROGRAM_INFO", channum: ChannelNumber, chanId: ChanId,
      dir: ChannelBrowseDirection, startTime: MythDateTime) =>
      val channelId = if (chanId.id == 0) "" else serialize(chanId)
      val args = List(serialize(cardId), sub, serialize(channum), channelId, serialize(dir.id), startTime.toIsoFormat)
      val elems = List(command, args mkString Separator)
      elems mkString " "
    case Seq(cardId: CaptureCardId, sub @ "SET_SIGNAL_MONITORING_RATE", rate: Int, notifyFrontend: Boolean) =>
      val args = List(serialize(cardId), sub, serialize(rate), serialize(notifyFrontend))
      val elems = List(command, args mkString Separator)
      elems mkString " "
    case Seq(cardId: CaptureCardId, sub @ "SPAWN_LIVETV", usePiP: Boolean, channumStart: ChannelNumber) =>
      val args = List(serialize(cardId), sub, serialize(usePiP), serialize(channumStart))
      val elems = List(command, args mkString Separator)
      elems mkString " "
    case Seq(cardId: CaptureCardId, sub @ "SET_LIVE_RECORDING", recordingState: Int) =>
      val args = List(serialize(cardId), sub, serialize(recordingState))
      val elems = List(command, args mkString Separator)
      elems mkString " "
    case _ => throwArgumentExceptionMultipleSig(command, """
      | cardId: CaptureCardId, sub @ ( "IS_RECORDING" | "GET_FRAMERATE" | "GET_FRAMES_WRITTEN" | "GET_FILE_POSITION" |
      |                                "GET_MAX_BITRATE" | "GET_CURRENT_RECORDING" | "GET_RECORDING" | "FRONTEND_READY" |
      |                                "STOP_LIVETV" | "PAUSE" | "FINISH_RECORDING" | "GET_INPUT" | "GET_COLOUR" |
      |                                "GET_CONTRAST"| "GET_BRIGHTNESS" | "GET_HUE" )
      | cardId: CaptureCardId, sub @ "GET_KEYFRAME_POS", desiredPos: VideoPositionFrame
      | cardId: CaptureCardId, sub @ ("FILL_POSITION_MAP" | "FILL_DURATION_MAP"), start: VideoPositionFrame, end: VideoPositionFrame
      | cardId: CaptureCardId, sub @ "CANCEL_NEXT_RECORDING", cancel: Boolean
      | cardId: CaptureCardId, sub @ ("SET_CHANNEL" | "CHECK_CHANNEL"), channum: ChannelNumber
      | cardId: CaptureCardId, sub @ "CHECK_CHANNEL_PREFIX", channumPrefix: ChannelNumber
      | cardId: CaptureCardId, sub @ "SET_INPUT", inputName: String
      | cardId: CaptureCardId, sub @ "GET_FREE_INPUTS", excludedCardIds @ _*
      | cardId: CaptureCardId, sub @ ("GET_CHANNEL_INFO" | "SHOULD_SWITCH_CARD"), chanId: ChanId
      | cardId: CaptureCardId, sub @ "CHANGE_CHANNEL", dir: ChannelChangeDirection
      | cardId: CaptureCardId, sub @ "TOGGLE_CHANNEL_FAVORITE", channelGroup: String
      | cardId: CaptureCardId, sub @ ("CHANGE_COLOUR" | "CHANGE_CONTRAST" | "CHANGE_BRIGHTNESS" | "CHANGE_HUE"), adjType: PictureAdjustType, up: Boolean
      | cardId: CaptureCardId, sub @ "GET_NEXT_PROGRAM_INFO", channum: ChannelNumber, chanId: ChanId, dir: ChannelBrowseDirection, startTime: MythDateTime
      | cardId: CaptureCardId, sub @ "SET_SIGNAL_MONITORING_RATE", rate: Int, notifyFrontend: Boolean
      | cardId: CaptureCardId, sub @ "SPAWN_LIVETV", usePiP: Boolean, channumStart: ChannelNumber
      | cardId: CaptureCardId, sub @ "SET_LIVE_RECORDING", recordingState: Int""")
  }

  protected def serializeQueryRecording(command: String, args: Seq[Any]): String = args match {
    case Seq(sub @ "TIMESLOT", chanId: ChanId, startTime: MythDateTime) =>
      val time: MythDateTimeString = startTime
      val elems = List(command, sub, serialize(chanId), serialize(time))
      elems mkString " "
    case Seq(sub @ "BASENAME", basePathName: String) =>
      val elems = List(command, sub, basePathName)
      elems mkString " "
    case _ => throwArgumentExceptionMultipleSig(command, """
      | "TIMESLOT", chanId: ChanId, startTime: MythDateTime
      | "BASENAME", basePathName: String""")
  }

  protected def serializeQueryRemoteEncoder(command: String, args: Seq[Any]): String = args match {
    case Seq(
      cardId: CaptureCardId,
      sub @
        ( "GET_STATE"
        | "GET_SLEEPSTATUS"
        | "GET_FLAGS"
        | "GET_RECORDING_STATUS"
        | "STOP_RECORDING"
        | "GET_MAX_BITRATE"
        | "GET_CURRENT_RECORDING"
        | "IS_BUSY"
        )) =>
      val args = List(serialize(cardId), sub)
      val elems = List(command, args mkString Separator)
      elems mkString " "
    case Seq(cardId: CaptureCardId, sub @ "IS_BUSY", timeBufferSeconds: Int) =>
      val args = List(serialize(cardId), sub, serialize(timeBufferSeconds))
      val elems = List(command, args mkString Separator)
      elems mkString " "
    case Seq(cardId: CaptureCardId, sub @ ("MATCHES_RECORDING" | "START_RECORDING"), rec: Recording) =>
      val args = List(serialize(cardId), sub, serialize(rec))
      val elems = List(command, args mkString Separator)
      elems mkString " "
    case Seq(cardId: CaptureCardId, sub @ "RECORD_PENDING", secsLeft: Int, hasLater: Boolean, rec: Recording) =>
      val args = List(serialize(cardId), sub, serialize(secsLeft), serialize(hasLater), serialize(rec))
      val elems = List(command, args mkString Separator)
      elems mkString " "
    case Seq(cardId: CaptureCardId, sub @ "CANCEL_NEXT_RECORDING", cancel: Boolean) =>
      val args = List(serialize(cardId), sub, serialize(cancel))
      val elems = List(command, args mkString Separator)
      elems mkString " "
    case Seq(cardId: CaptureCardId, sub @ "GET_FREE_INPUTS", excludedCardIds @ _*) =>
      val excludedIds = excludedCardIds collect { case c: CaptureCardId => c } map serialize[CaptureCardId]
      val args = List(serialize(cardId), sub) ++ excludedIds
      val elems = List(command, args mkString Separator)
      elems mkString " "
    case _ => throwArgumentExceptionMultipleSig(command, """
      | cardId: CaptureCardId, sub @ ( "GET_STATE" | "GET_SLEEPSTATUS" | "GET_FLAGS" | "GET_RECORDING_STATUS" |
      |                                "STOP_RECORDING" | "GET_MAX_BITRATE" | "GET_CURRENT_RECORDING" | "IS_BUSY" )
      | cardId: CaptureCardId, sub @ "IS_BUSY", timeBufferSeconds: Int
      | cardId: CaptureCardId, sub @ ("MATCHES_RECORDING" | "START_RECORDING"), rec: Recording
      | cardId: CaptureCardId, sub @ "RECORD_PENDING", secsLeft: Int, hasLater: Boolean, rec: Recording
      | cardId: CaptureCardId, sub @ "CANCEL_NEXT_RECORDING", cancel: Boolean
      | cardId: CaptureCardId, sub @ "GET_FREE_INPUTS", excludedCardIds @ _*""")
  }

  protected def serializeQueryRecordings(command: String, args: Seq[Any]): String = args match {
    case Seq(sortOrFilter: String) =>
      val elems = List(command, sortOrFilter)
      elems mkString " "
    case _ => throwArgumentExceptionSignature(command, "sortOrFilter: String")
  }

  protected def serializeQuerySGFileQuery(command: String, args: Seq[Any]): String = args match {
    case Seq(hostName: String, storageGroup: String, fileName: String) =>
      val elems = List(command, hostName, storageGroup, fileName)
      elems mkString Separator
    case _ => throwArgumentExceptionSignature(command, """
      | hostName: String, storageGroup: String, fileName: String""")
  }

  protected def serializeQuerySGGetFileList(command: String, args: Seq[Any]): String = args match {
    case Seq(hostName: String, storageGroup: String, path: String) =>
      val elems = List(command, hostName, storageGroup, path)
      elems mkString Separator
    case Seq(hostName: String, storageGroup: String, path: String, fileNamesOnly: Boolean) =>
      val elems = List(command, hostName, storageGroup, path, serialize(fileNamesOnly))
      elems mkString Separator
    case _ => throwArgumentExceptionMultipleSig(command, """
      | hostName: String, storageGroup: String, path: String
      | hostName: String, storageGroup: String, path: String, fileNamesOnly: Boolean""")
 }

  protected def serializeQuerySetting(command: String, args: Seq[Any]): String = args match {
    case Seq(hostName: String, settingName: String) =>
      val elems = List(command, hostName, settingName)
      elems mkString " "
    case _ => throwArgumentExceptionSignature(command, "hostName: String, settingName: String")
  }

  protected def serializeRescheduleRecordings(command: String, args: Seq[Any]): String = args match {
    case Seq(sub @ "MATCH", recordId: RecordRuleId, sourceId: ListingSourceId, mplexId: MultiplexId, reason: String) =>
      val args = List(sub, serialize(recordId), serialize(sourceId), serialize(mplexId), "-", reason)
      val elems = List(command, args mkString " ")
      elems mkString Separator
    case Seq(sub @ "MATCH", recordId: RecordRuleId, sourceId: ListingSourceId, mplexId: MultiplexId,
      maxStartTime: MythDateTime, reason: String) =>
      val args = List(sub, serialize(recordId), serialize(sourceId), serialize(mplexId), maxStartTime.toIsoFormat, reason)
      val elems = List(command, args mkString " ")
      elems mkString Separator
    case Seq(sub @ "CHECK", recStatus: RecStatus, recordId: RecordRuleId, findId: Int, reason: String,
      title: String, subtitle: String, description: String, programId: String) =>
      val args = List(sub, serialize(recStatus), serialize(recordId), serialize(findId), reason)
      val elems = List(command, args mkString " ", title, subtitle, description, programId)
      elems mkString Separator
    case Seq(sub @ "PLACE", reason: String) =>
      val args = List(sub, reason)
      val elems = List(command, args mkString " ")
      elems mkString Separator
    case _ => throwArgumentExceptionMultipleSig(command, """
      | "MATCH", recordId: RecordRuleId, sourceId: ListingSourceId, mplexId: MultiplexId, reason: String
      | "MATCH", recordId: RecordRuleId, sourceId: ListingSourceId, mplexId: MultiplexId, maxStartTime: MythDateTime, reason: String
      | "CHECK", recStatus: RecStatus, recordId: RecordRuleId, findId: Int, reason: String, title: String, subtitle: String, description: String, programId: String
      | "PLACE", reason: String""")
  }

  protected def serializeSetBookmark(command: String, args: Seq[Any]): String = args match {
    case Seq(chanId: ChanId, startTime: MythDateTime, position: VideoPositionFrame) =>
      val elems = List(command, serialize(chanId), serialize(startTime), serialize(position))
      elems mkString " "
    case _ => throwArgumentExceptionSignature(command, """
      | chanId: ChanId, startTime: MythDateTime, position: VideoPositionFrame""")
  }

  protected def serializeSetChannelInfo(command: String, args: Seq[Any]): String = args match {
    case Seq(chanId: ChanId, sourceId: ListingSourceId, oldChanNum: ChannelNumber, callsign: String,
      channum: ChannelNumber, name: String, xmltvId: String) =>
      val elems = List(command, serialize(chanId), serialize(sourceId), serialize(oldChanNum),
        callsign, serialize(channum), name, xmltvId)
      elems mkString Separator
    case _ => throwArgumentExceptionSignature(command, """
      | chanId: ChanId, sourceId: ListingSourceId, oldChanNum: ChannelNumber, callsign: String, channum: ChannelNumber, name: String, xmltvId: String""")
  }

  protected def serializeSetSetting(command: String, args: Seq[Any]): String = args match {
    case Seq(hostName: String, settingName: String, settingValue: String) =>
      val elems = List(command, hostName, settingName, settingValue)
      elems mkString " "
    case _ => throwArgumentExceptionSignature(command, """
      | hostName: String, settingName: String, settingValue: String""")
  }

  protected def serializeShutdownNow(command: String, args: Seq[Any]): String = args match {
    case Seq(haltCommand: String) =>
      val elems = List(command, haltCommand)
      elems mkString Separator
    case _ => throwArgumentExceptionMultipleSig(command, """
      | haltCommand: String""")
  }

  protected def serializeUndeleteRecording(command: String, args: Seq[Any]): String = args match {
    case Seq(_: Recording) => serializeProgramInfo(command, args)
    case Seq(chanId: ChanId, startTime: MythDateTime) =>
      val start: MythDateTimeString = startTime
      val elems = List(command, serialize(chanId), serialize(start))
      elems mkString Separator
    case _ => throwArgumentExceptionMultipleSig(command, """
      | rec: Recording
      | chanId: ChanId, startTime: MythDateTime""")
  }

  /*
   * Response handling
   */

  import scala.language.implicitConversions
  protected implicit def try2Result[T](t: Try[T]): MythProtocolResult[T] = t match {
    case Success(value) => Right(value)
    case Failure(ex) => Left(MythProtocolFailureThrowable(ex))
  }

  protected def handleNOP(request: BackendRequest, response: BackendResponse): MythProtocolResult[Nothing] =
    Left(MythProtocolNoResult)

  protected def handleAllowShutdown(request: BackendRequest, response: BackendResponse): MythProtocolResult[Boolean] = {
    Right(response.raw == "OK")
  }

  protected def handleAnnounce(request: BackendRequest, response: BackendResponse): MythProtocolResult[AnnounceResult] = {
    import AnnounceResult._
    val mode = request.args match { case Seq(mode: String, _*) => mode }
    if (mode == "FileTransfer") {
      val items = response.split
      if (items(0) != "OK") Left(MythProtocolFailureMessage(items mkString " "))
      else {
        val ftId = deserialize[FileTransferId](items(1))
        val fileSize = DecimalByteCount(deserialize[Long](items(2)))
        val checkFiles = items drop 3
        Right(AnnounceFileTransfer(ftId, fileSize, checkFiles))
      }
    }
    else {
      if (response.raw == "OK") Right(AnnounceAcknowledgement)
      else Left(MythProtocolFailureUnknown)
    }
  }

  protected def handleBlockShutdown(request: BackendRequest, response: BackendResponse): MythProtocolResult[Boolean] = {
    Right(response.raw == "OK")
  }

  protected def handleCheckRecording(request: BackendRequest, response: BackendResponse): MythProtocolResult[Boolean] = {
    Try(deserialize[Boolean](response.raw))
  }

  protected def handleDeleteFile(request: BackendRequest, response: BackendResponse): MythProtocolResult[Boolean] = {
    Try(deserialize[Boolean](response.raw))
  }

  protected def handleDeleteRecording(request: BackendRequest, response: BackendResponse): MythProtocolResult[Int] = {
    Try(deserialize[Int](response.raw))
  }

  protected def handleDownloadFile(request: BackendRequest, response: BackendResponse): MythProtocolResult[URI] = {
    val items = response.split
    Either.cond(items(0) == "OK" && items.length > 1, new URI(items(1)), MythProtocolNoResult)
  }

  protected def handleDownloadFileNow(request: BackendRequest, response: BackendResponse): MythProtocolResult[URI] = {
    val items = response.split
    Either.cond(items(0) == "OK" && items.length > 1, new URI(items(1)), MythProtocolFailureMessage(items mkString " "))
  }

  protected def handleFillProgramInfo(request: BackendRequest, response: BackendResponse): MythProtocolResult[Recording] = {
    Try(deserialize[Recording](response.split))
  }

  protected def handleForceDeleteRecording(request: BackendRequest, response: BackendResponse): MythProtocolResult[Int] = {
    Try(deserialize[Int](response.raw))
  }

  protected def handleForgetRecording(request: BackendRequest, response: BackendResponse): MythProtocolResult[Boolean] = {
    Try(deserialize[Int](response.raw) == 0)
  }

  protected def handleFreeTuner(request: BackendRequest, response: BackendResponse): MythProtocolResult[Boolean] = {
    Either.cond(response.raw == "OK", true, MythProtocolFailureMessage(response.raw))
  }

  protected def handleGenPixmap2(request: BackendRequest, response: BackendResponse): MythProtocolResult[Boolean] = {
    val items = response.split
    Either.cond(items(0) == "OK", true, MythProtocolFailureMessage(items mkString " "))
  }

  protected def handleGetFreeRecorder(request: BackendRequest, response: BackendResponse): MythProtocolResult[RemoteEncoder] = {
    val items = response.split
    if (items(0) == "-1") Left(MythProtocolNoResult)
    else Try {
      val cardId = deserialize[CaptureCardId](items(0))
      val host = items(1)
      val port = deserialize[Int](items(2))
      BackendRemoteEncoder(cardId, host, port)
    }
  }

  protected def handleGetFreeRecorderCount(request: BackendRequest, response: BackendResponse): MythProtocolResult[Int] = {
    Try(deserialize[Int](response.raw))
  }

  protected def handleGetFreeRecorderList(request: BackendRequest, response: BackendResponse): MythProtocolResult[List[CaptureCardId]] = {
    Try {
      if (response.raw == "0") Nil
      else {
        val cards = response.split map deserialize[CaptureCardId]
        cards.toList
      }
    }
  }

  protected def handleGetNextFreeRecorder(request: BackendRequest, response: BackendResponse): MythProtocolResult[RemoteEncoder] = {
    val items = response.split
    if (items(0) == "-1") Left(MythProtocolNoResult)
    else Try {
      val cardId = deserialize[CaptureCardId](items(0))
      val host = items(1)
      val port = deserialize[Int](items(2))
      BackendRemoteEncoder(cardId, host, port)
    }
  }

  protected def handleGetRecorderFromNum(request: BackendRequest, response: BackendResponse): MythProtocolResult[RemoteEncoder] = {
    val items = response.split
    if (items(1) == "-1") Left(MythProtocolNoResult)
    else Try {
      val host = items(0)
      val port = deserialize[Int](items(1))
      val cardId = request.args match { case Seq(cardId: CaptureCardId) => cardId }
      BackendRemoteEncoder(cardId, host, port)
    }
  }

  protected def handleGetRecorderNum(request: BackendRequest, response: BackendResponse): MythProtocolResult[RemoteEncoder] = {
    val items = response.split
    if (items(0) == "-1") Left(MythProtocolNoResult)
    else Try {
      val cardId = deserialize[CaptureCardId](items(0))
      val host = items(1)
      val port = deserialize[Int](items(2))
      BackendRemoteEncoder(cardId, host, port)
    }
  }

  protected def handleGoToSleep(request: BackendRequest, response: BackendResponse): MythProtocolResult[Boolean] = {
    Either.cond(response.raw == "OK", true, MythProtocolFailureMessage(response.raw))
  }

  protected def handleLockTuner(request: BackendRequest, response: BackendResponse): MythProtocolResult[Tuner] = {
    val items = response.split
    if      (items(0) == "-1") Left(MythProtocolNoResult)
    else if (items(0) == "-2") Left(MythProtocolFailureMessage("tuner already locked"))
    else Try {
      val cardId = deserialize[CaptureCardId](items(0))
      val videoDevice = if (items(1).isEmpty) None else Some(items(1))
      val audioDevice = if (items(2).isEmpty) None else Some(items(2))
      val vbiDevice   = if (items(3).isEmpty) None else Some(items(3))
      BackendTuner(cardId, videoDevice, audioDevice, vbiDevice)
    }
  }

  protected def handleMessage(request: BackendRequest, response: BackendResponse): MythProtocolResult[Boolean] = {
    Either.cond(response.raw == "OK", true, MythProtocolFailureMessage(response.raw))
  }

  protected def handleMythProtoVersion(request: BackendRequest, response: BackendResponse): MythProtocolResult[(Boolean, Int)] = {
    val parts = response.split
    assert(parts.length > 1)
    val accepted = parts(0) == "ACCEPT"
    val acceptedVersion = deserialize[Int](parts(1))
    Right((accepted, acceptedVersion))
  }

  protected def handleQueryActiveBackends(request: BackendRequest, response: BackendResponse): MythProtocolResult[List[String]] = {
    Try {
      val recs = response.split
      val expectedCount = deserialize[Int](recs(0))
      if (expectedCount == 0) Nil
      else (recs.iterator drop 1).toList
    }
  }

  protected def handleQueryBookmark(request: BackendRequest, response: BackendResponse): MythProtocolResult[VideoPositionFrame] = {
    Try(deserialize[VideoPositionFrame](response.raw))
  }

  protected def handleQueryCheckFile(request: BackendRequest, response: BackendResponse): MythProtocolResult[String] = {
    val items = response.split
    val exists = deserialize[Boolean](items(0))
    if (exists) Right(items(1))
    else Left(MythProtocolNoResult)
  }

  protected def handleQueryCommBreak(request: BackendRequest, response: BackendResponse): MythProtocolResult[List[VideoSegment]] = {
    val items = response.split
    Try {
      val count = deserialize[Int](items(0))
      assert(count % 2 == 0)  // FIXME not guaranteed to be true!?

      if (count == 0) Nil
      else {
        // we also assume that the number of start/end marks are balanced and in sorted order
        val marks = items.iterator drop 1 grouped 2 withPartial false map deserialize[RecordedMarkup]
        val segments = marks grouped 2 map {
          case Seq(start: RecordedMarkup, end: RecordedMarkup) =>
            assert(start.tag == Markup.CommStart)
            assert(end.tag == Markup.CommEnd)
            BackendVideoSegment(start.position, end.position)
          }
        segments.toList
      }
    }
  }

  protected def handleQueryCutList(request: BackendRequest, response: BackendResponse): MythProtocolResult[List[VideoSegment]] = {
    val items = response.split
    Try {
      val count = deserialize[Int](items(0))
      assert(count % 2 == 0)  // FIXME not guaranteed to be true!?

      if (count == 0) Nil
      else {
        // we also assume that the number of start/end marks are balanced and in sorted order
        val marks = items.iterator drop 1 grouped 2 withPartial false map deserialize[RecordedMarkup]
        val segments = marks grouped 2 map {
          case Seq(start: RecordedMarkup, end: RecordedMarkup) =>
            assert(start.tag == Markup.CutStart)
            assert(end.tag == Markup.CutEnd)
            BackendVideoSegment(start.position, end.position)
          }
        segments.toList
      }
    }
  }

  protected def handleQueryFileExists(request: BackendRequest, response: BackendResponse): MythProtocolResult[(String, FileStats)] = {
    val items = response.split
    val statusCode = deserialize[Int](items(0))
    if (statusCode > 0) Try {
      val fullName = items(1)
      val stats = deserialize[FileStats](items.view(2, 2 + 13))  // TODO hardcoded size of # file stats fields
      (fullName, stats)
    }
    else Left(MythProtocolFailureUnknown)
  }

  protected def handleQueryFileHash(request: BackendRequest, response: BackendResponse): MythProtocolResult[MythFileHash] = {
    Right(new MythFileHash(response.raw))
  }

  protected def handleQueryFileTransfer(request: BackendRequest, response: BackendResponse): MythProtocolResult[QueryFileTransferResult] = {
    import QueryFileTransferResult._

    def acknowledgement: MythProtocolResult[QueryFileTransferResult] =
      if (response.raw == "OK") Right(QueryFileTransferAcknowledgement)
      else Left(MythProtocolFailureUnknown)

    def boolean: MythProtocolResult[QueryFileTransferResult] =
      Try(QueryFileTransferBoolean(deserialize[Boolean](response.raw)))

    def bytesTransferred: MythProtocolResult[QueryFileTransferResult] = {
      val count = deserialize[Int](response.raw)
      if (count >= 0) Right(QueryFileTransferBytesTransferred(count))
      else Left(MythProtocolFailureUnknown)
    }

    def position: MythProtocolResult[QueryFileTransferResult] = {
      val pos = deserialize[Long](response.raw)
      if (pos >= 0) Right(QueryFileTransferPosition(pos))
      else Left(MythProtocolFailureUnknown)
    }

    def requestSize: MythProtocolResult[QueryFileTransferResult] = Try {
      val items = response.split
      val size = deserialize[Long](items(0))
      val readOnly = deserialize[Boolean](items(1))
      QueryFileTransferRequestSize(size, readOnly)
    }

    if (response.raw startsWith "ERROR") Left(MythProtocolFailureUnknown)
    else {
      val subcommand = request.args(1).toString
      subcommand match {
        case "DONE"          => acknowledgement
        case "REQUEST_BLOCK" => bytesTransferred
        case "WRITE_BLOCK"   => bytesTransferred
        case "SEEK"          => position
        case "IS_OPEN"       => boolean
        case "REOPEN"        => boolean
        case "SET_TIMEOUT"   => acknowledgement
        case "REQUEST_SIZE"  => requestSize
        case _               => Left(MythProtocolFailureUnknown)
      }
    }
  }

  protected def handleQueryFreeSpace(request: BackendRequest, response: BackendResponse): MythProtocolResult[List[FreeSpace]] = {
    Try {
      val items = response.split
      val fieldCount = freeSpaceSerializer.fieldCount
      val it = items.iterator grouped fieldCount withPartial false map deserialize[FreeSpace]
      it.toList
    }
  }

  protected def handleQueryFreeSpaceList(request: BackendRequest, response: BackendResponse): MythProtocolResult[List[FreeSpace]] = {
    Try {
      val items = response.split
      val fieldCount = freeSpaceSerializer.fieldCount
      val it = items.iterator grouped fieldCount withPartial false map deserialize[FreeSpace]
      it.toList
    }
  }

  protected def handleQueryFreeSpaceSummary(request: BackendRequest, response: BackendResponse): MythProtocolResult[(ByteCount, ByteCount)] = {
    val items = response.split
    assert(items.length > 1)
    if (items(0) == "0" && items(1) == "0") Left(MythProtocolFailureUnknown)
    else Try {
      val data = items map (n => deserialize[Long](n) * 1024)
      (DecimalByteCount(data(0)), DecimalByteCount(data(1)))
    }
  }

  protected def handleQueryGetAllPending(request: BackendRequest, response: BackendResponse): MythProtocolResult[ExpectedCountIterator[Recording]] = {
    val recs = response.split
    Try {
      val hasConflicts = deserialize[Boolean](recs(0))  // TODO return this also?
      val expectedCount = deserialize[Int](recs(1))
      if (expectedCount == 0) ExpectedCountIterator.empty
      else {
        val fieldCount = programInfoSerializer.fieldCount
        val it = recs.iterator drop 2 grouped fieldCount withPartial false
        new ExpectedCountIterator(expectedCount, it map deserialize[Recording])
      }
    }
  }

  protected def handleQueryGetAllScheduled(request: BackendRequest, response: BackendResponse): MythProtocolResult[ExpectedCountIterator[Recording]] = {
    val recs = response.split
    Try {
      val expectedCount = deserialize[Int](recs(0))
      if (expectedCount == 0) ExpectedCountIterator.empty
      else {
        val fieldCount = programInfoSerializer.fieldCount
        val it = recs.iterator drop 1 grouped fieldCount withPartial false
        new ExpectedCountIterator(expectedCount, it map deserialize[Recording])
      }
    }
  }

  protected def handleQueryGetConflicting(request: BackendRequest, response: BackendResponse): MythProtocolResult[ExpectedCountIterator[Recording]] = {
    val recs = response.split
    Try {
      val expectedCount = deserialize[Int](recs(0))
      if (expectedCount == 0) ExpectedCountIterator.empty
      else {
        val fieldCount = programInfoSerializer.fieldCount
        val it = recs.iterator drop 1 grouped fieldCount withPartial false
        new ExpectedCountIterator(expectedCount, it map deserialize[Recording])
      }
    }
  }

  protected def handleQueryGetExpiring(request: BackendRequest, response: BackendResponse): MythProtocolResult[ExpectedCountIterator[Recording]] = {
    val recs = response.split
    Try {
      val expectedCount = deserialize[Int](recs(0))
      if (expectedCount == 0) ExpectedCountIterator.empty
      else {
        val fieldCount = programInfoSerializer.fieldCount
        val it = recs.iterator drop 1 grouped fieldCount withPartial false
        new ExpectedCountIterator(expectedCount, it map deserialize[Recording])
      }
    }
  }

  protected def handleQueryGuideDataThrough(request: BackendRequest, response: BackendResponse): MythProtocolResult[MythDateTime] = {
    if (response.raw startsWith "0000") Left(MythProtocolNoResult)
    else Try(deserialize[MythDateTime](response.raw))
  }

  protected def handleQueryHostname(request: BackendRequest, response: BackendResponse): MythProtocolResult[String] = {
    Right(response.raw)
  }

  protected def handleQueryIsActiveBackend(request: BackendRequest, response: BackendResponse): MythProtocolResult[Boolean] = {
    Try(deserialize[Boolean](response.raw))
  }

  protected def handleQueryIsRecording(request: BackendRequest, response: BackendResponse): MythProtocolResult[(Int, Int)] = {
    Try {
      val results = response.split map deserialize[Int]
      assert(results.length > 1)
      (results(0), results(1))
    }
  }

  protected def handleQueryLoad(request: BackendRequest, response: BackendResponse): MythProtocolResult[(Double, Double, Double)] = {
    val items = response.split
    if (items(0) == "ERROR") Left(MythProtocolFailureMessage(items mkString " "))
    else Try {
      val loads = items map deserialize[Double]
      assert(loads.length > 2)
      (loads(0), loads(1), loads(2))
    }
  }

  protected def handleQueryMemStats(request: BackendRequest, response: BackendResponse): MythProtocolResult[(ByteCount, ByteCount, ByteCount, ByteCount)] = {
    val items = response.split
    if (items(0) == "ERROR") Left(MythProtocolFailureMessage(items mkString " "))
    else Try {
      val stats = items map (n => BinaryByteCount(deserialize[Long](n) * 1024 * 1024))
      assert(stats.length > 3)
      (stats(0), stats(1), stats(2), stats(3))
    }
  }

  protected def handleQueryPixmapGetIfModified(request: BackendRequest, response: BackendResponse): MythProtocolResult[(MythDateTime, Option[PixmapInfo])] = {
    val items = response.split
    if (items(0) == "ERROR" || items(0) == "WARNING") Left(MythProtocolFailureMessage(items mkString " "))
    else Try {
      val lastModified = deserialize[MythDateTime](items(0))
      if (items.length == 1) (lastModified, None)
      else {
        val fileSize = deserialize[Long](items(1))
        val crc16 = new Crc16(deserialize[Int](items(2)))
        val base64data = new Base64String(items(3))
        (lastModified, Some(PixmapInfo(DecimalByteCount(fileSize), crc16, base64data)))
      }
    }
  }

  protected def handleQueryPixmapLastModified(request: BackendRequest, response: BackendResponse): MythProtocolResult[MythDateTime] = {
    if (response.raw == "BAD") Left(MythProtocolFailureMessage(response.raw))
    else Try(deserialize[MythDateTime](response.raw))
  }

  protected def handleQueryRecorder(request: BackendRequest, response: BackendResponse): MythProtocolResult[QueryRecorderResult] = {
    import QueryRecorderResult._

    def acknowledgement: MythProtocolResult[QueryRecorderResult] =
      if (response.raw == "OK") Right(QueryRecorderAcknowledgement)
      else Left(MythProtocolFailureUnknown)

    def boolean: MythProtocolResult[QueryRecorderResult] =
      Try(QueryRecorderBoolean(deserialize[Boolean](response.raw)))

    def bitrate: MythProtocolResult[QueryRecorderResult] =
      Try(QueryRecorderBitrate(deserialize[Long](response.raw)))

    def frameRate: MythProtocolResult[QueryRecorderResult] =
      if (response.raw == "-1") Left(MythProtocolFailureUnknown)
      else Try(QueryRecorderFrameRate(deserialize[Double](response.raw)))

    def frameCount: MythProtocolResult[QueryRecorderResult] =
      if (response.raw == "-1") Left(MythProtocolFailureUnknown)
      else Try(QueryRecorderFrameCount(deserialize[Long](response.raw)))

    def input: MythProtocolResult[QueryRecorderResult] =
      if (response.raw == "UNKNOWN") Left(MythProtocolFailureUnknown)
      else Right(QueryRecorderInput(response.raw))

    def pictureAttribute: MythProtocolResult[QueryRecorderResult] = {
      val value = deserialize[Int](response.raw)
      if (value == -1) Left(MythProtocolFailureUnknown)
      else Right(QueryRecorderPictureAttribute(value))
    }

    def position: MythProtocolResult[QueryRecorderResult] =
      Try(QueryRecorderPosition(deserialize[Long](response.raw)))

    def positionMap: MythProtocolResult[QueryRecorderResult] =
      if (response.raw == "OK") Right(QueryRecorderPositionMap(Map.empty))
      else if (response.raw == "error") Left(MythProtocolFailureUnknown)
      else Try {
        val map = (response.split grouped 2 map {
          case Array(frame, offset) =>
            (deserialize[VideoPositionFrame](frame), deserialize[Long](offset))
        }).toMap
        QueryRecorderPositionMap(map)
      }

    def recording: MythProtocolResult[QueryRecorderResult] = {
      val rec = deserialize[Recording](response.split)
      if (rec.title.isEmpty && rec.chanId.id == 0) Left(MythProtocolNoResult)
      else Right(QueryRecorderRecording(rec))
    }

    def signalMonitorRate: MythProtocolResult[QueryRecorderResult] = {
      val rate = deserialize[Int](response.raw)
      if (rate < 0) Left(MythProtocolFailureUnknown)
      else Right(QueryRecorderBoolean(rate != 0))
    }

    def freeInputs: MythProtocolResult[QueryRecorderResult] = {
      if (response.raw == "EMPTY_LIST") Right(QueryRecorderCardInputList(Nil))
      else Try {
        val fieldCount = cardInputSerializer.fieldCount
        val it = response.split.iterator grouped fieldCount withPartial false
        val inputs = (it map deserialize[CardInput]).toList
        QueryRecorderCardInputList(inputs)
      }
    }

    def checkChannelPrefix: MythProtocolResult[QueryRecorderResult] = Try {
      val items = response.split
      val matched = deserialize[Boolean](items(0))
      val cardId = deserialize[Int](items(1))
      val extraCharUseful = deserialize[Boolean](items(2))
      val spacer = items(3)
      val card = if (cardId == 0) None else Some(CaptureCardId(cardId))
      QueryRecorderCheckChannelPrefix(matched, card, extraCharUseful, spacer)
    }

    def channelInfo: MythProtocolResult[QueryRecorderResult] =
      Try(QueryRecorderChannelInfo(deserialize[Channel](response.split)))

    def nextProgramInfo: MythProtocolResult[QueryRecorderResult] =
      Try(QueryRecorderNextProgramInfo(deserialize[UpcomingProgram](response.split)))

    if (response.raw == "bad") Left(MythProtocolFailureMessage(response.raw))
    else {
      val subcommand = request.args(1).toString
      subcommand match {
        case "IS_RECORDING"               => boolean
        case "GET_FRAMERATE"              => frameRate
        case "GET_FRAMES_WRITTEN"         => frameCount
        case "GET_FILE_POSITION"          => position
        case "GET_MAX_BITRATE"            => bitrate
        case "GET_KEYFRAME_POS"           => position
        case "FILL_POSITION_MAP"          => positionMap
        case "FILL_DURATION_MAP"          => positionMap
        case "GET_CURRENT_RECORDING"      => recording
        case "GET_RECORDING"              => recording
        case "FRONTEND_READY"             => acknowledgement
        case "CANCEL_NEXT_RECORDING"      => acknowledgement
        case "SPAWN_LIVETV"               => acknowledgement
        case "STOP_LIVETV"                => acknowledgement
        case "PAUSE"                      => acknowledgement
        case "FINISH_RECORDING"           => acknowledgement
        case "SET_LIVE_RECORDING"         => acknowledgement
        case "GET_FREE_INPUTS"            => freeInputs
        case "GET_INPUT"                  => input
        case "SET_INPUT"                  => input
        case "TOGGLE_CHANNEL_FAVORITE"    => acknowledgement
        case "CHANGE_CHANNEL"             => acknowledgement
        case "SET_CHANNEL"                => acknowledgement
        case "SET_SIGNAL_MONITORING_RATE" => signalMonitorRate
        case "GET_COLOUR"                 => pictureAttribute
        case "GET_CONTRAST"               => pictureAttribute
        case "GET_BRIGHTNESS"             => pictureAttribute
        case "GET_HUE"                    => pictureAttribute
        case "CHANGE_COLOUR"              => pictureAttribute
        case "CHANGE_CONTRAST"            => pictureAttribute
        case "CHANGE_BRIGHTNESS"          => pictureAttribute
        case "CHANGE_HUE"                 => pictureAttribute
        case "CHECK_CHANNEL"              => boolean
        case "SHOULD_SWITCH_CARD"         => boolean
        case "CHECK_CHANNEL_PREFIX"       => checkChannelPrefix
        case "GET_CHANNEL_INFO"           => channelInfo
        case "GET_NEXT_PROGRAM_INFO"      => nextProgramInfo
        case _                            => Left(MythProtocolFailureUnknown)
      }
    }
  }

  protected def handleQueryRecording(request: BackendRequest, response: BackendResponse): MythProtocolResult[Recording] = {
    val items = response.split
    if (items(0) == "OK") Try(deserialize[Recording](items drop 1))
    else Left(MythProtocolFailureMessage(items mkString " "))
  }

  protected def handleQueryRecordings(request: BackendRequest, response: BackendResponse): MythProtocolResult[Iterator[Recording]] = {
    val recs = response.split
    Try {
      val expectedCount = deserialize[Int](recs(0))
      if (expectedCount == 0) ExpectedCountIterator.empty
      else {
        val fieldCount = programInfoSerializer.fieldCount
        val it = recs.iterator drop 1 grouped fieldCount withPartial false
        new ExpectedCountIterator(expectedCount, it map deserialize[Recording])
      }
    }
  }

  protected def handleQueryRemoteEncoder(request: BackendRequest, response: BackendResponse): MythProtocolResult[QueryRemoteEncoderResult] = {
    import QueryRemoteEncoderResult._

    /* Unfortunately, the backend sends "-1" as the result when an unknown encoder
       number is passed as the cardId argument. Since this is a valid response in
       some cases (e.g. getFlags), we cannot check for it globally but must check
       in each instance where it is an invalid response. Some instances of passing
       an invalid encoder number may not be able to be detected: e.g. startRecording
       where a result of "-1" is a RecStatus of "WillRecord" */

    def acknowledgement: MythProtocolResult[QueryRemoteEncoderResult] =
      if (response.raw == "OK") Right(QueryRemoteEncoderAcknowledgement)
      else if (response.raw == "-1") unknownEncoder
      else Left(MythProtocolFailureUnknown)

    def bitrate: MythProtocolResult[QueryRemoteEncoderResult] =
      if (response.raw == "-1") unknownEncoder
      else Try(QueryRemoteEncoderBitrate(deserialize[Long](response.raw)))

    def boolean: MythProtocolResult[QueryRemoteEncoderResult] =
      if (response.raw == "-1") unknownEncoder
      else Try(QueryRemoteEncoderBoolean(deserialize[Boolean](response.raw)))

    def flags: MythProtocolResult[QueryRemoteEncoderResult] =
      Try(QueryRemoteEncoderFlags(deserialize[Int](response.raw)))

    def freeInputs: MythProtocolResult[QueryRemoteEncoderResult] = {
      if (response.raw == "-1") unknownEncoder
      else if (response.raw == "EMPTY_LIST") Right(QueryRemoteEncoderCardInputList(Nil))
      else Try {
        val fieldCount = cardInputSerializer.fieldCount
        val it = response.split.iterator grouped fieldCount withPartial false
        val inputs = (it map deserialize[CardInput]).toList
        QueryRemoteEncoderCardInputList(inputs)
      }
    }

    def recording: MythProtocolResult[QueryRemoteEncoderResult] = {
      if (response.raw == "-1") unknownEncoder
      else {
        val recTry = Try(QueryRemoteEncoderRecording(deserialize[Recording](response.split)))
        if (recTry.isSuccess && recTry.get.recording.isDummy) Left(MythProtocolNoResult)
        else recTry
      }
    }

    def recstatus: MythProtocolResult[QueryRemoteEncoderResult] =
      Try(QueryRemoteEncoderRecStatus(deserialize[RecStatus](response.raw)))

    def sleepStatus: MythProtocolResult[QueryRemoteEncoderResult] =
      if (response.raw == "-1") unknownEncoder
      else Try(QueryRemoteEncoderSleepStatus(deserialize[SleepStatus](response.raw)))

    def state: MythProtocolResult[QueryRemoteEncoderResult] =
      Try(QueryRemoteEncoderState(deserialize[TvState](response.raw)))

    def tunedInputInfo: MythProtocolResult[QueryRemoteEncoderResult] =
      if (response.raw == "-1") unknownEncoder
      else Try {
        val items = response.split
        val busy = deserialize[Boolean](items.head)
        val chanId = if (busy) Some(deserialize[ChanId](items.last)) else None
        val input = if (busy) Some(deserialize[CardInput](items drop 1)) else None
        QueryRemoteEncoderTunedInputInfo(busy, input, chanId)
      }

    def unknownEncoder: MythProtocolResult[QueryRemoteEncoderResult] =
      Left(MythProtocolFailureMessage("unknown encoder: " + request.args(0)))

    val subcommand = request.args(1).toString
    subcommand match {
      case "GET_STATE"             => state
      case "GET_SLEEPSTATUS"       => sleepStatus
      case "GET_FLAGS"             => flags
      case "IS_BUSY"               => tunedInputInfo
      case "MATCHES_RECORDING"     => boolean
      case "START_RECORDING"       => recstatus
      case "GET_RECORDING_STATUS"  => recstatus
      case "RECORD_PENDING"        => acknowledgement
      case "CANCEL_NEXT_RECORDING" => acknowledgement
      case "STOP_RECORDING"        => acknowledgement
      case "GET_MAX_BITRATE"       => bitrate
      case "GET_CURRENT_RECORDING" => recording
      case "GET_FREE_INPUTS"       => freeInputs
      case _                       => Left(MythProtocolFailureUnknown)
    }
  }

  // returns "-1" on error but that could also be a legitimate value  Not really much we can do here...
  protected def handleQuerySetting(request: BackendRequest, response: BackendResponse): MythProtocolResult[String] = {
    Right(response.raw)
  }

  protected def handleQuerySGFileQuery(request: BackendRequest, response: BackendResponse): MythProtocolResult[(String, MythDateTime, ByteCount)] = {
    val items = response.split
    if (items(0) == "EMPTY LIST" || items(0).startsWith("SLAVE UNREACHABLE")) Left(MythProtocolFailureMessage(items mkString " "))
    else Try {
      val fullPath = items(0)
      val timestamp = deserialize[MythDateTime](items(1))
      val fileSize = deserialize[Long](items(2))
      (fullPath, timestamp, DecimalByteCount(fileSize))
    }
  }

  protected def handleQuerySGGetFileList(request: BackendRequest, response: BackendResponse): MythProtocolResult[List[String]] = {
    val items = response.split
    if (items(0) == "EMPTY LIST" || items(0).startsWith("SLAVE UNREACHABLE")) Left(MythProtocolFailureMessage(items mkString " "))
    else Right(items.toList)
  }

  protected def handleQueryTimeZone(request: BackendRequest, response: BackendResponse): MythProtocolResult[TimeZoneInfo] = {
    Try {
      val items = response.split
      assert(items.length > 2)
      val tz = items(0)
      val off = ZoneOffset.ofTotalSeconds(deserialize[Int](items(1)))
      val time = deserialize[Instant](items(2))
      new TimeZoneInfo {
        def tzName = tz
        def offset = off
        def currentTime = time
      }
    }
  }

  protected def handleQueryUptime(request: BackendRequest, response: BackendResponse): MythProtocolResult[Duration] = {
    val items = response.split
    if (items(0) == "ERROR") Left(MythProtocolFailureMessage(items mkString " "))
    else Try {
      val seconds = deserialize[Long](items(0))
      Duration.ofSeconds(seconds)
    }
  }

  protected def handleRefreshBackend(request: BackendRequest, response: BackendResponse): MythProtocolResult[Boolean] = {
    val success = response.raw == "OK"
    Right(success)
  }

  protected def handleRescheduleRecordings(request: BackendRequest, response: BackendResponse): MythProtocolResult[Boolean] = {
    Try(deserialize[Boolean](response.raw))
  }

  protected def handleScanVideos(request: BackendRequest, response: BackendResponse): MythProtocolResult[Boolean] = {
    Either.cond(response.raw == "OK", true, MythProtocolFailureMessage(response.raw))
  }

  protected def handleSetBookmark(request: BackendRequest, response: BackendResponse): MythProtocolResult[Boolean] = {
    Either.cond(response.raw == "OK", true, MythProtocolFailureMessage(response.raw))
  }

  protected def handleSetChannelInfo(request: BackendRequest, response: BackendResponse): MythProtocolResult[Boolean] = {
    Try(deserialize[Boolean](response.raw))
  }

  protected def handleSetSetting(request: BackendRequest, response: BackendResponse): MythProtocolResult[Boolean] = {
    Either.cond(response.raw == "OK", true, MythProtocolFailureMessage(response.raw))
  }

  protected def handleStopRecording(request: BackendRequest, response: BackendResponse): MythProtocolResult[CaptureCardId] = {
    val cardIdTry = Try(deserialize[CaptureCardId](response.raw))
    if (cardIdTry.isFailure) try2Result(cardIdTry)
    else {
      val cardId = cardIdTry.get
      Either.cond(cardId.id != -1, cardId, MythProtocolNoResult)
    }
  }

  protected def handleUndeleteRecording(request: BackendRequest, response: BackendResponse): MythProtocolResult[Boolean] = {
    Try {
      val status = deserialize[Int](response.raw)
      status == 0
    }
  }
}

private[myth] trait MythProtocolLike75 extends MythProtocolLikeRef
private[myth] trait MythProtocolLike77 extends MythProtocolLike75
private[myth] trait MythProtocolLike88 extends MythProtocolLike77
