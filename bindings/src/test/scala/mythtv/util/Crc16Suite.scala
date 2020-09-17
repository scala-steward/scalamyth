// SPDX-License-Identifier: LGPL-2.1-only
/*
 * Crc16Suite.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package util

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import org.scalatest.funsuite.AnyFunSuite

class Crc16Suite extends AnyFunSuite {
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
