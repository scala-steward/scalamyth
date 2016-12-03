package mythtv
package connection
package myth

import java.net.URI
import java.time.Instant

import model._
import model.EnumTypes.RecStatus
import util.{ Base64String, ByteCount, Crc16, DecimalByteCount, MythDateTime, URIFactory }
import data.BackendLiveTvChain

sealed trait Event

sealed trait SystemEvent extends Event {
  def sender: String
}

object Event {
  case class  AskRecordingEvent(cardId: CaptureCardId, timeUntil: Int, hasRec: Boolean, hasLaterShowing: Boolean, rec: Recordable) extends Event
  case object ClearSettingsCacheEvent extends Event
  case class  CommflagRequestEvent(chanId: ChanId, recStartTs: MythDateTime) extends Event
  case class  CommflagStartEvent(chanId: ChanId, recStartTs: MythDateTime) extends Event
  case class  DoneRecordingEvent(cardId: CaptureCardId, secondsSinceStart: Int, framesWritten: Long) extends Event
  case class  DownloadFileFinishedEvent(sourceUri: URI, targetUri: URI, fileSize: ByteCount, errString: String, errCode: Int) extends Event
  case class  DownloadFileUpdateEvent(sourceUri: URI, targetUri: URI, bytesReceived: ByteCount, bytesTotal: ByteCount) extends Event
  case class  FileClosedEvent(fileName: String) extends Event
  case class  FileWrittenEvent(fileName: String, fileSize: ByteCount) extends Event
  case class  GeneratedPixmapEvent(chanId: ChanId, startTime: MythDateTime, message: String, finishTime: Instant, size: ByteCount, crc: Crc16, data: Base64String, tokens: List[String]) extends Event
  case class  GeneratedPixmapFailEvent(chanId: ChanId, startTime: MythDateTime, message: String, tokens: List[String]) extends Event
  case class  HousekeeperRunningEvent(hostName: String, tag: String, lastRunTime: Instant) extends Event
  case class  LiveTvChainUpdateEvent(chainId: LiveTvChainId, maxPos: Int, chain: List[LiveTvChain]) extends Event
  case class  RecordingListAddEvent(chanId: ChanId, recStartTs: MythDateTime) extends Event
  case object RecordingListChangeEmptyEvent extends Event
  case class  RecordingListDeleteEvent(chanId: ChanId, recStartTs: MythDateTime) extends Event
  case class  RecordingListUpdateEvent(rec: Recording) extends Event
  case object ScheduleChangeEvent extends Event
  case class  SignalEvent(cardId: CaptureCardId, values: Map[String, SignalMonitorValue]) extends Event
  case class  SignalMessageEvent(cardId: CaptureCardId, message: String) extends Event
  case class  UpdateFileSizeEvent(chanId: ChanId, recStartTs: MythDateTime, size: ByteCount) extends Event
  case class  VideoListChangeEvent(changes: Map[String, Set[VideoId]]) extends Event
  case object VideoListNoChangeEvent extends Event
  case class  UnknownEvent(name: String, body: String*) extends Event
  case object NoMoreEvents extends Event  // poison pill
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
  case class PlayStoppedEvent(hostName: String, chanId: ChanId, startTime: MythDateTime, sender: String) extends SystemEvent
  case class PlayStoppedNoProgramEvent(sender: String) extends SystemEvent
  case class PlayUnpausedEvent(hostName: String, chanId: ChanId, startTime: MythDateTime, sender: String) extends SystemEvent
  case class RecordingDeletedEvent(chanId: ChanId, recStartTs: MythDateTime, sender: String) extends SystemEvent
  case class RecordingExpiredEvent(hostName: String, chanId: ChanId, recStartTs: MythDateTime, sender: String) extends SystemEvent
  case class RecordingFinishedEvent(cardId: CaptureCardId, chanId: ChanId, recStartTs: MythDateTime, status: RecStatus, sender: String) extends SystemEvent
  case class RecordingStartedEvent(cardId: CaptureCardId, chanId: ChanId, recStartTs: MythDateTime, status: RecStatus, sender: String) extends SystemEvent
  case class RecordingStartedWritingEvent(cardId: CaptureCardId, chanId: ChanId, recStartTs: MythDateTime, status: RecStatus, sender: String) extends SystemEvent
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

private[myth] trait EventParser {
  def parse(rawEvent: BackendEventResponse): Event
}

private class EventParserImpl extends EventParser with MythProtocolSerializer {
  import Event._
  import SystemEvent._

