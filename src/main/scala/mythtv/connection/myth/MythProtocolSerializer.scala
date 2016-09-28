package mythtv
package connection
package myth

import model.{ ChanId, Recording }
import util.MythDateTime

trait MythProtocolSerializer[A] {
  def deserialize(in: String): A
  def serialize(in: A): String
  def serialize(in: A, builder: StringBuilder): StringBuilder = { builder.append(serialize(in)); builder }
}

object IntSerializer extends MythProtocolSerializer[Int] {
  def deserialize(in: String): Int = in.toInt
  def serialize(in: Int): String = in.toString
  override def serialize(in: Int, builder: StringBuilder): StringBuilder = { builder.append(in); builder }
}

object LongSerializer extends MythProtocolSerializer[Long] {
  def deserialize(in: String): Long = in.toLong
  def serialize(in: Long): String = in.toString
  override def serialize(in: Long, builder: StringBuilder): StringBuilder = { builder.append(in); builder }
}

object BooleanSerializer extends MythProtocolSerializer[Boolean] {
  def deserialize(in: String): Boolean = in.toBoolean  // handles "True" and "False" (case-insensitive)
  def serialize(in: Boolean): String = if (in) "1" else "0"
}

object ChanIdSerializer extends MythProtocolSerializer[ChanId] {
  def deserialize(in: String): ChanId = ChanId(in.toInt)
  def serialize(in: ChanId): String = in.id.toString
  override def serialize(in: ChanId, builder: StringBuilder): StringBuilder = { builder.append(in.id); builder }
}

object MythDateTimeSerializer extends MythProtocolSerializer[MythDateTime] {
  def deserialize(in: String): MythDateTime = MythDateTime.fromTimestamp(in.toLong)
  def serialize(in: MythDateTime): String = in.toTimestamp.toString
  override def serialize(in: MythDateTime, builder: StringBuilder): StringBuilder = { builder.append(in.toTimestamp); builder }
}

// Idea here is a generic deserializer for objects that can be built from a Seq[String] and a FIELD_ORDER
trait GenericObjectDeserializer[A] {
  def deserialize(in: String): A = {
    val data: Seq[String] = in split MythProtocol.SPLIT_PATTERN
    //new A(data)   // TODO how do I do this? need a Builder or Factory?
    ???
  }
}

// NB "ProgramInfo" serializer may vary across protocol versions
abstract class AbstractProgramInfoSerializer extends MythProtocolSerializer[Recording] {
  // TODO might like to be able to serialize a Program or Recordable...
  //       ... we would need some defaults for the missing fields ...
  //       ... is this advisable? we should probably only ever pass back records we got from the backend
  //def serialize(in: Recording): String = ???

  // TODO deserialize also may vary across versions! Depends on FIELD_ORDER definition, anything else ...
  def deserialize(in: String): Recording = new BackendProgram(in split MythProtocol.SPLIT_PATTERN)
}

// TODO generalize this over different potential FIELD_ORDERs for different protocol versions
object ProgramInfoSerializerCurrent extends AbstractProgramInfoSerializer with MythProtocolSerializer[Recording] {
  def serialize(in: Recording): String = in match {
    // This works because BackendProgram keeps a copy of the original string data around in the fields map
    case p: BackendProgram => BackendProgram.FIELD_ORDER map (p(_)) mkString MythProtocol.BACKEND_SEP
    case _ => ???
  }

  override def serialize(in: Recording, builder: StringBuilder): StringBuilder = in match {
    case p: BackendProgram => (BackendProgram.FIELD_ORDER map (p(_))).addString(builder, MythProtocol.BACKEND_SEP)
    case _ => ???
  }
}
