package mythtv
package connection
package myth

import java.time.Instant

import model._
import model.EnumTypes.RecStatus
import util.{ ByteCount, DecimalByteCount, MythDateTime }
import data.BackendLiveTvChain

sealed trait Event

sealed trait SystemEvent extends Event {
  def sender: String
}

object Event {
  case class  AskRecordingEvent(cardId: CaptureCardId, timeUntil: Int, hasRec: Boolean, hasLaterShowing: Boolean, rec: Recordable) extends Event
  case class  CommflagRequestEvent(chanId: ChanId, recStartTs: MythDateTime) extends Event
  case class  CommflagStartEvent(chanId: ChanId, recStartTs: MythDateTime) extends Event
  case class  DoneRecordingEvent(cardId: CaptureCardId, secondsSinceStart: Int, framesWritten: Long) extends Event
  case class  DownloadFileFinished(url: String, fileName: String, fileSize: ByteCount, errString: String, errCode: Int) extends Event
  case class  DownloadFileUpdateEvent(url: String, fileName: String, bytesReceived: ByteCount, bytesTotal: ByteCount) extends Event
  case class  FileClosedEvent(fileName: String) extends Event
  case class  FileWrittenEvent(fileName: String, fileSize: ByteCount) extends Event
  case class  GeneratedPixmapEvent() extends Event  // TODO parameters
  case class  GeneratedPixmapFailEvent() extends Event // TODO parameters
  case class  HousekeeperRunningEvent(hostName: String, tag: String, lastRunTime: Instant) extends Event
  case class  LiveTvChainUpdateEvent(chainId: LiveTvChainId, maxPos: Int, chain: List[LiveTvChain]) extends Event
  case class  RecordingListAddEvent(chanId: ChanId, recStartTs: MythDateTime) extends Event
  case class  RecordingListDeleteEvent(chanId: ChanId, recStartTs: MythDateTime) extends Event
  case class  RecordingListUpdateEvent(program: Program) extends Event
  case object ScheduleChangeEvent extends Event
  case class  UpdateFileSizeEvent(chanId: ChanId, recStartTs: MythDateTime, size: ByteCount) extends Event
  case class  VideoListChangeEvent(changes: Map[String, Set[VideoId]]) extends Event
  case object VideoListNoChangeEvent extends Event
  case class  UnknownEvent(name: String, body: String*) extends Event
}