  protected implicit val programInfoSerializer = ProgramInfoSerializerGeneric
  protected implicit val liveTvChainSerializer = LiveTvChainSerializerGeneric

  private val SystemEventPattern = """SYSTEM_EVENT ([^ ]*) (?:(.*) )?SENDER (.+)""".r

  private val HostnamePattern   = """HOSTNAME (.+)""".r
  private val PlayEventPattern  = """HOSTNAME (.+) CHANID (.+) STARTTIME (.+)""".r
  private val RecDeletedPattern = """CHANID (.+) STARTTIME (.+)""".r
  private val RecEventPattern   = """CARDID (.+) CHANID (.+) STARTTIME (.+) RECSTATUS (.+)""".r
  private val RecPendingPattern = """SECS (.+) CARDID (.+) CHANID (.+) STARTTIME (.+) RECSTATUS (.+)""".r

  private val ScreenCreatedPattern   = """CREATED (.+)""".r
  private val ScreenDestroyedPattern = """DESTROYED (.+)""".r
  private val ThemeInstalledPattern  = """PATH (.+)""".r
  private val TuningTimeoutPattern   = """CARDID (.+)""".r

  def parse(rawEvent: BackendEventResponse): Event = {
    val split = rawEvent.split
    val name = split(1).takeWhile(_ != ' ')
    name match {
      case "SYSTEM_EVENT"          => parseSystemEvent(name, split)
      case "ASK_RECORDING"         => parseAskRecording(name, split)
      case "CLEAR_SETTINGS_CACHE"  => ClearSettingsCacheEvent
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
      case "SIGNAL"                => parseSignal(name, split)
      case "UPDATE_FILE_SIZE"      => parseUpdateFileSize(name, split)
      case "VIDEO_LIST_CHANGE"     => parseVideoListChange(name, split)
      case "VIDEO_LIST_NO_CHANGE"  => VideoListNoChangeEvent
      case "NO_MORE_EVENTS"        => NoMoreEvents
      case _ => unknownEvent(name, split)
    }
  }

  def parseSystemEvent(name: String, split: Array[String]): Event = {
    split(1) match {
      case SystemEventPattern(evt, body, sender) => evt match {
        case "AIRPLAY_DELETE_CONNECTION"  => AirPlayDeleteConnectionEvent(sender)
        case "AIRPLAY_NEW_CONNECTION"     => AirPlayNewConnectionEvent(sender)
        case "AIRTUNES_DELETE_CONNECTION" => AirTunesDeleteConnectionEvent(sender)
        case "AIRTUNES_NEW_CONNECTION"    => AirTunesNewConnectionEvent(sender)
        case "CLIENT_CONNECTED"           => hostnameEvent(ClientConnectedEvent, evt, body, sender)
        case "CLIENT_DISCONNECTED"        => hostnameEvent(ClientDisconnectedEvent, evt, body, sender)
        case "LIVETV_STARTED"             => LiveTvStartedEvent(sender)
        case "MASTER_SHUTDOWN"            => MasterShutdownEvent(sender)
        case "MASTER_STARTED"             => MasterStartedEvent(sender)
        case "MYTHFILLDATABASE_RAN"       => MythfilldatabaseRanEvent(sender)
        case "NET_CTRL_CONNECTED"         => NetControlConnectedEvent(sender)
        case "NET_CTRL_DISCONNECTED"      => NetControlDisconnectedEvent(sender)
        case "PLAY_CHANGED"               => playEvent(PlayChangedEvent, evt, body, sender)
        case "PLAY_PAUSED"                => playEvent(PlayPausedEvent, evt, body, sender)
        case "PLAY_STARTED"               => playEvent(PlayStartedEvent, evt, body, sender)
        case "PLAY_STOPPED"               => playEvent(PlayStoppedEvent, evt, body, sender, fallback = playStoppedNoProgramEvent)
        case "PLAY_UNPAUSED"              => playEvent(PlayUnpausedEvent, evt, body, sender)
        case "REC_DELETED"                => recDeletedEvent(RecordingDeletedEvent, evt, body, sender)
        case "REC_EXPIRED"                => playEvent(RecordingExpiredEvent, evt, body, sender)
        case "REC_FINISHED"               => recEvent(RecordingFinishedEvent, evt, body, sender)
        case "REC_PENDING"                => recPendingEvent(RecordPendingEvent, evt, body, sender)
        case "REC_STARTED"                => recEvent(RecordingStartedEvent, evt, body, sender)
        case "REC_STARTED_WRITING"        => recEvent(RecordingStartedWritingEvent, evt, body, sender)
        case "SCHEDULER_RAN"              => SchedulerRanEvent(sender)
        case "SCREEN_TYPE"                => screenTypeEvent(evt, body, sender)
        case "SETTINGS_CACHE_CLEARED"     => SettingsCacheClearedEvent(sender)
        case "SLAVE_CONNECTED"            => hostnameEvent(SlaveConnectedEvent, evt, body, sender)
        case "SLAVE_DISCONNECTED"         => hostnameEvent(SlaveDisconnectedEvent, evt, body, sender)
        case "THEME_INSTALLED"            => themeInstalledEvent(evt, body, sender)
        case "TUNING_SIGNAL_TIMEOUT"      => tuningSignalTimeoutEvent(evt, body, sender)
        case _ => unknownSystemEvent(evt, body, sender)
      }
      case _ => unknownEvent(name, split)
    }
  }

