// SPDX-License-Identifier: LGPL-2.1-only
/*
 * Seekable.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth

import EnumTypes.SeekWhence

object SeekWhence extends Enumeration {
  type SeekWhence = Value
  final val Begin      = Value(0)
  final val Current    = Value(1)
  final val End        = Value(2)
}

trait Seekable {
  def rewind(): Unit
  def seek(offset: Long, whence: SeekWhence): Unit
  def tell: Long
}