object SystemEvent {
  case class AirPlayDeleteConnectionEvent(sender: String) extends SystemEvent
  case class AirPlayNewConnectionEvent(sender: String) extends SystemEvent
  case class AirTunesDeleteConnectionEvent(sender: String) extends SystemEvent
  case class AirTunesNewConnectionEvent(sender: String) extends SystemEvent
  case class ClientConnectedEvent(hostName: String, sender: String) extends SystemEvent
  case class ClientDisconnectedEvent(hostName: String, sender: String) extends SystemEvent
  case class LiveTvStartedEvent(sender: String) extends SystemEvent
  case class MasterShutdownEvent(sender: String) extends SystemEvent
  case class MasterStartedEvent(sender: String) extends SystemEvent
  case class MythfilldatabaseRanEvent(sender: String) extends SystemEvent
  case class NetControlConnectedEvent(sender: String) extends SystemEvent
  case class NetControlDisconnectedEvent(sender: String) extends SystemEvent
  case class PlayChangedEvent(hostName: String, chanId: ChanId, startTime: MythDateTime, sender: String) extends SystemEvent
  case class PlayPausedEvent(hostName: String, chanId: ChanId, startTime: MythDateTime, sender: String) extends SystemEvent
  case class PlayStartedEvent(hostName: String, chanId: ChanId, startTime: MythDateTime, sender: String) extends SystemEvent
  case class PlayStoppedEvent(hostName: String, chanId: ChanId, startTime: MythDateTime, sender: String) extends SystemEvent  // TODO may not have any parameters sometimes
  case class PlayUnpausedEvent(hostName: String, chanId: ChanId, startTime: MythDateTime, sender: String) extends SystemEvent
  case class RecordingDeletedEvent(chanId: ChanId, startTime: MythDateTime, sender: String) extends SystemEvent
  case class RecordingExpiredEvent(hostName: String, chanId: ChanId, startTime: MythDateTime, sender: String) extends SystemEvent
  case class RecordingFinishedEvent(cardId: CaptureCardId, chanId: ChanId, startTime: MythDateTime, status: RecStatus, sender: String) extends SystemEvent
  case class RecordingStartedEvent(cardId: CaptureCardId, chanId: ChanId, startTime: MythDateTime, status: RecStatus, sender: String) extends SystemEvent
  case class RecordingStartedWritingEvent(cardId: CaptureCardId, chanId: ChanId, startTime: MythDateTime, status: RecStatus, sender: String) extends SystemEvent
  case class RecordPendingEvent(secondsUntilStart: Int, cardId: CaptureCardId, chanId: ChanId, startTime: MythDateTime, status: RecStatus, sender: String) extends SystemEvent
  case class SchedulerRanEvent(sender: String) extends SystemEvent
  case class ScreenCreatedEvent(screenType: String, sender: String) extends SystemEvent
  case class ScreenDestroyedEvent(screenType: String, sender: String) extends SystemEvent
  case class SettingsCacheClearedEvent(sender: String) extends SystemEvent
  case class SlaveConnectedEvent(hostName: String, sender: String) extends SystemEvent
  case class SlaveDisconnectedEvent(hostName: String, sender: String) extends SystemEvent
  case class ThemeInstalledEvent(path: String, sender: String) extends SystemEvent
  case class TuningSignalTimeoutEvent(cardId: CaptureCardId, sender: String) extends SystemEvent
  case class UnknownSystemEvent(name: String, data: String, sender: String) extends SystemEvent
}

trait EventParser {
  def parse(rawEvent: BackendEvent): Event
}

private class EventParserImpl extends EventParser with MythProtocolSerializer {
  import Event._
  import SystemEvent._

  protected implicit val programInfoSerializer = ProgramInfoSerializerGeneric
  protected implicit val liveTvChainSerializer = LiveTvChainSerializerGeneric

  private val SystemEventPattern = """SYSTEM_EVENT ([^ ]*) (?:(.*) )?SENDER (.*)""".r

  def parse(rawEvent: BackendEvent): Event = {
    val split = rawEvent.split
    val name = split(1).takeWhile(_ != ' ')
    name match {
      case "SYSTEM_EVENT"          => parseSystemEvent(name, split)
      case "ASK_RECORDING"         => parseAskRecording(name, split)
      case "COMMFLAG_REQUEST"      => parseCommflagRequest(name, split)
      case "COMMFLAG_START"        => parseCommflagStart(name, split)
      case "DOWNLOAD_FILE"         => parseDownloadFile(name, split)
      case "DONE_RECORDING"        => parseDoneRecording(name, split)
      case "FILE_CLOSED"           => parseFileClosed(name, split)
      case "FILE_WRITTEN"          => parseFileWritten(name, split)
      case "GENERATED_PIXMAP"      => parseGeneratedPixmap(name, split)
      case "HOUSE_KEEPER_RUNNING"  => parseHousekeeperRunning(name, split)
      case "LIVETV_CHAIN"          => parseLiveTvChain(name, split)
      case "RECORDING_LIST_CHANGE" => parseRecordingListChange(name, split)
      case "SCHEDULE_CHANGE"       => ScheduleChangeEvent
      case "UPDATE_FILE_SIZE"      => parseUpdateFileSize(name, split)
      case "VIDEO_LIST_CHANGE"     => parseVideoListChange(name, split)
      case "VIDEO_LIST_NO_CHANGE"  => VideoListNoChangeEvent
      case _ => unknownEvent(name, split)
    }
  }

