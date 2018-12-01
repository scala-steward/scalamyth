// SPDX-License-Identifier: LGPL-2.1-only
/*
 * log.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package model

import java.time.Instant

import util.{ LongBitmaskEnum, LooseEnum }
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
  final val Any        = Value(-1)
  final val Emerg      = Value(0)
  final val Alert      = Value(1)
  final val Crit       = Value(2)
  final val Err        = Value(3)
  final val Warning    = Value(4)
  final val Notice     = Value(5)
  final val Info       = Value(6)
  final val Debug      = Value(7)
  final val Unknown    = Value(8)

  def description(value: Value): String = value.toString.toLowerCase
  def withDescription(description: String): Value = withName(description.capitalize)
}

// To get strings used for verbose arguments, convert name to lowercase
object MythVerboseLevel extends LongBitmaskEnum {
  type MythVerboseLevel = Base
  final val None       =  Mask(0)
  final val All        =  Mask(0xffffffffffffffffL)
  final val Most       =  Mask(0xffffffff3ffeffffL)
  final val General    = Value(0x0000000000000002L)
  final val Record     = Value(0x0000000000000004L)
  final val Playback   = Value(0x0000000000000008L)
  final val Channel    = Value(0x0000000000000010L)
  final val OSD        = Value(0x0000000000000020L)
  final val File       = Value(0x0000000000000040L)
  final val Schedule   = Value(0x0000000000000080L)
  final val Network    = Value(0x0000000000000100L)
  final val CommFlag   = Value(0x0000000000000200L)
  final val Audio      = Value(0x0000000000000400L)
  final val LibAV      = Value(0x0000000000000800L)
  final val JobQueue   = Value(0x0000000000001000L)
  final val Siparser   = Value(0x0000000000002000L)
  final val EIT        = Value(0x0000000000004000L)
  final val VBI        = Value(0x0000000000008000L)
  final val Database   = Value(0x0000000000010000L)
  final val DSMCC      = Value(0x0000000000020000L)
  final val MHEG       = Value(0x0000000000040000L)
  final val UPNP       = Value(0x0000000000080000L)
  final val Socket     = Value(0x0000000000100000L)
  final val XMLTV      = Value(0x0000000000200000L)
  final val DVBCAM     = Value(0x0000000000400000L)
  final val Media      = Value(0x0000000000800000L)
  final val Idle       = Value(0x0000000001000000L)
  final val ChanScan   = Value(0x0000000002000000L)
  final val GUI        = Value(0x0000000004000000L)
  final val System     = Value(0x0000000008000000L)
  final val Timestamp  = System                         // alias for System
  final val Process    = Value(0x0000000100000000L)
  final val Frame      = Value(0x0000000200000000L)
  final val RplxQueue  = Value(0x0000000400000000L)
  final val Decode     = Value(0x0000000800000000L)
  final val GPU        = Value(0x0000004000000000L)
  final val GPUAudio   = Value(0x0000008000000000L)
  final val GPUVideo   = Value(0x0000010000000000L)
  final val RefCount   = Value(0x0000020000000000L)
}
