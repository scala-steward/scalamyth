// SPDX-License-Identifier: LGPL-2.1-only
/*
 * BackendSerializationBuilder.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth
package data

private[myth] class BackendSerializationBuilder(builder: StringBuilder) extends MythProtocolSerializer {
  private[this] var count = 0

  def result: StringBuilder = builder

  def += [T: MythProtocolSerializable](obj: T): this.type = {
    if (count > 0) builder ++= MythProtocol.Separator
    serialize(obj, builder)
    count += 1
    this
  }
}
