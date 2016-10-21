package mythtv
package util

import java.time.{ Instant, LocalDateTime }

import org.scalatest.FlatSpec

class MythDateTimeSpec extends FlatSpec {
  val myEpochSecond: Long = 1455462317L
  val myYear: Int = 2016
  val myMonth: Int = 2
  val myDay: Int = 14
  val myHour: Int = 15
  val myMinute: Int = 5
  val mySecond: Int = 17

  val myInstant: Instant = Instant.ofEpochSecond(myEpochSecond)
  val myNaiveISO: String = "2016-02-14T15:05:17"
  val myStrictISO: String = myNaiveISO + "Z"
  val myNaiveISOLoose: String = myNaiveISO.replace("T", " ")
  val myNaiveISOLooseNoSeconds: String = myNaiveISOLoose.substring(0, myNaiveISOLoose.length - 3)
  val myMythFormat: String = "20160214150517"
  val myMythFormatZeroSeconds: String = "20160214150500"

  def checkAllAccessors(mdt: MythDateTime): Unit = {
    assert(mdt.year === myYear)
    assert(mdt.month === myMonth)
    assert(mdt.day === myDay)
    assert(mdt.hour === myHour)
    assert(mdt.minute === myMinute)
    assert(mdt.second === mySecond)
  }

  "A MythDateTime" should "be constructed from a java.time.Instant" in {
    val mdt = new MythDateTime(myInstant)
  }

  it should "have a working year accessor" in {
    val mdt = new MythDateTime(myInstant)
    assert(mdt.year === myYear)
  }

  it should "have a working month accessor" in {
    val mdt = new MythDateTime(myInstant)
    assert(mdt.month === myMonth)
  }

  it should "have a working day accessor" in {
    val mdt = new MythDateTime(myInstant)
    assert(mdt.day === myDay)
  }

  it should "have a working hour accessor" in {
    val mdt = new MythDateTime(myInstant)
    assert(mdt.hour === myHour)
  }

  it should "have a working minute accessor" in {
    val mdt = new MythDateTime(myInstant)
    assert(mdt.minute === myMinute)
  }

  it should "have a working second accessor" in {
    val mdt = new MythDateTime(myInstant)
    assert(mdt.second === mySecond)
  }

  it should "support formatting as myth format" in {
    val mdt = new MythDateTime(myInstant)
    assert(mdt.toMythFormat === myMythFormat)
  }
  it should "support mythformat as an alias for toMythFormat" in {
    val mdt = new MythDateTime(myInstant)
    assert(mdt.mythformat === mdt.toMythFormat)
  }
  it should "support formatting as strict ISO-8601 format" in {
    val mdt = new MythDateTime(myInstant)
    assert(mdt.toIsoFormat === myStrictISO)
  }
  it should "support conversion to java.time.LocalDateTime" in {
    val mdt = new MythDateTime(myInstant)
    val local = mdt.toLocalDateTime()
    assert(local.getYear === myYear)
    assert(local.getMonthValue === myMonth)
    assert(local.getDayOfMonth === myDay)
    assert(local.getHour === myHour)
    assert(local.getMinute === myMinute)
    assert(local.getSecond === mySecond)
  }
  it should "support conversion to a unix timestamp" in {
    val mdt = new MythDateTime(myInstant)
    assert(mdt.toTimestamp === myEpochSecond)
  }

  "A MythDateTime factory" should "build from a java.lang.Instant" in {
    val mdt = MythDateTime(myInstant)
    checkAllAccessors(mdt)
  }
  it should "build from itemized year,month,day,hour,minute,second" in {
    val mdt = MythDateTime(myYear, myMonth, myDay, myHour, myMinute, mySecond)
    checkAllAccessors(mdt)
  }
  it should "parse mythformat input" in {
    val mdt = MythDateTime.fromMythFormat(myMythFormat)
    checkAllAccessors(mdt)
  }
  it should "parse strict ISO format input, with Z as timezone" in {
    val mdt = MythDateTime.fromIso(myStrictISO)
    checkAllAccessors(mdt)
  }
  it should "parse strict ISO format input, without timezone indicator (naive UTC)" in {
    val mdt = MythDateTime.fromNaiveIso(myNaiveISO)
    checkAllAccessors(mdt)
  }
  it should "parse loose ISO format input, with space separator instead of 'T', no timezone" in {
    val mdt = MythDateTime.fromNaiveIsoLoose(myNaiveISOLoose)
    checkAllAccessors(mdt)
  }
  it should "parse loose ISO format input, with space separator instead of 'T', no timezone, no seconds" in {
    val mdt = MythDateTime.fromNaiveIsoLoose(myNaiveISOLooseNoSeconds)
    assert(mdt.year === myYear)
    assert(mdt.month === myMonth)
    assert(mdt.day === myDay)
    assert(mdt.hour === myHour)
    assert(mdt.minute === myMinute)
    assert(mdt.second === 0)
  }
  it should "convert from timestamp (epoch second) to MythDateTime" in {
    val mdt = MythDateTime.fromTimestamp(myEpochSecond)
    checkAllAccessors(mdt)
  }
  it should "produce a valid current date time from now()" in {
    val mdt = MythDateTime.now
    assert(mdt.toMythFormat.length === 14)
  }
}