  def parseSystemEvent(name: String, split: Array[String]): Event = {
    split(1) match {
      case SystemEventPattern(evt, body, sender) =>
        UnknownSystemEvent(evt, if (body eq null) "" else body, sender)
      case _ => unknownEvent(name, split)
    }
  }

  def parseAskRecording(name: String, split: Array[String]): Event = {
    val parts = split(1).split(' ')
    AskRecordingEvent(
      deserialize[CaptureCardId](parts(1)),
      deserialize[Int](parts(2)),
      deserialize[Boolean](parts(3)),
      deserialize[Boolean](parts(4)),
      deserialize[Recording](split.drop(2))
    )
  }

  def parseCommflagRequest(name: String, split: Array[String]): Event = {
    val parts = split(1).split(' ')
    val tokenSplit = parts(1).split('_')
    CommflagRequestEvent(deserialize[ChanId](tokenSplit(0)), MythDateTime(deserialize[Instant](tokenSplit(1))))
  }

  def parseCommflagStart(name: String, split: Array[String]): Event = {
    val parts = split(1).split(' ')
    val tokenSplit = parts(1).split('_')
    CommflagStartEvent(deserialize[ChanId](tokenSplit(0)), MythDateTime(deserialize[Instant](tokenSplit(1))))
  }

  def parseDoneRecording(name: String, split: Array[String]): Event = {
    val parts = split(1).split(' ')
    DoneRecordingEvent(deserialize[CaptureCardId](parts(1)), deserialize[Int](parts(2)), deserialize[Long](parts(3)))
  }

  def parseDownloadFile(name: String, split: Array[String]): Event = {
    split(1).substring(name.length + 1) match {
      case "FINISHED" => DownloadFileFinished(
        split(2),
        split(3),
        DecimalByteCount(deserialize[Long](split(4))),
        split(5),
        deserialize[Int](split(6))     // TODO last param may be empty, not convertible to int?
      )
      case "UPDATE" => DownloadFileUpdateEvent(
        split(2),
        split(3),
        DecimalByteCount(deserialize[Long](split(4))),
        DecimalByteCount(deserialize[Long](split(5)))
      )
      case _ => unknownEvent(name, split)
    }
  }

  def parseFileClosed(name: String, split: Array[String]): Event = {
    val parts = split(1).split(' ')
    FileClosedEvent(parts(1))
  }

  def parseFileWritten(name: String, split: Array[String]): Event = {
    val parts = split(1).split(' ')
    FileWrittenEvent(parts(1), DecimalByteCount(deserialize[Long](parts(2))))
  }

  def parseGeneratedPixmap(name: String, split: Array[String]): Event = {
    GeneratedPixmapEvent()  // TODO parse parameters; also success or failure
  }

  def parseHousekeeperRunning(name: String, split: Array[String]): Event = {
    val parts = split(1).split(' ')
    HousekeeperRunningEvent(parts(1), parts(2), deserialize[Instant](parts(3)))
  }

  def parseLiveTvChain(name: String, split: Array[String]): Event = {
    val parts = split(1).split(' ')
    val chainId = deserialize[LiveTvChainId](parts(2))
    val maxPos = deserialize[Int](split(2))

    val fieldCount = BackendLiveTvChain.FIELD_ORDER.length - 1
    val it = split.iterator drop 2 grouped fieldCount withPartial false map (chainId.id +: _)
    val chain = it map deserialize[LiveTvChain]

    LiveTvChainUpdateEvent(chainId, maxPos, chain.toList)
  }

  def parseRecordingListChange(name: String, split: Array[String]): Event = {
    split(1).substring(name.length + 1).takeWhile(_ != ' ') match {
      case "ADD" =>
        val parts = split(1).substring(name.length + 5).split(' ')
        RecordingListAddEvent(deserialize[ChanId](parts(0)), MythDateTime(deserialize[Instant](parts(1))))
      case "DELETE" =>
        val parts = split(1).substring(name.length + 8).split(' ')
        RecordingListDeleteEvent(deserialize[ChanId](parts(0)), MythDateTime(deserialize[Instant](parts(1))))
      case "UPDATE" =>
        RecordingListUpdateEvent(deserialize[Recording](split.drop(2)))
      case _ => unknownEvent(name, split)
    }
  }

