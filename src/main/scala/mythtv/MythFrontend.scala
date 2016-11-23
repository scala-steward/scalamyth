package mythtv

import java.time.{ Duration, Instant }

import model._
import util.ByteCount
import connection.myth.FrontendNetworkControlConnection

class MythFrontend(val host: String) extends Frontend with FrontendOperations {
  private[this] val conn = FrontendNetworkControlConnection(host)

  def close(): Unit = {
    conn.close()
  }

  def play(media: PlayableMedia): Boolean = media.playOnFrontend(this)

  def uptime: Duration = conn.queryUptime

  def loadAverages: List[Double] = conn.queryLoad

  def memoryStats: Map[String, ByteCount] = conn.queryMemStats

  def currentTime: Instant = conn.queryTime

  def jump = conn.jump

  def key = conn.key

  /* frontend http server methods (port 6547) */
  def screenshot(format: String, width: Int, height: Int): Array[Byte] = ???
}

object MythFrontend {
  type KeyName = String
  type JumpPoint = String
}
