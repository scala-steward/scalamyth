package mythtv
package util

import org.scalatest.FunSuite

class BitmaskEnumSuite extends FunSuite {
  object Days extends BitmaskEnum[Int] {
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
    //assert(Days(0x1f) === Days.Weekday)  // FIXME? mask values are no longer in vmap to be processed by apply()
  }
}