  def parseUpdateFileSize(name: String, split: Array[String]): Event = {
    val parts = split(1).split(' ')
    UpdateFileSizeEvent(
      deserialize[ChanId](parts(1)),
      MythDateTime(deserialize[Instant](parts(2))),
      DecimalByteCount(deserialize[Long](parts(3)))
    )
  }

  def parseVideoListChange(name: String, split: Array[String]): Event = {
    // This is a list of { added | deleted | moved } :: <videoid>
    val changeItems = split.slice(2, split.length)
    val changeTuples = changeItems map (_ split "::") collect {
      case Array(x, y) => (x, VideoId(y.toInt))
    }
    // ... group by change type (key), change value to set of videoId
    val changeMap = changeTuples groupBy (_._1) mapValues { a => (a map (_._2)).toSet }
    VideoListChangeEvent(changeMap)
  }

  def unknownEvent(name: String, split: Array[String]): Event =
    UnknownEvent(name, split.slice(1, split.length): _*)
}

/*
 * Some BACKEND_MESSAGE examples
 *
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT CLIENT_CONNECTED HOSTNAME myth1 SENDER myth1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT CLIENT_DISCONNECTED HOSTNAME myth1 SENDER myth1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT NET_CTRL_CONNECTED SENDER mythfe1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT NET_CTRL_DISCONNECTED SENDER mythfe2[]:[]empty
 *  2016-11-14T13:31:57.852 SystemEvent(PLAY_CHANGED,HOSTNAME mythtest-atom CHANID 1692 STARTTIME 2016-11-14T21:32:01Z,mythtest-atom)
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT PLAY_STARTED HOSTNAME mythfe1 CHANID 1081 STARTTIME 2016-09-29T03:00:00Z SENDER mythfe1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT PLAY_STOPPED HOSTNAME mythfe1 CHANID 1081 STARTTIME 2016-09-29T03:00:00Z SENDER mythfe1[]:[]empty
 *  2016-11-14T08:57:36.187 SystemEvent(REC_EXPIRED,HOSTNAME myth1 CHANID 1132 STARTTIME 2016-11-14T16:21:00Z,myth1)
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT REC_FINISHED CARDID 4 CHANID 1151 STARTTIME 2016-11-12T22:30:00Z RECSTATUS -3 SENDER myth1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT REC_PENDING SECS 59 CARDID 4 CHANID 1151 STARTTIME 2016-11-12T22:30:00Z RECSTATUS -1 SENDER myth1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT REC_PENDING SECS 30 CARDID 4 CHANID 1151 STARTTIME 2016-11-12T22:30:00Z RECSTATUS -1 SENDER myth1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT REC_STARTED CARDID 4 CHANID 1151 STARTTIME 2016-11-12T22:30:00Z RECSTATUS -1 SENDER myth1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT REC_STARTED_WRITING CARDID 4 CHANID 1151 STARTTIME 2016-11-12T22:30:00Z RECSTATUS -2 SENDER myth1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT SCHEDULER_RAN SENDER myth1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT SCREEN_TYPE DESTROYED playbackbox SENDER mythfe1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT SCREEN_TYPE CREATED mythscreentypebusydialog SENDER mythfe1[]:[]empty
 *
 *  BACKEND_MESSAGE[]:[]ASK_RECORDING 4 29 0 0[]:[]Martha Bakes[]:[]New England[]:[]Cheddar-crusted apple pie; steamed Boston brown bread made in a can; lemon-blueberry tart; hermit bars.[]:[]0[]:[]0[]:[]706[]:[]Cooking[]:[]1151[]:[]15-1[]:[]KPBS-HD[]:[]KPBSDT (KPBS-DT)[]:[]/video/record[]:[]0[]:[]1478989800[]:[]1478991600[]:[]0[]:[]myth1[]:[]1[]:[]4[]:[]1[]:[]0[]:[]-1[]:[]562[]:[]4[]:[]15[]:[]6[]:[]1478989800[]:[]1478991600[]:[]2048[]:[]Cooking[]:[][]:[]EP01360285[]:[]EP013602850078[]:[][]:[]1478989740[]:[]0[]:[]2016-11-10[]:[]Default[]:[]0[]:[]0[]:[]Default[]:[]1[]:[]32[]:[]0[]:[]0[]:[]0[]:[]0
 *  BACKEND_MESSAGE[]:[]COMMFLAG_REQUEST 1081_2016-09-29T03:00:00Z[]:[]empty
 *  BACKEND_MESSAGE[]:[]COMMFLAG_START 1081_2016-11-11T04:00:00Z[]:[]empty
 *  BACKEND_MESSAGE[]:[]DONE_RECORDING 4 1801 -1[]:[]empty
 *  BACKEND_MESSAGE[]:[]FILE_CLOSED /video/record/1151_20161112223000.mpg[]:[]empty
 *  BACKEND_MESSAGE[]:[]FILE_WRITTEN /video/record/1151_20161112223000.mpg 1874473928[]:[]empty
 *  BACKEND_MESSAGE[]:[]GENERATED_PIXMAP[]:[]OK[]:[]1081_2016-05-19T05:00:00Z[]:[]Generated on myth1 in 3.919 seconds, starting at 16:19:33[]:[]2016-10-05T23:19:33Z[]:[]83401[]:[]39773[]:[] <<< base64 data + extra tokens redacted >>>
 *  BACKEND_MESSAGE[]:[]GENERATED_PIXMAP[]:[]OK[]:[]1151_2016-07-30T20:30:00Z[]:[]On Disk[]:[]2016-10-05T23:19:42Z[]:[]87547[]:[]13912[]:[] << base64 data + extra tokens redacted >>
 *  BACKEND_MESSAGE[]:[]HOUSE_KEEPER_RUNNING myth1 DBCleanup 2016-11-12T22:20:24Z[]:[]empty
 *  BACKEND_MESSAGE[]:[]RECORDING_LIST_CHANGE ADD 1151 2016-11-12T22:30:00Z[]:[]empty
 *  BACKEND_MESSAGE[]:[]RECORDING_LIST_CHANGE UPDATE[]:[]Survivor[]:[]Not Going Down Without a Fight[]:[]Castaways from all three tribes remain, and one will be crowned the Sole Survivor.[]:[]32[]:[]14[]:[]3214[]:[]Reality[]:[]1081[]:[]8-1[]:[]KFMB-DT[]:[]KFMBDT (KFMB-DT)[]:[]1081_20160519030000.mpg[]:[]13323011228[]:[]1463626800[]:[]1463634000[]:[]0[]:[]myth1[]:[]0[]:[]0[]:[]0[]:[]0[]:[]-3[]:[]380[]:[]0[]:[]15[]:[]6[]:[]1463626800[]:[]1463634001[]:[]11583492[]:[]Reality TV[]:[][]:[]EP00367078[]:[]EP003670780116[]:[]76733[]:[]1471849923[]:[]0[]:[]2016-05-18[]:[]Default[]:[]0[]:[]0[]:[]Default[]:[]9[]:[]17[]:[]1[]:[]0[]:[]0[]:[]0 *  BACKEND_MESSAGE[]:[]VIDEO_LIST_NO_CHANGE[]:[]empty
 *  BACKEND_MESSAGE[]:[]SCHEDULE_CHANGE[]:[]empty
 *  BACKEND_MESSAGE[]:[]UPDATE_FILE_SIZE 1151 2016-11-12T22:30:00Z 1864356708[]:[]empty
 */

