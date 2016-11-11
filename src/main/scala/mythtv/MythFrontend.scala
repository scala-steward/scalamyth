package mythtv

import java.time.{ Duration, Instant }

import connection.myth.FrontendConnection
import model._
import util.{ ByteCount, BinaryByteCount }

// TODO inherit from FrontendConnection?  would mean those public methods are exposed here...
//      Liskov substitution principle applies to this decision?

class MythFrontend(val host: String) extends Frontend with FrontendOperations {
  import MythFrontend._

  private[this] val conn = new FrontendConnection(host)

  def close() = {
    conn.disconnect()
  }

  def play(media: PlayableMedia): Boolean = media.playOnFrontend(this)

  def uptime: Duration = {
    val res = conn.sendCommand("query uptime").getOrElse("")
    Duration.ofSeconds(res.toLong)
  }

  def loadAverages: List[Double] = {
    val res = conn.sendCommand("query load").getOrElse("")
    (res split "\\s+" map (_.toDouble)).toList
  }

  // memory type -> bytes available
  def memoryStats: Map[String, ByteCount] = {
    val res = conn.sendCommand("query memstats").getOrElse("")
    val data = res split "\\s+" map (v => BinaryByteCount(v.toLong * 1024L * 1024L))
    val keys = Array("totalmem", "freemem", "totalswap", "freeswap")
    (keys zip data).toMap
  }

  def currentTime: Instant = {
    val res = conn.sendCommand("query time").getOrElse("")
    Instant.parse(res)
  }

  def jump = Jumper
  def key = KeySender

  object Jumper extends PartialFunction[JumpPoint, Boolean] {
    lazy val points: Map[String, String] = retrieveJumpPoints
    private val helpPat = """(\w+)[ ]+- ([\w /,]+)""".r

    def isDefinedAt(point: JumpPoint): Boolean = points contains point

    def apply(point: JumpPoint): Boolean = {
      if (isDefinedAt(point)) conn.sendCommand("jump " + point).getOrElse("") == "OK"
      else false
    }

    private def retrieveJumpPoints: Map[JumpPoint, String] = {
      val help = conn.sendCommand("help jump").getOrElse("")
      (for (m <- helpPat findAllMatchIn help) yield (m group 1, m group 2)).toMap
    }
  }

  object KeySender extends PartialFunction[KeyName, Boolean] {
    lazy val special: Set[KeyName] = retrieveSpecialKeys

    private val alphanum: Map[String, Char] = (('0' to '9') ++ ('A' to 'Z') ++ ('a' to 'z')
      map (c => (String.valueOf(c), c))).toMap

    def isDefinedAt(key: KeyName): Boolean =
      (alphanum contains key) || (special contains key)

    def apply(key: KeyName): Boolean = {
      if (isDefinedAt(key)) conn.sendCommand("key " + key).getOrElse("") == "OK"
      else false
    }

    private def retrieveSpecialKeys: Set[KeyName] = {
      val help = conn.sendCommand("help key").getOrElse("")
      val specialList = (help split "\r\n")(4)  // skip four lines of preamble
      (specialList split ", ").toSet
    }
  }

  /*
   * frontend http server methods (port 6547)
   */

  def screenshot(format: String, width: Int, height: Int): Array[Byte] = ???
}

object MythFrontend {
  type KeyName = String
  type JumpPoint = String
}
