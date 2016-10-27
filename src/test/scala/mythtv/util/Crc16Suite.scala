package mythtv
package util

import org.scalatest.FunSuite

class Crc16Suite extends FunSuite {
  test("CRC-16/CCITT X.25 of '123456789' should be 0x906e") {
    assert(Crc16("123456789") === 0x906e)
  }
}