/*
 * Some MESSAGE examples
 *
 *   MESSAGE[]:[]MASTER_UPDATE_PROG_INFO 1151 2016-04-14T04:00:00Z
 */

/*
 * Format of SYSTEM_EVENT
 *   SYSTEM_EVENT %s SENDER %s
 *
 *   also look into libs/libmythtv/mythsystemevent.cpp
 *              and libs/libmythbase/mythcorecontext.cpp
 */

/*
 * Format of GENERATED_PIXMAP
 *  On success:
 *      "OK"
 *      <programInfoKey>   (from pginfo->MakeUniqueKey())
 *      <msg>              (e.g "On Disk" or "Generated...")
 *      <datetime>         from the PREVIEW_SUCCESS Qt event received by the backend
 *      <dataSize>         byte count of the data (in binary, not encoded base 64)
 *      <checksum>         CRC-16 of the binary data (computed by qChecksum)
 *      <base64Data>       data encoded in base-64 format
 *      <token> *          may not be present, or may be multiple!
 *  On failure:
 *      "ERROR"
 *      <programInfoKey>
 *      <msg>              from the PREVIEW_FAILURE Qt event received by the backend
 *      <token> *          may not be present, or may be multiple!
 *   requestor token should be the first one in the token list?
 */

/*
 * Format of VIDEO_LIST_CHANGE
 *   ["added:%d"]*            %d is VideoId; repeated for each addition
 *   ["moved:%d"]*                 "       ;     "     "    "  file move
 *   ["deleted:%d"]*               "       ;     "     "    "  deletion
 */

