package mythtv
package connection
package myth

import mythtv.model.ChanId
import mythtv.util.MythDateTime

trait MythProtocolSerializer[A] {
  def serialize(in: A): String
  def deserialize(in: String): A
}

object IntSerializer extends MythProtocolSerializer[Int] {
  def serialize(in: Int): String = in.toString
  def deserialize(in: String): Int = in.toInt
}

object LongSerializer extends MythProtocolSerializer[Long] {
  def serialize(in: Long): String = in.toString
  def deserialize(in: String): Long = in.toLong
}

object BooleanSerializer extends MythProtocolSerializer[Boolean] {
  def serialize(in: Boolean): String = if (in) "1" else "0"
  def deserialize(in: String): Boolean = in.toBoolean  // handles "True" and "False" (case-insensitive)
}

object ChanIdSerializer extends MythProtocolSerializer[ChanId] {
  def serialize(in: ChanId): String = in.id.toString
  def deserialize(in: String): ChanId = ChanId(in.toInt)
}

object MythDateTimeSerializer extends MythProtocolSerializer[MythDateTime] {
  def serialize(in: MythDateTime): String = in.toTimestamp.toString
  def deserialize(in: String): MythDateTime = MythDateTime.fromTimestamp(in.toLong)
}

// NB "Program" serializer may vary across protocol versions
