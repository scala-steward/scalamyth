package mythtv
package model

import java.time.Instant

import util.{ BitmaskEnum, LooseEnum }
import EnumTypes.MythLogLevel

trait LogMessage {
  def hostName: String
  def application: String
  def pid: Int
  def tid: Int
  def thread: String
  def fileName: String
  def lineNum: Int
  def function: String
  def messageTime: Instant
  def level: MythLogLevel
  def message: String

  override def toString: String =
    s"$messageTime $hostName $application[$pid] ${level.toString.head} $thread $fileName:$lineNum ($function) $message"
}

object MythLogLevel extends LooseEnum /* with EnumWithDescription[MythLogLevel#Value] */{
  type MythLogLevel = Value
  val Any        = Value(-1)
  val Emerg      = Value(0)
  val Alert      = Value(1)
  val Crit       = Value(2)
  val Err        = Value(3)
  val Warning    = Value(4)
  val Notice     = Value(5)
  val Info       = Value(6)
  val Debug      = Value(7)
  val Unknown    = Value(8)

  def description(value: Value): String = value.toString.toLowerCase
  def withDescription(description: String): Value =
    withName(description(0).toUpper + description.substring(1))
}

// To get strings used for verbose arguments, convert name to lowercase
object MythVerboseLevel extends BitmaskEnum[Long] {
  type MythVerboseLevel = Base
  val None       =  Mask(0)
  val All        =  Mask(0xffffffffffffffffL)
  val Most       =  Mask(0xffffffff3ffeffffL)
  val General    = Value(0x0000000000000002L)
  val Record     = Value(0x0000000000000004L)
  val Playback   = Value(0x0000000000000008L)
  val Channel    = Value(0x0000000000000010L)
  val OSD        = Value(0x0000000000000020L)
  val File       = Value(0x0000000000000040L)
  val Schedule   = Value(0x0000000000000080L)
  val Network    = Value(0x0000000000000100L)
  val CommFlag   = Value(0x0000000000000200L)
  val Audio      = Value(0x0000000000000400L)
  val LibAV      = Value(0x0000000000000800L)
  val JobQueue   = Value(0x0000000000001000L)
  val Siparser   = Value(0x0000000000002000L)
  val EIT        = Value(0x0000000000004000L)
  val VBI        = Value(0x0000000000008000L)
  val Database   = Value(0x0000000000010000L)
  val DSMCC      = Value(0x0000000000020000L)
  val MHEG       = Value(0x0000000000040000L)
  val UPNP       = Value(0x0000000000080000L)
  val Socket     = Value(0x0000000000100000L)
  val XMLTV      = Value(0x0000000000200000L)
  val DVBCAM     = Value(0x0000000000400000L)
  val Media      = Value(0x0000000000800000L)
  val Idle       = Value(0x0000000001000000L)
  val ChanScan   = Value(0x0000000002000000L)
  val GUI        = Value(0x0000000004000000L)
  val System     = Value(0x0000000008000000L)
  val Timestamp  = System                         // alias for System
  val Process    = Value(0x0000000100000000L)
  val Frame      = Value(0x0000000200000000L)
  val RplxQueue  = Value(0x0000000400000000L)
  val Decode     = Value(0x0000000800000000L)
  val GPU        = Value(0x0000004000000000L)
  val GPUAudio   = Value(0x0000008000000000L)
  val GPUVideo   = Value(0x0000010000000000L)
  val RefCount   = Value(0x0000020000000000L)
}
