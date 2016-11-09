package mythtv
package connection
package myth

import model.{ ChanId, Program, VideoId }
import util.MythDateTime
import data.BackendProgram

sealed trait Event

object Event {
  case class SystemEvent(name: String, data: String, sender: String) extends Event
  case class DownloadFileFinished(url: String, fileName: String, fileSize: Long, errString: String, errCode: Int) extends Event
  case class DownloadFileUpdateEvent(url: String, fileName: String, bytesReceived: Long, bytesTotal: Long) extends Event
  case class GeneratedPixmapEvent() extends Event  // TODO parameters
  case class GeneratedPixmapFailEvent() extends Event // TODO parameters
  case class RecordingListAddEvent(chanId: ChanId, recStartTs: MythDateTime) extends Event
  case class RecordingListDeleteEvent(chanId: ChanId, recStartTs: MythDateTime) extends Event
  case class RecordingListUpdateEvent(program: Program) extends Event
  case object ScheduleChangeEvent extends Event
  case class UpdateFileSizeEvent(chanId: ChanId, recStartTs: MythDateTime, size: Long) extends Event
  case class VideoListChangeEvent(changes: Map[String, Set[VideoId]]) extends Event
  case object VideoListNoChangeEvent extends Event
  case class UnknownEvent(name: String, body: String*) extends Event
}

class EventParser {
  import Event._

  private val SystemEventPattern = """SYSTEM_EVENT ([^ ]*) (?:(.*) )?SENDER (.*)""".r

  def parse(rawEvent: BackendEvent): Event = {
    val split = rawEvent.split
    val name = split(1).takeWhile(_ != ' ')
    name match {
      case "SYSTEM_EVENT"          => parseSystemEvent(name, split)
      case "DOWNLOAD_FILE"         => parseDownloadFile(name, split)
      case "GENERATED_PIXMAP"      => parseGeneratedPixmap(name, split)
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
        SystemEvent(evt, if (body eq null) "" else body, sender)
      case _ => unknownEvent(name, split)
    }
  }

  def parseDownloadFile(name: String, split: Array[String]): Event = {
    split(1).substring(name.length + 1) match {
      case "FINISHED" => DownloadFileFinished(split(2), split(3), split(4).toLong, split(5), split(6).toInt) // TODO last param may be empty, not convertible to int?
      case "UPDATE" => DownloadFileUpdateEvent(split(2), split(3), split(4).toLong, split(5).toLong)
      case _ => unknownEvent(name, split)
    }
  }

  def parseGeneratedPixmap(name: String, split: Array[String]): Event = {
    GeneratedPixmapEvent()  // TODO parse parameters; also success or failure
  }

  def parseRecordingListChange(name: String, split: Array[String]): Event = {
    split(1).substring(name.length + 1) match {
      case "ADD" =>
        val parts = split(1).substring(name.length + 4).split(' ')
        RecordingListAddEvent(ChanId(parts(0).toInt), MythDateTime.fromIso(parts(1)))
      case "DELETE" =>
        val parts = split(1).substring(name.length + 7).split(' ')
        RecordingListDeleteEvent(ChanId(parts(0).toInt), MythDateTime.fromIso(parts(1)))
      case "UPDATE" =>
        RecordingListUpdateEvent(BackendProgram(split.drop(2)))
      case _ => unknownEvent(name, split)
    }
  }

  def parseUpdateFileSize(name: String, split: Array[String]): Event = {
    val parts = split(1).split(' ')
    UpdateFileSizeEvent(ChanId(parts(1).toInt), MythDateTime.fromIso(parts(2)), parts(3).toLong)
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
    UnknownEvent(name, split.slice(2, split.length): _*)
}

/*
 * Some BACKEND_MESSAGE examples
 *
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT NET_CTRL_CONNECTED SENDER mythfe1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT NET_CTRL_DISCONNECTED SENDER mythfe2[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT SCREEN_TYPE DESTROYED playbackbox SENDER mythfe1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT SCREEN_TYPE CREATED mythscreentypebusydialog SENDER mythfe1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT CLIENT_CONNECTED HOSTNAME myth1 SENDER myth1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT CLIENT_DISCONNECTED HOSTNAME myth1 SENDER myth1[]:[]empty
 *  BACKEND_MESSAGE[]:[]VIDEO_LIST_NO_CHANGE[]:[]empty
 *  BACKEND_MESSAGE[]:[]RECORDING_LIST_CHANGE UPDATE[]:[]Survivor[]:[]Not Going Down Without a Fight[]:[]Castaways from all three tribes remain, and one will be crowned the Sole Survivor.[]:[]32[]:[]14[]:[]3214[]:[]Reality[]:[]1081[]:[]8-1[]:[]KFMB-DT[]:[]KFMBDT (KFMB-DT)[]:[]1081_20160519030000.mpg[]:[]13323011228[]:[]1463626800[]:[]1463634000[]:[]0[]:[]myth1[]:[]0[]:[]0[]:[]0[]:[]0[]:[]-3[]:[]380[]:[]0[]:[]15[]:[]6[]:[]1463626800[]:[]1463634001[]:[]11583492[]:[]Reality TV[]:[][]:[]EP00367078[]:[]EP003670780116[]:[]76733[]:[]1471849923[]:[]0[]:[]2016-05-18[]:[]Default[]:[]0[]:[]0[]:[]Default[]:[]9[]:[]17[]:[]1[]:[]0[]:[]0[]:[]0
 *  BACKEND_MESSAGE[]:[]GENERATED_PIXMAP[]:[]OK[]:[]1081_2016-05-19T05:00:00Z[]:[]Generated on myth1 in 3.919 seconds, starting at 16:19:33[]:[]2016-10-05T23:19:33Z[]:[]83401[]:[]39773[]:[] <<< base64 data + extra tokens redacted >>>
 *  BACKEND_MESSAGE[]:[]GENERATED_PIXMAP[]:[]OK[]:[]1151_2016-07-30T20:30:00Z[]:[]On Disk[]:[]2016-10-05T23:19:42Z[]:[]87547[]:[]13912[]:[] << base64 data + extra tokens redacted >>
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
 * Format of DOWNLOAD_FILE
 *   UPDATE   <url> <outFile> <bytesReceived> <bytesTotal>
 *   FINISHED <url> <outfile> <fileSize> <errorStringPlaceholder> <errorCode>
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
 *   CLIENT_CONNECTED HOSTNAME %1
 *   CLIENT_DISCONNECTED HOSTNAME %1
 *   SLAVE_CONNECTED HOSTNAME %1
 *   SLAVE_DISCONNECTED HOSTNAME %1
 *   REC_DELETED CHANID %1 STARTTIME %2
 *   AIRTUNES_NEW_CONNECTION
 *   AIRTUNES_DELETE_CONNECTION
 *   AIRPLAY_NEW_CONNECTION
 *   AIRPLAY_DELETE_CONNECTION
 *   LIVETV_STARTED
 *   PLAY_STOPPED
 *   TUNING_SIGNAL_TIMEOUT CARDID %1
 *   MASTER_STARTED
 *   MASTER_SHUTDOWN
 *   SCHEDULER_RAN
 *   NET_CTRL_DISCONNECTED
 *   NET_CTRL_CONNECTED
 *   MYTHFILLDATABASE_RAN
 *   SCREEN_TYPE CREATED %1
 *   SCREEN_TYPE DESTROYED %1
 *   THEME_INSTALLED PATH %1
 */
