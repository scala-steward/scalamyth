package mythtv
package util

import org.scalatest.FunSuite

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
    assert(Days.Monday.id    === 0x01)
    assert(Days.Tuesday.id   === 0x02)
    assert(Days.Wednesday.id === 0x04)
    assert(Days.Thursday.id  === 0x08)
    assert(Days.Friday.id    === 0x10)
    assert(Days.Saturday.id  === 0x20)
    assert(Days.Sunday.id    === 0x40)
    assert(Days.Weekday.id   === 0x1f)
    assert(Days.Weekend.id   === 0x60)
  }

  test("Days.values.size is 7") {
    assert(Days.values.size === 7)
  }

  test("Days value elements toString") {
    assert(Days.Monday.toString === "Monday")
    assert(Days.Tuesday.toString === "Tuesday")
    assert(Days.Wednesday.toString === "Wednesday")
    assert(Days.Thursday.toString === "Thursday")
    assert(Days.Friday.toString === "Friday")
    assert(Days.Saturday.toString === "Saturday")
    assert(Days.Sunday.toString === "Sunday")
  }

  test("Days mask elements toString") {
    assert(Days.Weekday.toString === "Weekday")
    assert(Days.Weekend.toString === "Weekend")
  }

  test("Days mask elements set contents") {
    assert(Days.Weekday.toSet === Set(Days.Monday, Days.Tuesday, Days.Wednesday, Days.Thursday, Days.Friday))
    assert(Days.Weekend.toSet === Set(Days.Saturday, Days.Sunday))
  }

  test("Days mask contains method") {
    assert(Days.Weekend contains Days.Saturday)
    assert(Days.Weekend contains Days.Sunday)
    assert(!(Days.Weekend contains Days.Monday))
    assert(!(Days.Weekend contains Days.Friday))
  }

  test("Arbitrary mask definition") {
    val mask = Days.Monday | Days.Wednesday | Days.Friday
    assert(mask.id === 0x15)
    assert(mask.toString === "Days.Mask(Monday | Wednesday | Friday)")
  }

  test("Arbitrary mask with over-coverage") {
    val mask = ~Days.Weekend
    val mask2 = mask & ~Days.Weekday
    assert(mask.size === 30)
    assert(mask2.size === 25)
  }

  test("Factory apply") {
    assert(Days(0x04) === Days.Wednesday)
    assert(Days(0x40) === Days.Sunday)
    assert(Days(0x1f) === Days.Weekday)
  }

  test("Mask factory apply") {
    assert(Days.Mask(Days.Thursday).id === Days.Thursday.id)
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
    assert(m.id === Days.Tuesday.id)
  }

  test("Mask keysIteratorFrom") {
    assert((Days.Weekday keysIteratorFrom Days.Wednesday).toList ===
      List(Days.Wednesday, Days.Thursday, Days.Friday))
    assert((Days.Weekday keysIteratorFrom Days.Saturday).toList === Nil)
  }

  test("Mask range") {
    assert((Days.Weekday range (Days.Tuesday, Days.Thursday)).toList ===
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
    assert(Ordinals.First.id === 1)
    assert(Ordinals.ThirtySecond.id === (1 << 31))
  }

  test("Excessive number of values defined should fail") {
    object ExtraOrdinals extends AbstractOrdinals {
      val ThirtyThird   = Value
    }
    intercept[AssertionError] {  // TODO should throw a different exception to indicate fullness?
      val _ = ExtraOrdinals.First
    }
  }
}
