package mythtv
package util

import java.time.LocalDateTime

import org.scalatest.FlatSpec

class MythDateTimeSpec extends FlatSpec {
  "A MythDateTime" should "support toMythFormat" in {
    val mdt = MythDateTime.fromNaiveIso("2016-02-14T15:05:17")
    assert(mdt.toMythFormat === "20160214150517")
  }
  it should "support mythformat as an alias for toMythFormat" in {
    val mdt = MythDateTime.fromNaiveIso("2016-02-14T15:05:17")
    assert(mdt.toMythFormat === mdt.mythformat)
  }
  it should "support toIsoFormat" in {
    val mdt = MythDateTime.fromNaiveIso("2016-02-14T15:05:17")
    assert(mdt.toIsoFormat === "2016-02-14T15:05:17Z")
  }
  it should "support toLocalDateTime" in {
    val mdt = MythDateTime.fromNaiveIso("2016-02-14T15:05:17")
    val local: LocalDateTime = mdt.toLocalDateTime
    assert(local.getYear === 2016)
    assert(local.getMonth.getValue === 2)
    assert(local.getDayOfMonth === 14)
    assert(local.getHour === 15)
    assert(local.getMinute === 5)
    assert(local.getSecond === 17)
  }
  it should "support toTimestamp" in {
    val mdt = MythDateTime.fromNaiveIso("2016-02-14T15:05:17")
    assert(mdt.toTimestamp === 1455462317L)
  }

  "A MythDateTime factory" should "parse mythformat input" in {
    val mdt = MythDateTime.fromMythFormat("20160214150517")
    assert(mdt.toIsoFormat === "2016-02-14T15:05:17Z")
  }
  it should "parse strict ISO format input, with Z as timezone" in {
    val mdt = MythDateTime.fromIso("2016-02-14T15:05:17Z")
    assert(mdt.toMythFormat === "20160214150517")
  }
  it should "parse strict ISO format input, without timezone indicator (naive UTC)" in {
    val mdt = MythDateTime.fromNaiveIso("2016-02-14T15:05:17")
    assert(mdt.toMythFormat === "20160214150517")
  }
  it should "parse loose ISO format input, with space separator instead of 'T', no timezone" in {
    val mdt = MythDateTime.fromNaiveIsoLoose("2016-02-14 15:05:17")
    assert(mdt.toMythFormat === "20160214150517")
  }
  it should "parse loose ISO format input, with space separator instead of 'T', no timezone, no seconds" in {
    val mdt = MythDateTime.fromNaiveIsoLoose("2016-02-14 15:05")
    assert(mdt.toMythFormat === "20160214150500")
  }
  it should "convert from timestamp (epoch second) to MythDateTime" in {
    val mdt = MythDateTime.fromTimestamp(1455462317L)
    assert(mdt.toMythFormat === "20160214150517")
  }
  it should "produce a valid current date time from now()" in {
    val mdt = MythDateTime.now
    assert(mdt.toMythFormat.length === 14)
  }
}