/*
 * Format of VIDEO_LIST_NO_CHANGE
 *   <empty>
 */

/*
 * Format of RECORDING_LIST_CHANGE
 *   <may have no body>
 *   ADD     <chanId> <recstartts:ISO>
 *   DELETE  <chanId> <recstartts:ISO>
 *   UPDATE  ?? see code in mainserver.cpp
 *     update is sent by mainserver in response to Qt event MASTER_UPDATE_PROG_INFO
 */

/*
 * Format of ASK_RECORDING
 *   <cardId> <timeUntil> <hasRec> <hasLaterShowing>
 *   []:[]
 *   <programinfo> of the pending recording
 *   timeUntil is in seconds ?
 */

/*
 * Format of COMMFLAG_REQUEST and COMMFLAG_START
 *  <chanid>_<recStartTs>  (single token with underscore separator)
 */

/*
 * Format of DONE_RECORDING
 *  <cardId> <secondsSinceStart> <framesWritten>
 */

/*
 * Format of DOWNLOAD_FILE
 *   UPDATE   <url> <outFile> <bytesReceived> <bytesTotal>
 *   FINISHED <url> <outfile> <fileSize> <errorStringPlaceholder> <errorCode>
 */

/*
 * Format of HOUSE_KEEPER_RUNNING
 *   <hostName> <tag> <lastRunDateTime:ISO>
 */

/*
 * Format of UPDATE_FILE_SIZE
 *   <chanId> <recstartts:ISO> <fileSize>
 */

/*
 * Format of MASTER_UPDATE_PROG_INFO
 *   <chanId> <recstartts:ISO>
 */

/*
 * Some other events:
 *   LOCAL_RECONNECT_TO_MASTER
 *   LOCAL_SLAVE_BACKEND_ONLINE %2
 *   LOCAL_SLAVE_BACKEND_OFFLINE %1
 *   LOCAL_SLAVE_BACKEND_ENCODERS_OFFLINE
 *   THEME_INSTALLED
 *   THEME_RELOAD
 */

