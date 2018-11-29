package mythtv
package util

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import org.scalatest.FunSuite

class Crc16Suite extends FunSuite {
  test("CRC-16/CCITT X.25 of '123456789' should be 0x906e") {
    assert(Crc16("123456789").crc === 0x906e)
    assert(new Crc16(0x906e).verify("123456789"))

    val digitsBuf = StandardCharsets.UTF_8.encode("123456789")
    val directBuf = ByteBuffer.allocateDirect(digitsBuf.limit).put(digitsBuf)
    digitsBuf.rewind()
    directBuf.flip()

    assert(Crc16(digitsBuf).crc === 0x906e)
    assert(Crc16(directBuf).crc === 0x906e)
  }
}
