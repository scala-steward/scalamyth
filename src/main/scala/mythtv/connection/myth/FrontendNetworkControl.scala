package mythtv
package connection
package myth

import java.net.URI
import java.time.{ Duration, Instant }

import model.{ ChanId, ChannelNumber }
import util.{ BinaryByteCount, ByteCount, MythDateTime }
import MythFrontend.{ JumpPoint, KeyName }  // TODO TEMP relocate as appropriate
import EnumTypes.PlaybackSpeed

object PlaybackSpeed extends Enumeration {
  type PlaybackSpeed = Value
  final val Normal    = Value("1x")
  final val OneHalf   = Value("1/2x")
  final val OneThird  = Value("1/3x")
  final val OneFourth = Value("1/4x")
  final val OneEighth = Value("1/8x")
}

trait FrontendJumper extends PartialFunction[JumpPoint, Boolean] {
  def points: Map[String, String]
}

trait FrontendKeySender extends PartialFunction[KeyName, Boolean] {
  def special: Set[KeyName]
}

trait FrontendNetworkControl {
  /*
    * Commands:
    *
    * help
    * jump JUMPPOINT
    * key { LETTER | NUMBER | CODE }
    * play  < lots of subcommands >
    * query < lots of subcommands >
    * set { verbose MASK }
    * screenshot [WxH]
    * message
    * notification
    * exit
    */

  def jump: FrontendJumper
  def key: FrontendKeySender

  def sendMessage(message: String): Unit
  def sendNotification(message: String): Unit

  def playChannelUp(): Unit
  def playChannelDown(): Unit
  def playChannel(chanId: ChanId): Unit
  def playChannel(channum: ChannelNumber): Unit

  def playProgram(chanId: ChanId, startTime: MythDateTime): Unit
  def resumeProgram(chanId: ChanId, startTime: MythDateTime): Unit

  def playFile(fileName: String): Unit
  def playFile(mythUri: URI): Unit

  def playSeekBeginning(): Unit
  def playSeekBackward(): Unit
  def playSeekForward(): Unit

  def playSeekTo(timeOffset: Duration): Unit
  def playSeekTo(hours: Int = 0, minutes: Int = 0, seconds: Int = 0): Unit

  def playSpeed(speed: PlaybackSpeed): Unit
  def playSpeed(speed: Float): Unit

  def playPause(): Unit
  def playStop(): Unit

  def playVolume(volumePercent: Int): Unit

  // FIXME UPSTREAM the track number necessary to supply here doesn't necessaily seem to align
  // with those given in the response to query location
  def playSubtitles(track: Int): Unit

  def queryLocation: String
  def queryVersion: String  // TODO parse result into separate components?
  def queryVolume: Int

  def queryLoad: List[Double]
  def queryMemStats: Map[String, ByteCount]
  def queryTime: Instant
  def queryUptime: Duration
}

