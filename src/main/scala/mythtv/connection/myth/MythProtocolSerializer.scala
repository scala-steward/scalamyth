package mythtv
package connection
package myth

object MythProtocolSerializer {
  def deserialize[T: MythProtocolSerializable](arg: String): T =
    implicitly[MythProtocolSerializable[T]].deserialize(arg)
  def serialize[T: MythProtocolSerializable](arg: T): String =
    implicitly[MythProtocolSerializable[T]].serialize(arg)
  def serialize[T: MythProtocolSerializable](arg: T, builder: StringBuilder): StringBuilder =
    implicitly[MythProtocolSerializable[T]].serialize(arg, builder)
}