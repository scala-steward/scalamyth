package mythtv
package connection
package myth
package data

private[myth] class BackendSerializationBuilder(builder: StringBuilder) extends MythProtocolSerializer {
  private[this] var count = 0

  def result: StringBuilder = builder

  def += [T: MythProtocolSerializable](obj: T): this.type = {
    if (count > 0) builder ++= MythProtocol.BACKEND_SEP
    serialize(obj, builder)
    count += 1
    this
  }
}
