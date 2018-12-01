// SPDX-License-Identifier: LGPL-2.1-only
/*
 * Base64String.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package util

import java.util.Base64

class Base64String(val str: String) extends AnyVal {
  def decode: Array[Byte] = Base64.getDecoder.decode(str)
  override def toString: String = str
}

object Base64String {
  def apply(data: Array[Byte]): Base64String =
    new Base64String(Base64.getEncoder.encodeToString(data))
}
