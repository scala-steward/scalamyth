// SPDX-License-Identifier: LGPL-2.1-only
/*
 * MythDateTimeSpec.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package util

import java.time.Instant

import munit.FunSuite

class MythDateTimeSpec extends FunSuite {
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
    assertEquals(mdt.year, myYear)
    assertEquals(mdt.month, myMonth)
    assertEquals(mdt.day, myDay)
    assertEquals(mdt.hour, myHour)
    assertEquals(mdt.minute, myMinute)
    assertEquals(mdt.second, mySecond)
  }

  test("A MythDateTime should be constructed from a java.time.Instant") {
    val mdt = new MythDateTime(myInstant)
    assert(mdt.isInstanceOf[MythDateTime])
  }

  test("have a working year accessor") {
    val mdt = new MythDateTime(myInstant)
    assertEquals(mdt.year, myYear)
  }

  test("have a working month accessor") {
    val mdt = new MythDateTime(myInstant)
    assertEquals(mdt.month, myMonth)
  }

  test("have a working day accessor") {
    val mdt = new MythDateTime(myInstant)
    assertEquals(mdt.day, myDay)
  }

  test("have a working hour accessor") {
    val mdt = new MythDateTime(myInstant)
    assertEquals(mdt.hour, myHour)
  }

  test("have a working minute accessor") {
    val mdt = new MythDateTime(myInstant)
    assertEquals(mdt.minute, myMinute)
  }

  test("have a working second accessor") {
    val mdt = new MythDateTime(myInstant)
    assertEquals(mdt.second, mySecond)
  }

  test("support formatting as myth format") {
    val mdt = new MythDateTime(myInstant)
    assertEquals(mdt.toMythFormat, myMythFormat)
  }

  test("support mythformat as an alias for toMythFormat") {
    val mdt = new MythDateTime(myInstant)
    assertEquals(mdt.mythformat, mdt.toMythFormat)
  }

  test("support formatting as strict ISO-8601 format") {
    val mdt = new MythDateTime(myInstant)
    assertEquals(mdt.toIsoFormat, myStrictISO)
  }

  test("support conversion to java.time.LocalDateTime") {
    val mdt = new MythDateTime(myInstant)
    val local = mdt.toLocalDateTime()
    assertEquals(local.getYear, myYear)
    assertEquals(local.getMonthValue, myMonth)
    assertEquals(local.getDayOfMonth, myDay)
    assertEquals(local.getHour, myHour)
    assertEquals(local.getMinute, myMinute)
    assertEquals(local.getSecond, mySecond)
  }

  test("support conversion to a unix timestamp") {
    val mdt = new MythDateTime(myInstant)
    assertEquals(mdt.toTimestamp, myEpochSecond)
  }

  test("A MythDateTime factory should build from a java.lang.Instant") {
    val mdt = MythDateTime(myInstant)
    checkAllAccessors(mdt)
  }

  test("build from itemized year,month,day,hour,minute,second") {
    val mdt = MythDateTime(myYear, myMonth, myDay, myHour, myMinute, mySecond)
    checkAllAccessors(mdt)
  }

  test("parse mythformat input") {
    val mdt = MythDateTime.fromMythFormat(myMythFormat)
    checkAllAccessors(mdt)
  }

  test("parse strict ISO format input, with Z as timezone") {
    val mdt = MythDateTime.fromIso(myStrictISO)
    checkAllAccessors(mdt)
  }

  test("parse strict ISO format input, without timezone indicator (naive UTC)") {
    val mdt = MythDateTime.fromNaiveIso(myNaiveISO)
    checkAllAccessors(mdt)
  }

  test("parse loose ISO format input, with space separator instead of 'T', no timezone") {
    val mdt = MythDateTime.fromNaiveIsoLoose(myNaiveISOLoose)
    checkAllAccessors(mdt)
  }

  test("parse loose ISO format input, with space separator instead of 'T', no timezone, no seconds") {
    val mdt = MythDateTime.fromNaiveIsoLoose(myNaiveISOLooseNoSeconds)
    assertEquals(mdt.year, myYear)
    assertEquals(mdt.month, myMonth)
    assertEquals(mdt.day, myDay)
    assertEquals(mdt.hour, myHour)
    assertEquals(mdt.minute, myMinute)
    assertEquals(mdt.second, 0)
  }

  test("convert from timestamp (epoch second) to MythDateTime") {
    val mdt = MythDateTime.fromTimestamp(myEpochSecond)
    checkAllAccessors(mdt)
  }

  test("produce a valid current date time from now()") {
    val mdt = MythDateTime.now
    assertEquals(mdt.toMythFormat.length, 14)
  }
}
