package mythtv
package connection
package myth

import java.net.URI
import java.time.{ Duration, Instant }

import model.{ ChanId, ChannelNumber }
import util.{ BinaryByteCount, ByteCount, MythDateTime }
import MythFrontend.{ JumpPoint, KeyName }  // TODO TEMP relocate as appropriate

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

  def jump: PartialFunction[JumpPoint, Boolean]  // TODO extend to expose 'points'
  def key: PartialFunction[KeyName, Boolean]     // TODO extend to expose 'special'

  def sendMessage(message: String): Unit = ???
  def sendNotification(message: String): Unit = ???

  // TODO more API stuff, especially rich play XXXXX

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
  def playSeekTo(): Unit = ???   // TODO parameter is an HH:MM:SS offset

  def playSpeed(): Unit = ???  // TODO various parameters (normal, 1/2, 1/3, 1/4, 1/8)

  def playPause(): Unit
  def playStop(): Unit

  def playVolume(volumePercent: Int): Unit

  def playSubtitles(): Unit = ???  // TOOD: parameter one (or more?) subtitle track numbers

  def queryLocation: String
  def queryVersion: String  // TODO parse result into separate components?
  def queryVolume: Int

  def queryLoad: List[Double]
  def queryMemStats: Map[String, ByteCount]
  def queryTime: Instant
  def queryUptime: Duration
}

trait FrontendNetworkControlLike {
  self: FrontendProtocol with FrontendNetworkControl =>

  // TODO need error handling around return types

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

  def playPause(): Unit = {
    sendCommand("play speed pause")
  }

  def playStop(): Unit = {
    sendCommand("play stop")
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

  def jump: PartialFunction[JumpPoint, Boolean] = Jumper
  def key: PartialFunction[KeyName, Boolean] = KeySender

  private object Jumper extends PartialFunction[JumpPoint, Boolean] {
    lazy val points: Map[String, String] = retrieveJumpPoints
    private val helpPat = """(\w+)[ ]+- ([\w /,]+)""".r

    def isDefinedAt(point: JumpPoint): Boolean = points contains point

    def apply(point: JumpPoint): Boolean = {
      if (isDefinedAt(point)) sendCommand("jump " + point).getOrElse("") == "OK"
      else false
    }

    private def retrieveJumpPoints: Map[JumpPoint, String] = {
      val help = sendCommand("help jump").getOrElse("")
      (for (m <- helpPat findAllMatchIn help) yield (m group 1, m group 2)).toMap
    }
  }

  private object KeySender extends PartialFunction[KeyName, Boolean] {
    lazy val special: Set[KeyName] = retrieveSpecialKeys

    private val alphanum: Map[String, Char] = (('0' to '9') ++ ('A' to 'Z') ++ ('a' to 'z')
      map (c => (String.valueOf(c), c))).toMap

    def isDefinedAt(key: KeyName): Boolean =
      (alphanum contains key) || (special contains key)

    def apply(key: KeyName): Boolean = {
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

trait FrontendNetworkControlConnection extends FrontendConnection with FrontendNetworkControl

object FrontendNetworkControlConnection {
  def apply(
    host: String,
    port: Int = FrontendConnection.DefaultPort,
    timeout: Int = FrontendConnection.DefaultTimeout
  ): FrontendNetworkControlConnection = {
    new FrontendNetworkControlConnectionImpl(host, port, timeout)
  }
}

private class FrontendNetworkControlConnectionImpl(host: String, port: Int, timeout: Int)
  extends FrontendConnectionImpl(host, port, timeout)
     with FrontendNetworkControlConnection
     with FrontendNetworkControlLike
