// SPDX-License-Identifier: LGPL-2.1-only
/*
 * MythProtocolSerializer.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth

trait MythProtocolSerializer {
  def deserialize[T: MythProtocolSerializable](arg: String): T =
    implicitly[MythProtocolSerializable[T]].deserialize(arg)
  def deserialize[T: MythProtocolSerializable](arg: Seq[String]): T =
    implicitly[MythProtocolSerializable[T]].deserialize(arg)
  def serialize[T: MythProtocolSerializable](arg: T): String =
    implicitly[MythProtocolSerializable[T]].serialize(arg)
  def serialize[T: MythProtocolSerializable](arg: T, builder: StringBuilder): StringBuilder =
    implicitly[MythProtocolSerializable[T]].serialize(arg, builder)
}

object MythProtocolSerializer extends MythProtocolSerializer