  def hostnameEvent(factory: (String, String) => SystemEvent, evt: String, body: String, sender: String): SystemEvent = {
    if (body eq null) unknownSystemEvent(evt, body, sender)
    else body match {
      case HostnamePattern(host) => factory(host, sender)
      case _                     => unknownSystemEvent(evt, body, sender)
    }
  }

  // NB play events for non-recordings (e.g. videos/DVD?) will have 0 for chanId, and a synthetic startTime
  def playEvent(factory: (String, ChanId, MythDateTime, String) => SystemEvent, evt: String, body: String, sender: String,
    fallback: (String, String, String) => SystemEvent = unknownSystemEvent): SystemEvent = {
    if (body eq null) fallback(evt, body, sender)
    else body match {
      case PlayEventPattern(host, chanId, startTime) =>
        factory(host, deserialize[ChanId](chanId), MythDateTime(deserialize[Instant](startTime)), sender)
      case _  => unknownSystemEvent(evt, body, sender)
    }
  }

  def playStoppedNoProgramEvent(evt: String, body: String, sender: String): SystemEvent =
    PlayStoppedNoProgramEvent(sender)

  def recEvent(factory: (CaptureCardId, ChanId, MythDateTime, RecStatus, String) => SystemEvent, evt: String, body: String, sender: String): SystemEvent = {
    if (body eq null) unknownSystemEvent(evt, body, sender)
    else body match {
      case RecEventPattern(cardId, chanId, startTime, recStatus) =>
        factory(
          deserialize[CaptureCardId](cardId),
          deserialize[ChanId](chanId),
          MythDateTime(deserialize[Instant](startTime)),
          deserialize[RecStatus](recStatus),
          sender
        )
      case _  => unknownSystemEvent(evt, body, sender)
    }
  }

  def recDeletedEvent(factory: (ChanId, MythDateTime, String) => SystemEvent, evt: String, body: String, sender: String): SystemEvent = {
    if (body eq null) unknownSystemEvent(evt, body, sender)
    else body match {
      case RecDeletedPattern(chanId, startTime) =>
        factory(
          deserialize[ChanId](chanId),
          MythDateTime(deserialize[Instant](startTime)),
          sender
        )
      case _  => unknownSystemEvent(evt, body, sender)
    }
  }

  def recPendingEvent(factory: (Int, CaptureCardId, ChanId, MythDateTime, RecStatus, String) => SystemEvent,
    evt: String, body: String, sender: String): SystemEvent = {
    if (body eq null) unknownSystemEvent(evt, body, sender)
    else body match {
      case RecPendingPattern(seconds, cardId, chanId, startTime, recStatus) =>
        factory(
          deserialize[Int](seconds),
          deserialize[CaptureCardId](cardId),
          deserialize[ChanId](chanId),
          MythDateTime(deserialize[Instant](startTime)),
          deserialize[RecStatus](recStatus),
          sender
        )
      case _  => unknownSystemEvent(evt, body, sender)
    }
  }

  def screenTypeEvent(evt: String, body: String, sender: String): SystemEvent = {
    if (body eq null) unknownSystemEvent(evt, body, sender)
    else body match {
      case ScreenCreatedPattern(screen)   => ScreenCreatedEvent(screen, sender)
      case ScreenDestroyedPattern(screen) => ScreenDestroyedEvent(screen, sender)
      case _                              => unknownSystemEvent(evt, body, sender)
    }
  }