private[myth] trait FrontendNetworkControlLike {
  self: FrontendProtocol with FrontendNetworkControl =>

  // TODO need error handling around return types

  def sendMessage(message: String): Unit = {
    sendCommand("message " + message)
  }

  def sendNotification(message: String): Unit = {
    sendCommand("notification " + message)
  }

  def playChannelUp(): Unit ={
    sendCommand("play channel up")
  }

  def playChannelDown(): Unit = {
    sendCommand("play channel down")
  }

  def playChannel(chanId: ChanId): Unit = {
    sendCommand("play chanid " + chanId.id)
  }

  def playChannel(channum: ChannelNumber): Unit = {
    sendCommand("play channel " + channum.num)
  }

  def playProgram(chanId: ChanId, startTime: MythDateTime): Unit = {
    sendCommand("play program " + chanId.id + " " + startTime.toNaiveIsoFormat)
  }

  def resumeProgram(chanId: ChanId, startTime: MythDateTime): Unit = {
    sendCommand("play program " + chanId.id + " " + startTime.toNaiveIsoFormat + " resume")
  }

  def playFile(fileName: String): Unit = {
    sendCommand("play file " + fileName)
  }

  def playFile(mythUri: URI): Unit = {
    require(mythUri.getScheme == "myth")
    sendCommand("play file " + mythUri)
  }

  def playSeekBeginning(): Unit = {
    sendCommand("play seek beginning")
  }

  def playSeekBackward(): Unit = {
    sendCommand("play seek backward")
  }

  def playSeekForward(): Unit = {
    sendCommand("play seek forward")
  }

  def playSeekTo(timeOffset: Duration): Unit = {
    val duration = """PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)(?:[.]\d+)?S)?""".r
    val (hours, mins, secs) = timeOffset.toString match {
      case duration(h, m, s) => (
        if (h eq null) 0 else h.toInt,
        if (m eq null) 0 else m.toInt,
        if (s eq null) 0 else s.toInt
      )
    }
    playSeekTo(hours, mins, secs)
  }

  def playSeekTo(hours: Int, minutes: Int, seconds: Int): Unit = {
    sendCommand(f"play seek $hours%02d:$minutes%02d:$seconds%02d")
  }

  def playSpeed(speed: PlaybackSpeed): Unit = {
    sendCommand("play speed " + speed)
  }

  def playSpeed(speed: Float): Unit = {
    sendCommand("play speed " + speed + "x")
  }

  def playPause(): Unit = {
    sendCommand("play speed pause")
  }

  def playStop(): Unit = {
    sendCommand("play stop")
  }

  def playSubtitles(track: Int): Unit = {
    sendCommand("play subtitles " + track)
  }

  def playVolume(volumePercent: Int): Unit = {
    require(volumePercent >= 0 && volumePercent <= 100)
    sendCommand("play volume " + volumePercent + "%")
  }

  def queryVolume: Int = {
    val volume = """(\d+)%""".r
    val result = sendCommand("query volume").getOrElse("")
    result match { case volume(vol) => vol.toInt }
  }

  def queryLocation: String = {
    sendCommand("query location").getOrElse("")
  }

  def queryVersion: String = {
    sendCommand("query version").getOrElse("")
  }

  def queryTime: Instant = {
    val res = sendCommand("query time").getOrElse("")
    Instant.parse(res)
  }

  def queryLoad: List[Double] = {
    val res = sendCommand("query load").getOrElse("")
    (res split "\\s+" map (_.toDouble)).toList
  }

  // memory type -> bytes available
  def queryMemStats: Map[String, ByteCount] = {
    val res = sendCommand("query memstats").getOrElse("")
    val data = res split "\\s+" map (v => BinaryByteCount(v.toLong * 1024L * 1024L))
    val keys = Array("totalmem", "freemem", "totalswap", "freeswap")
    (keys zip data).toMap
  }

  def queryUptime: Duration = {
    val res = sendCommand("query uptime").getOrElse("")
    Duration.ofSeconds(res.toLong)
  }

  def jump: FrontendJumper = Jumper

  def key: FrontendKeySender = KeySender

  private object Jumper extends FrontendJumper {
    lazy val points: Map[String, String] = retrieveJumpPoints
    private val helpPat = """(\w+)[ ]+- ([\w /,]+)""".r

    override def isDefinedAt(point: JumpPoint): Boolean = points contains point

    override def apply(point: JumpPoint): Boolean = {
      if (isDefinedAt(point)) sendCommand("jump " + point).getOrElse("") == "OK"
      else false
    }

    private def retrieveJumpPoints: Map[JumpPoint, String] = {
      val help = sendCommand("help jump").getOrElse("")
      (for (m <- helpPat findAllMatchIn help) yield (m group 1, m group 2)).toMap
    }
  }

  private object KeySender extends FrontendKeySender {
    lazy val special: Set[KeyName] = retrieveSpecialKeys

    private val alphanum: Map[String, Char] = (('0' to '9') ++ ('A' to 'Z') ++ ('a' to 'z')
      map (c => (String.valueOf(c), c))).toMap

    override def isDefinedAt(key: KeyName): Boolean =
      (alphanum contains key) || (special contains key)

    override def apply(key: KeyName): Boolean = {
      if (isDefinedAt(key)) sendCommand("key " + key).getOrElse("") == "OK"
      else false
    }

    private def retrieveSpecialKeys: Set[KeyName] = {
      val help = sendCommand("help key").getOrElse("")
      val specialList = (help split "\r\n")(4)  // skip four lines of preamble
      (specialList split ", ").toSet
    }
  }
}
