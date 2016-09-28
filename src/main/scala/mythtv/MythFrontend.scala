package mythtv

import java.time.{ Duration, Instant }

import connection.myth.FrontendConnection
import model._

// TODO inherit from FrontendConnection?  would mean those public methods are exposed here...
//      Liskov substitution principle applies to this decision?

class MythFrontend(val host: String) extends Frontend with FrontendOperations {
  import MythFrontend._

  private[this] val conn = new FrontendConnection(host, DEFAULT_PORT)

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

  // memory type -> bytes available [what units?]  TODO use ByteCount?
  def memoryStats: Map[String, Long] = {
    val res = conn.sendCommand("query memstats").getOrElse("")
    val data = res split "\\s+" map (_.toLong)
    val keys = Array("totalmem", "freemem", "totalswap", "freeswap")
    (keys zip data).toMap
  }

  def currentTime: Instant = {
    val res = conn.sendCommand("query time").getOrElse("")
    Instant.parse(res)
  }

  lazy val jump = new Jumper      // TODO is it good practice to expose these lazy vals directly to the API?
  lazy val key = new KeySender

  type JumpPoint = String

  // TODO should (can) this be an object? or private class?
  class Jumper extends PartialFunction[JumpPoint, Boolean] {
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

  type KeyName = String

  class KeySender extends PartialFunction[KeyName, Boolean] {
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

  /*******
   frontend http server methods (port 6547)
   ******/

  def screenshot(format: String, width: Int, height: Int): Array[Byte] = ???
}

object MythFrontend {
  final val DEFAULT_PORT: Int = 6546
}