  def themeInstalledEvent(evt: String, body: String, sender: String): SystemEvent = {
    if (body eq null) unknownSystemEvent(evt, body, sender)
    else body match {
      case ThemeInstalledPattern(path) => ThemeInstalledEvent(path, sender)
      case _                           => unknownSystemEvent(evt, body, sender)
    }
  }

  def tuningSignalTimeoutEvent(evt: String, body: String, sender: String): SystemEvent = {
    if (body eq null) unknownSystemEvent(evt, body, sender)
    else body match {
      case TuningTimeoutPattern(cardId) => TuningSignalTimeoutEvent(deserialize[CaptureCardId](cardId), sender)
      case _                            => unknownSystemEvent(evt, body, sender)
    }
  }

  def unknownSystemEvent(name: String, body: String, sender: String): SystemEvent =
    UnknownSystemEvent(name, if (body eq null) "" else body, sender)

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
      case "FINISHED" => DownloadFileFinishedEvent(
        URIFactory(split(2)),
        URIFactory(split(3)),
        DecimalByteCount(deserialize[Long](split(4))),
        split(5),
        deserialize[Int](split(6))
      )
      case "UPDATE" => DownloadFileUpdateEvent(
        URIFactory(split(2)),
        URIFactory(split(3)),
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
    def splitProgramInfoToken(token: String): (ChanId, MythDateTime) = {
      val pi = token.split('_')
      (deserialize[ChanId](pi(0)), MythDateTime(deserialize[Instant](pi(1))))
    }
    split(2) match {
      case "OK" =>
        val (chanId, startTime) = splitProgramInfoToken(split(3))
        val message = split(4)
        val finishTime = deserialize[Instant](split(5))
        val size = DecimalByteCount(deserialize[Long](split(6)))
        val crc = new Crc16(deserialize[Int](split(7)))
        val data = new Base64String(split(8))
        val tokens = split.slice(9, split.length).toList
        GeneratedPixmapEvent(chanId, startTime, message, finishTime, size, crc, data, tokens)
      case "ERROR" =>
        val (chanId, startTime) = splitProgramInfoToken(split(3))
        val message = split(4)
        val tokens = split.slice(5, split.length).toList
        GeneratedPixmapFailEvent(chanId, startTime, message, tokens)
      case _ => unknownEvent(name, split)
    }
  }

  def parseHousekeeperRunning(name: String, split: Array[String]): Event = {
    val parts = split(1).split(' ')
    HousekeeperRunningEvent(parts(1), parts(2), deserialize[Instant](parts(3)))
  }

  def parseLiveTvChain(name: String, split: Array[String]): Event = {
    val parts = split(1).split(' ')
    val chainId = deserialize[LiveTvChainId](parts(2))
    val maxPos = deserialize[Int](split(2))

    val fieldCount = liveTvChainSerializer.fieldCount - 1
    val it = split.iterator drop 2 grouped fieldCount withPartial false map (chainId.id +: _)
    val chain = it map deserialize[LiveTvChain]

    LiveTvChainUpdateEvent(chainId, maxPos, chain.toList)
  }

  def parseRecordingListChange(name: String, split: Array[String]): Event = {
    val body = split(1)
    if (body.length > name.length) {
      body.substring(name.length + 1).takeWhile(_ != ' ') match {
        case "ADD" =>
          val parts = body.substring(name.length + 5).split(' ')
          RecordingListAddEvent(deserialize[ChanId](parts(0)), MythDateTime(deserialize[Instant](parts(1))))
        case "DELETE" =>
          val parts = body.substring(name.length + 8).split(' ')
          RecordingListDeleteEvent(deserialize[ChanId](parts(0)), MythDateTime(deserialize[Instant](parts(1))))
        case "UPDATE" =>
          RecordingListUpdateEvent(deserialize[Recording](split.drop(2)))
        case _ => unknownEvent(name, split)
      }
    }
    else RecordingListChangeEmptyEvent
  }

