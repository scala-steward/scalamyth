// SPDX-License-Identifier: LGPL-2.1-only
/*
 * BitmaskEnumSuite.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package util

import munit.FunSuite

class BitmaskEnumSuite extends FunSuite {
  object Days extends IntBitmaskEnum {
    val Monday    = Value
    val Tuesday   = Value
    val Wednesday = Value
    val Thursday  = Value
    val Friday    = Value
    val Saturday  = Value
    val Sunday    = Value
    val Weekday   = Mask(Monday | Tuesday | Wednesday | Thursday | Friday)
    val Weekend   = Mask(Saturday | Sunday)
  }

  abstract class AbstractOrdinals extends IntBitmaskEnum {
    val First         = Value
    val Second        = Value
    val Third         = Value
    val Fouth         = Value
    val Fifth         = Value
    val Sixth         = Value
    val Seventh       = Value
    val Eighth        = Value
    val Ninth         = Value
    val Tenth         = Value
    val Eleventh      = Value
    val Twelfth       = Value
    val Thirteenth    = Value
    val Fourteenth    = Value
    val Fixteenth     = Value
    val Sixteenth     = Value
    val Seventeenth   = Value
    val Eighteenth    = Value
    val Nineteenth    = Value
    val Twentieth     = Value
    val TwentyFirst   = Value
    val TwentySecond  = Value
    val TwenthThird   = Value
    val TwentyFourth  = Value
    val TwentyFifth   = Value
    val TwentySixth   = Value
    val TwentySeventh = Value
    val TwentyEighth  = Value
    val TwentyNinth   = Value
    val Thirtieth     = Value
    val ThirtyFirst   = Value
    val ThirtySecond  = Value
  }

  test("Days of week bitmask enum of Int with autoincrement values") {
    assertEquals(Days.Monday.id,    0x01)
    assertEquals(Days.Tuesday.id,   0x02)
    assertEquals(Days.Wednesday.id, 0x04)
    assertEquals(Days.Thursday.id,  0x08)
    assertEquals(Days.Friday.id,    0x10)
    assertEquals(Days.Saturday.id,  0x20)
    assertEquals(Days.Sunday.id,    0x40)
    assertEquals(Days.Weekday.id,   0x1f)
    assertEquals(Days.Weekend.id,   0x60)
  }

  test("Days.values.size is 7") {
    assertEquals(Days.values.size, 7)
  }

  test("Days value elements toString") {
    assertEquals(Days.Monday.toString, "Monday")
    assertEquals(Days.Tuesday.toString, "Tuesday")
    assertEquals(Days.Wednesday.toString, "Wednesday")
    assertEquals(Days.Thursday.toString, "Thursday")
    assertEquals(Days.Friday.toString, "Friday")
    assertEquals(Days.Saturday.toString, "Saturday")
    assertEquals(Days.Sunday.toString, "Sunday")
  }

  test("Days mask elements toString") {
    assertEquals(Days.Weekday.toString, "Weekday")
    assertEquals(Days.Weekend.toString, "Weekend")
  }

  test("Days mask elements set contents") {
    assertEquals(Days.Weekday.toSet, Set(Days.Monday, Days.Tuesday, Days.Wednesday, Days.Thursday, Days.Friday))
    assertEquals(Days.Weekend.toSet, Set(Days.Saturday, Days.Sunday))
  }

  test("Days mask contains method") {
    assert(Days.Weekend contains Days.Saturday)
    assert(Days.Weekend contains Days.Sunday)
    assert(!(Days.Weekend contains Days.Monday))
    assert(!(Days.Weekend contains Days.Friday))
  }

  test("Arbitrary mask definition") {
    val mask = Days.Monday | Days.Wednesday | Days.Friday
    assertEquals(mask.id, 0x15)
    assertEquals(mask.toString, "Days.Mask(Monday | Wednesday | Friday)")
  }

  test("Arbitrary mask with over-coverage") {
    val mask = ~Days.Weekend
    val mask2 = mask & ~Days.Weekday
    assertEquals(mask.size, 30)
    assertEquals(mask2.size, 25)
  }

  test("Factory apply") {
    assertEquals(Days(0x04), Days.Wednesday)
    assertEquals(Days(0x40), Days.Sunday)
    assertEquals(Days(0x1f), Days.Weekday)
  }

  test("Mask factory apply") {
    assertEquals(Days.Mask(Days.Thursday).id, Days.Thursday.id)
  }

  test("Value equality") {
    object A extends IntBitmaskEnum {
      val One = Value
      val Two = Value
    }

    object B extends IntBitmaskEnum {
      val One = Value
      val Two = Value
    }

    assert(A.One equals A.One)
    assert(A.Two equals A.Two)
    assert(!(A.One equals A.Two))
    assert(A.One equals A(1))

    assert(B.One equals B.One)
    assert(B.Two equals B.Two)
    assert(!(B.One equals B.Two))
    assert(B.One equals B(1))

    assert(A.One.id equals B.One.id)
    assert(A.Two.id equals B.Two.id)
    assert(!(A.One equals B.One))
    assert(!(A.Two equals B.Two))
  }

  test("Value toMask") {
    val m = Days.Tuesday.toMask
    assert(m.isInstanceOf[Days.Mask])
    assertEquals(m.id, Days.Tuesday.id)
  }

  test("Mask iteratorFrom") {
    assertEquals((Days.Weekday iteratorFrom Days.Wednesday).toList,
      List(Days.Wednesday, Days.Thursday, Days.Friday))
    assertEquals((Days.Weekday iteratorFrom Days.Saturday).toList, Nil)
  }

  test("Mask range") {
    assertEquals((Days.Weekday.range(Days.Tuesday, Days.Thursday)).toList,
      List(Days.Tuesday, Days.Wednesday))
  }

  test("Mask equality") {
    assert(Days.Weekend equals Days.Weekend)
    assert(Days.Weekend equals Days.Mask(Days.Saturday | Days.Sunday))
    assert(!(Days.Weekend equals Days.Weekday))

    object Binary extends IntBitmaskEnum {
      val Zero = Mask(0)
      val One  = Value(1)
    }

    assert(Binary.Zero equals Binary.Zero)
    assert(Binary.Zero equals Binary.Mask.empty)
    assert(!(Binary.Zero equals Binary.One))
  }

  test("Extreme ends of autonumbering") {
    object Ordinals extends AbstractOrdinals
    assertEquals(Ordinals.First.id, 1)
    assertEquals(Ordinals.ThirtySecond.id, (1 << 31))
  }

  test("Excessive number of values defined should fail") {
    object ExtraOrdinals extends AbstractOrdinals {
      val ThirtyThird = Value
    }
    intercept[AssertionError] {  // TODO should throw a different exception to indicate fullness?
      val _ = ExtraOrdinals.First
    }
  }
}
