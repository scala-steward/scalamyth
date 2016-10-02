package mythtv

import java.time.{ LocalDateTime, Instant, ZoneId, ZoneOffset }
import java.time.format.DateTimeFormatter

package object util {

  // TODO: do we need to/from ISO (w,w/o zone offset, ISO_DATE_TIME, parseBest() ?)
  // TODO: do we need to/from RFC format (RFC_1123_DATE_TIME) ?

  // TODO: do we want to use LocalDateTime or Instant as our representation of Myth UTC time?

  implicit class MythDateTime(val localDateTime: LocalDateTime) extends AnyVal {
    import MythDateTime.FORMATTER_MYTH
    def mythformat: String = localDateTime.format(FORMATTER_MYTH)
    def toMythFormat: String = mythformat
    def toLocalDateTime: LocalDateTime = localDateTime
    def toTimestamp: Long = localDateTime.toInstant(ZoneOffset.UTC).getEpochSecond
    override def toString: String = localDateTime.toString
  }

  object MythDateTime {
    final val FORMATTER_MYTH = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    def fromMythFormat(mdt: String): MythDateTime =
      new MythDateTime(LocalDateTime.parse(mdt, FORMATTER_MYTH))

    def fromTimestamp(ts: Long, tz: ZoneId = ZoneOffset.UTC): MythDateTime =
      new MythDateTime(LocalDateTime.ofInstant(Instant.ofEpochSecond(ts), tz))

    def now: MythDateTime = new MythDateTime(LocalDateTime.now)
  }

  // Used to indicate serialization format for certain MythProtocol commands
  implicit class MythDateTimeString(mythDateTime: MythDateTime) {
    override def toString: String = mythDateTime.mythformat
    def toMythDateTime: MythDateTime = mythDateTime
  }
}