/*
 * Some system events:
 *   AIRPLAY_DELETE_CONNECTION
 *   AIRPLAY_NEW_CONNECTION
 *   AIRTUNES_DELETE_CONNECTION
 *   AIRTUNES_NEW_CONNECTION
 *   CLIENT_CONNECTED HOSTNAME %1
 *   CLIENT_DISCONNECTED HOSTNAME %1
 *   KEY_*                             // ???
 *   LIVETV_STARTED
 *   MASTER_SHUTDOWN
 *   MASTER_STARTED
 *   MYTHFILLDATABASE_RAN
 *   NET_CTRL_CONNECTED
 *   NET_CTRL_DISCONNECTED
 *   PLAY_CHANGED
 *   PLAY_PAUSED
 *   PLAY_STARTED
 *   PLAY_STOPPED
 *   PLAY_UNPAUSED
 *   REC_DELETED CHANID %1 STARTTIME %2
 *   REC_EXPIRED
 *   REC_FINISHED
 *   REC_PENDING
 *   REC_STARTED
 *   REC_STARTED_WRITING
 *   SCHEDULER_RAN
 *   SCREEN_TYPE CREATED %1
 *   SCREEN_TYPE DESTROYED %1
 *   SETTINGS_CACHE_CLEARED
 *   SLAVE_CONNECTED HOSTNAME %1
 *   SLAVE_DISCONNECTED HOSTNAME %1
 *   THEME_INSTALLED PATH %1
 *   TUNING_SIGNAL_TIMEOUT CARDID %1
 */

