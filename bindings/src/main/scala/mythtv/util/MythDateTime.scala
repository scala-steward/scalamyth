// SPDX-License-Identifier: LGPL-2.1-only
/*
 * MythDateTime.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package util

import java.time.{ LocalDateTime, OffsetDateTime, Instant, ZoneId, ZoneOffset, ZonedDateTime }
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class MythDateTime(val instant: Instant) extends AnyVal {
  private def offsetDt = instant atOffset ZoneOffset.UTC
  private def truncated = instant truncatedTo ChronoUnit.SECONDS

  // accessors
  def year: Int = offsetDt.getYear
  def month: Int = offsetDt.getMonthValue
  def day: Int = offsetDt.getDayOfMonth

  def hour: Int = offsetDt.getHour
  def minute: Int = offsetDt.getMinute
  def second: Int = offsetDt.getSecond

  // formatters
  import MythDateTime.FORMATTER_MYTH
  def mythformat: String = offsetDt format FORMATTER_MYTH
  def toMythFormat: String = mythformat
  def toIsoFormat: String = truncated.toString
  def toNaiveIsoFormat: String = truncated.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
  override def toString: String = instant.toString

  // converters
  def toTimestamp: Long = instant.getEpochSecond
  def toInstant: Instant = instant
  def toLocalDateTime(zoneId: ZoneId = ZoneOffset.UTC): LocalDateTime = toZonedDateTime(zoneId).toLocalDateTime
  def toOffsetDateTime(offset: ZoneOffset = ZoneOffset.UTC): OffsetDateTime = instant atOffset offset
  def toZonedDateTime(zoneId: ZoneId = ZoneOffset.UTC): ZonedDateTime = instant atZone zoneId
}

object MythDateTime {
  final val FORMATTER_MYTH = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

  final val empty = new MythDateTime(Instant.MIN)

  def apply(instant: Instant) = new MythDateTime(instant)

  def apply(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int) =
    new MythDateTime(
      OffsetDateTime.of(year, month, day, hour, minute, second, 0, ZoneOffset.UTC).toInstant)

  private def apply(localDt: LocalDateTime) = new MythDateTime(localDt.toInstant(ZoneOffset.UTC))

  def fromMythFormat(mdt: String): MythDateTime = MythDateTime(LocalDateTime.parse(mdt, FORMATTER_MYTH))

  // this is strict ISO format with UTC ("Z") timezone
  def fromIso(isoDt: String): MythDateTime = MythDateTime(Instant.parse(isoDt))

  // this is naive ISO format (no timezone, implied to be UTC)
  def fromNaiveIso(isoDt: String): MythDateTime = MythDateTime(LocalDateTime.parse(isoDt))

  // Like naive ISO format but with space delimiter instead of 'T'.
  // Returned by backend as result of QUERY_GUIDEDATATHROUGH for example
  def fromNaiveIsoLoose(isoDt: String): MythDateTime =
    MythDateTime(LocalDateTime.parse(isoDt, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss]")))

  def fromTimestamp(ts: Long): MythDateTime = MythDateTime(Instant.ofEpochSecond(ts))

  def now: MythDateTime = MythDateTime(Instant.now)

  object MythDateTimeOrdering extends Ordering[MythDateTime] {
    def compare(x: MythDateTime, y: MythDateTime) = x.instant compareTo y.instant
  }

  implicit def ordering: Ordering[MythDateTime] = MythDateTimeOrdering
}
