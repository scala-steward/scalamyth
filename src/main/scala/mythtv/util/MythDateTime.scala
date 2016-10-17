package mythtv
package util

import java.time.{ LocalDateTime, Instant, ZoneId, ZoneOffset }
import java.time.format.DateTimeFormatter

// TODO: do we need to/from RFC format (RFC_1123_DATE_TIME) ?
// TODO: do we want to use LocalDateTime or Instant as our representation of Myth UTC time?

class MythDateTime(val localDateTime: LocalDateTime) extends AnyVal {
  import MythDateTime.FORMATTER_MYTH
  def mythformat: String = localDateTime.format(FORMATTER_MYTH)
  def toMythFormat: String = mythformat
  def toIsoFormat: String = localDateTime.toString  // default LocalDateTime.toString is ISO format
  def toLocalDateTime: LocalDateTime = localDateTime
  def toTimestamp: Long = localDateTime.toInstant(ZoneOffset.UTC).getEpochSecond
  override def toString: String = localDateTime.toString
}

object MythDateTime {
  final val FORMATTER_MYTH = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

  // TODO define this implicit here or in util package object?
  // Defining it here makes it hard to import on purpose, and prevents
  // "enrichment" like LocalDateTime.toMythFormat from easily working.
  /*
  import scala.language.implicitConversions
  implicit def javaLocalDt2MythDt(dt: LocalDateTime) = new MythDateTime(dt)
   */

  def fromMythFormat(mdt: String): MythDateTime =
    new MythDateTime(LocalDateTime.parse(mdt, FORMATTER_MYTH))

  def fromIso(isoDt: String): MythDateTime =
    new MythDateTime(LocalDateTime.parse(isoDt))

  // Like ISO format but with space delimiter instead of 'T'.
  // Returned by backend as result of QUERY_GUIDEDATATHROUGH for example
  def fromIsoLoose(isoDt: String): MythDateTime =
    new MythDateTime(LocalDateTime.parse(isoDt, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss]")))

  def fromTimestamp(ts: Long, tz: ZoneId = ZoneOffset.UTC): MythDateTime =
    // TODO should I directly call LocalDateTime.ofEpochSecond? has slight diff signature (nanos)
    new MythDateTime(LocalDateTime.ofInstant(Instant.ofEpochSecond(ts), tz))

  def now: MythDateTime = new MythDateTime(LocalDateTime.now(ZoneOffset.UTC))

  object MythDateTimeOrdering extends Ordering[MythDateTime] {
    def compare(x: MythDateTime, y: MythDateTime) = x.localDateTime compareTo y.localDateTime
  }

  implicit def ordering: Ordering[MythDateTime] = MythDateTimeOrdering
}