/*

2016-11-14T08:40:36.875 UnknownEvent(LIVETV_CHAIN,WrappedArray(LIVETV_CHAIN UPDATE live-mythtest-atom-2016-11-14T16:40:33Z, 1, 1391, 2016-11-14T16:40:39Z, 2016-11-14T17:00:00Z, 0, myth://192.168.1.123:6543/, DUMMY, 39-1, DVBInput))

2016-11-14T08:40:36.990 UnknownEvent(SIGNAL,WrappedArray(SIGNAL 4, Script Status, script 3 3 0 3 0 1 1, Signal Lock, slock 1 1 0 1 3000 1 1, Signal Power, signal 0 0 0 65535 3000 1 1, Seen PAT, seen_pat 0 1 0 1 0 1 1, Matching PAT, matching_pat 0 1 0 1 0 1 1, Seen MGT, seen_mgt 0 1 0 1 0 1 1, Matching MGT, matching_mgt 0 1 0 1 0 1 1, Seen VCT, seen_vct 0 1 0 1 0 1 1, Matching VCT, matching_vct 0 1 0 1 0 1 1, Signal To Noise, snr 0 0 0 65535 0 1 1, Bit Error Rate, ber 0 65535 0 65535 0 0 1, Uncorrected Blocks, ucb 0 65535 0 65535 0 0 1))
2016-11-14T08:40:37.040 UnknownEvent(SIGNAL,WrappedArray(SIGNAL 4, Script Status, script 3 3 0 3 0 1 1, Signal Lock, slock 1 1 0 1 3000 1 1, Signal Power, signal 0 0 0 65535 3000 1 1, Seen PAT, seen_pat 1 1 0 1 0 1 1, Matching PAT, matching_pat 0 1 0 1 0 1 1, Seen MGT, seen_mgt 1 1 0 1 0 1 1, Matching MGT, matching_mgt 1 1 0 1 0 1 1, Seen VCT, seen_vct 0 1 0 1 0 1 1, Matching VCT, matching_vct 0 1 0 1 0 1 1, Signal To Noise, snr 0 0 0 65535 0 1 1, Bit Error Rate, ber 0 65535 0 65535 0 0 1, Uncorrected Blocks, ucb 0 65535 0 65535 0 0 1))
2016-11-14T08:40:37.091 UnknownEvent(SIGNAL,WrappedArray(SIGNAL 4, Script Status, script 3 3 0 3 0 1 1, Signal Lock, slock 1 1 0 1 3000 1 1, Signal Power, signal 0 0 0 65535 3000 1 1, Seen PAT, seen_pat 1 1 0 1 0 1 1, Matching PAT, matching_pat 1 1 0 1 0 1 1, Seen PMT, seen_pmt 0 1 0 1 0 1 1, Matching PMT, matching_pmt 0 1 0 1 0 1 1, Seen MGT, seen_mgt 1 1 0 1 0 1 1, Matching MGT, matching_mgt 1 1 0 1 0 1 1, Seen VCT, seen_vct 1 1 0 1 0 1 1, Matching VCT, matching_vct 1 1 0 1 0 1 1, Signal To Noise, snr 0 0 0 65535 0 1 1, Bit Error Rate, ber 0 65535 0 65535 0 0 1, Uncorrected Blocks, ucb 0 65535 0 65535 0 0 1))
2016-11-14T08:40:37.141 UnknownEvent(SIGNAL,WrappedArray(SIGNAL 4, Script Status, script 3 3 0 3 0 1 1, Signal Lock, slock 1 1 0 1 3000 1 1, Signal Power, signal 0 0 0 65535 3000 1 1, Seen PAT, seen_pat 1 1 0 1 0 1 1, Matching PAT, matching_pat 1 1 0 1 0 1 1, Seen PMT, seen_pmt 1 1 0 1 0 1 1, Matching PMT, matching_pmt 1 1 0 1 0 1 1, Seen MGT, seen_mgt 1 1 0 1 0 1 1, Matching MGT, matching_mgt 1 1 0 1 0 1 1, Seen VCT, seen_vct 1 1 0 1 0 1 1, Matching VCT, matching_vct 1 1 0 1 0 1 1, Signal To Noise, snr 0 0 0 65535 0 1 1, Bit Error Rate, ber 0 65535 0 65535 0 0 1, Uncorrected Blocks, ucb 0 65535 0 65535 0 0 1))
2016-11-14T08:40:37.191 UnknownEvent(SIGNAL,WrappedArray(SIGNAL 4, Script Status, script 3 3 0 3 0 1 1, Signal Lock, slock 1 1 0 1 3000 1 1, Signal Power, signal 0 0 0 65535 3000 1 1, Seen PAT, seen_pat 1 1 0 1 0 1 1, Matching PAT, matching_pat 1 1 0 1 0 1 1, Seen PMT, seen_pmt 1 1 0 1 0 1 1, Matching PMT, matching_pmt 1 1 0 1 0 1 1, Seen MGT, seen_mgt 1 1 0 1 0 1 1, Matching MGT, matching_mgt 1 1 0 1 0 1 1, Seen VCT, seen_vct 1 1 0 1 0 1 1, Matching VCT, matching_vct 1 1 0 1 0 1 1, Signal To Noise, snr 0 0 0 65535 0 1 1, Bit Error Rate, ber 0 65535 0 65535 0 0 1, Uncorrected Blocks, ucb 0 65535 0 65535 0 0 1))

2016-11-14T08:40:37.195 UnknownEvent(LIVETV_CHAIN,WrappedArray(LIVETV_CHAIN UPDATE live-mythtest-atom-2016-11-14T16:40:33Z, 1, 1391, 2016-11-14T16:40:39Z, 2016-11-14T16:40:39Z, 0, myth://192.168.1.123:6543/, DUMMY, 39-1, DVBInput))

2016-11-14T08:40:37.260 UnknownEvent(LIVETV_CHAIN,WrappedArray(LIVETV_CHAIN UPDATE live-mythtest-atom-2016-11-14T16:40:33Z, 2, 1391, 2016-11-14T16:40:39Z, 2016-11-14T16:40:39Z, 0, myth://192.168.1.123:6543/, DUMMY, 39-1, DVBInput, 1391, 2016-11-14T16:40:40Z, 2016-11-14T17:00:00Z, 1, myth://192.168.1.123:6543/, DVB, 39-1, DVBInput))

2016-11-14T08:42:39.211 UnknownEvent(LIVETV_CHAIN,WrappedArray(LIVETV_CHAIN UPDATE live-mythtest-atom-2016-11-14T16:40:33Z, 2, 1391, 2016-11-14T16:40:40Z, 2016-11-14T17:00:00Z, 1, myth://192.168.1.123:6543/, DVB, 39-1, DVBInput))

 */