  implicit object SignalMonitorValueSerializer extends MythProtocolSerializable[SignalMonitorValue] {
    def deserialize(in: String): SignalMonitorValue =
      deserialize(in split MythProtocol.SplitPattern)

    override def deserialize(in: Seq[String]): SignalMonitorValue = {
      val id = in(0)
      val args = in(1) split ' '
      new SignalMonitorValue {
        def name            = id
        def statusName      = args(0)
        def value           = MythProtocol.deserialize[Int](args(1))
        def threshold       = MythProtocol.deserialize[Int](args(2))
        def minValue        = MythProtocol.deserialize[Int](args(3))
        def maxValue        = MythProtocol.deserialize[Int](args(4))
        def timeout         = MythProtocol.deserialize[Int](args(5))
        def isHighThreshold = MythProtocol.deserialize[Boolean](args(6))
        def isValueSet      = MythProtocol.deserialize[Boolean](args(7))
      }
    }

    def serialize(in: SignalMonitorValue): String = ???
  }

  def parseSignal(name: String, split: Array[String]): Event = {
    val parts = split(1) split ' '
    val cardId = deserialize[CaptureCardId](parts(1))
    if (split(2) == "message") {
      SignalMessageEvent(cardId, split(3))
    } else {
      val values = split.iterator drop 2 grouped 2 withPartial false map deserialize[SignalMonitorValue]
      val valueMap = (values map { v => (v.name, v) }).toMap
      SignalEvent(cardId, valueMap)
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
 * Format of SIGNAL
 *   see libs/libmythtv/recorders/signalmonitor.cpp::GetStatusList
 *    plus its card-type-specific subclasses
 *     and
 *   libs/libmythtv/tv_rec.cpp::TuningFrequency
 *
 *  All are instances of SignalMonitorValue:
 *    libs/libmythtv/signalmonitorvalue.h
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
 * RESET_IDLETIME  ??  sent by mythshutdown
 * SHUTDOWN_NOW    ??
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
BACKEND_MESSAGE[]:[]SIGNAL 4[]:[]message[]:[]On known multiplex...

2016-11-14T08:40:36.875 UnknownEvent(LIVETV_CHAIN,WrappedArray(LIVETV_CHAIN UPDATE live-mythtest-atom-2016-11-14T16:40:33Z, 1, 1391, 2016-11-14T16:40:39Z, 2016-11-14T17:00:00Z, 0, myth://192.168.1.123:6543/, DUMMY, 39-1, DVBInput))

2016-11-14T08:40:36.990 UnknownEvent(SIGNAL,WrappedArray(SIGNAL 4, Script Status, script 3 3 0 3 0 1 1, Signal Lock, slock 1 1 0 1 3000 1 1, Signal Power, signal 0 0 0 65535 3000 1 1, Seen PAT, seen_pat 0 1 0 1 0 1 1, Matching PAT, matching_pat 0 1 0 1 0 1 1, Seen MGT, seen_mgt 0 1 0 1 0 1 1, Matching MGT, matching_mgt 0 1 0 1 0 1 1, Seen VCT, seen_vct 0 1 0 1 0 1 1, Matching VCT, matching_vct 0 1 0 1 0 1 1, Signal To Noise, snr 0 0 0 65535 0 1 1, Bit Error Rate, ber 0 65535 0 65535 0 0 1, Uncorrected Blocks, ucb 0 65535 0 65535 0 0 1))
2016-11-14T08:40:37.040 UnknownEvent(SIGNAL,WrappedArray(SIGNAL 4, Script Status, script 3 3 0 3 0 1 1, Signal Lock, slock 1 1 0 1 3000 1 1, Signal Power, signal 0 0 0 65535 3000 1 1, Seen PAT, seen_pat 1 1 0 1 0 1 1, Matching PAT, matching_pat 0 1 0 1 0 1 1, Seen MGT, seen_mgt 1 1 0 1 0 1 1, Matching MGT, matching_mgt 1 1 0 1 0 1 1, Seen VCT, seen_vct 0 1 0 1 0 1 1, Matching VCT, matching_vct 0 1 0 1 0 1 1, Signal To Noise, snr 0 0 0 65535 0 1 1, Bit Error Rate, ber 0 65535 0 65535 0 0 1, Uncorrected Blocks, ucb 0 65535 0 65535 0 0 1))
2016-11-14T08:40:37.091 UnknownEvent(SIGNAL,WrappedArray(SIGNAL 4, Script Status, script 3 3 0 3 0 1 1, Signal Lock, slock 1 1 0 1 3000 1 1, Signal Power, signal 0 0 0 65535 3000 1 1, Seen PAT, seen_pat 1 1 0 1 0 1 1, Matching PAT, matching_pat 1 1 0 1 0 1 1, Seen PMT, seen_pmt 0 1 0 1 0 1 1, Matching PMT, matching_pmt 0 1 0 1 0 1 1, Seen MGT, seen_mgt 1 1 0 1 0 1 1, Matching MGT, matching_mgt 1 1 0 1 0 1 1, Seen VCT, seen_vct 1 1 0 1 0 1 1, Matching VCT, matching_vct 1 1 0 1 0 1 1, Signal To Noise, snr 0 0 0 65535 0 1 1, Bit Error Rate, ber 0 65535 0 65535 0 0 1, Uncorrected Blocks, ucb 0 65535 0 65535 0 0 1))
2016-11-14T08:40:37.141 UnknownEvent(SIGNAL,WrappedArray(SIGNAL 4, Script Status, script 3 3 0 3 0 1 1, Signal Lock, slock 1 1 0 1 3000 1 1, Signal Power, signal 0 0 0 65535 3000 1 1, Seen PAT, seen_pat 1 1 0 1 0 1 1, Matching PAT, matching_pat 1 1 0 1 0 1 1, Seen PMT, seen_pmt 1 1 0 1 0 1 1, Matching PMT, matching_pmt 1 1 0 1 0 1 1, Seen MGT, seen_mgt 1 1 0 1 0 1 1, Matching MGT, matching_mgt 1 1 0 1 0 1 1, Seen VCT, seen_vct 1 1 0 1 0 1 1, Matching VCT, matching_vct 1 1 0 1 0 1 1, Signal To Noise, snr 0 0 0 65535 0 1 1, Bit Error Rate, ber 0 65535 0 65535 0 0 1, Uncorrected Blocks, ucb 0 65535 0 65535 0 0 1))
2016-11-14T08:40:37.191 UnknownEvent(SIGNAL,WrappedArray(
   SIGNAL 4,
   Script Status,      script 3     3 0     3    0 1 1,
   Signal Lock,         slock 1     1 0     1 3000 1 1,
   Signal Power,       signal 0     0 0 65535 3000 1 1,
   Seen PAT,         seen_pat 1     1 0     1    0 1 1,
   Matching PAT, matching_pat 1     1 0     1    0 1 1,
   Seen PMT,         seen_pmt 1     1 0     1    0 1 1,
   Matching PMT, matching_pmt 1     1 0     1    0 1 1,
   Seen MGT,         seen_mgt 1     1 0     1    0 1 1,
   Matching MGT, matching_mgt 1     1 0     1    0 1 1,
   Seen VCT,         seen_vct 1     1 0     1    0 1 1,
   Matching VCT, matching_vct 1     1 0     1    0 1 1,
   Signal To Noise,       snr 0     0 0 65535    0 1 1,
   Bit Error Rate,        ber 0 65535 0 65535    0 0 1,
   Uncorrected Blocks,    ucb 0 65535 0 65535    0 0 1
   ))

2016-11-14T08:40:37.195 UnknownEvent(LIVETV_CHAIN,WrappedArray(LIVETV_CHAIN UPDATE live-mythtest-atom-2016-11-14T16:40:33Z, 1, 1391, 2016-11-14T16:40:39Z, 2016-11-14T16:40:39Z, 0, myth://192.168.1.123:6543/, DUMMY, 39-1, DVBInput))

2016-11-14T08:40:37.260 UnknownEvent(LIVETV_CHAIN,WrappedArray(LIVETV_CHAIN UPDATE live-mythtest-atom-2016-11-14T16:40:33Z, 2, 1391, 2016-11-14T16:40:39Z, 2016-11-14T16:40:39Z, 0, myth://192.168.1.123:6543/, DUMMY, 39-1, DVBInput, 1391, 2016-11-14T16:40:40Z, 2016-11-14T17:00:00Z, 1, myth://192.168.1.123:6543/, DVB, 39-1, DVBInput))

2016-11-14T08:42:39.211 UnknownEvent(LIVETV_CHAIN,WrappedArray(LIVETV_CHAIN UPDATE live-mythtest-atom-2016-11-14T16:40:33Z, 2, 1391, 2016-11-14T16:40:40Z, 2016-11-14T17:00:00Z, 1, myth://192.168.1.123:6543/, DVB, 39-1, DVBInput))

 */
