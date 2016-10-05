package mythtv
package connection
package myth

import java.time.Instant

import data.{ BackendProgram, BackendRecordedMarkup }
import model.{ CaptureCardId, ChanId, Markup, RecordedMarkup, Recording, VideoPosition }
import util.{ MythDateTime, MythDateTimeString }

// Type class for serializable objects in the MythProtocol stream
trait MythProtocolSerializable[T] {
  def deserialize(in: String): T
  def deserialize(in: Seq[String]): T = throw new UnsupportedOperationException
  def serialize(in: T): String
  def serialize(in: T, builder: StringBuilder): StringBuilder = { builder.append(serialize(in)); builder }
}

object MythProtocolSerializable {
  implicit object IntSerializer extends MythProtocolSerializable[Int] {
    def deserialize(in: String): Int = in.toInt
    def serialize(in: Int): String = in.toString
    override def serialize(in: Int, builder: StringBuilder): StringBuilder = { builder.append(in); builder }
  }

  implicit object LongSerializer extends MythProtocolSerializable[Long] {
    def deserialize(in: String): Long = in.toLong
    def serialize(in: Long): String = in.toString
    override def serialize(in: Long, builder: StringBuilder): StringBuilder = { builder.append(in); builder }
  }

  implicit object DoubleSerializer extends MythProtocolSerializable[Double] {
    def deserialize(in: String): Double = in.toDouble
    def serialize(in: Double): String = in.toString
  }

  implicit object BooleanSerializer extends MythProtocolSerializable[Boolean] {
    def deserialize(in: String): Boolean = {
      try {
        in.toBoolean   // handles "True" and "False" (case-insensitive)
      } catch {
        // TODO if this fails, what exception to return to the caller?
        case _: IllegalArgumentException => in.toInt != 0   // handles "1" / "0"
      }
    }
    def serialize(in: Boolean): String = if (in) "1" else "0"
  }

  // TODO if we end up with a lot of these boilerplate XXXId serializer implicit objects that all
  // have the same format, can we create a generic template to inherit from?

  implicit object CaptureCardIdSerializer extends MythProtocolSerializable[CaptureCardId] {
    def deserialize(in: String): CaptureCardId = CaptureCardId(in.toInt)
    def serialize(in: CaptureCardId): String = in.id.toString
    override def serialize(in: CaptureCardId, builder: StringBuilder): StringBuilder = { builder.append(in.id); builder }
  }

  implicit object ChanIdSerializer extends MythProtocolSerializable[ChanId] {
    def deserialize(in: String): ChanId = ChanId(in.toInt)
    def serialize(in: ChanId): String = in.id.toString
    override def serialize(in: ChanId, builder: StringBuilder): StringBuilder = { builder.append(in.id); builder }
  }

  implicit object VideoPositionSerializer extends MythProtocolSerializable[VideoPosition] {
    def deserialize(in: String): VideoPosition = VideoPosition(in.toLong)
    def serialize(in: VideoPosition): String = in.pos.toString
    override def serialize(in: VideoPosition, builder: StringBuilder): StringBuilder = { builder.append(in.pos); builder }
  }

  implicit object InstantSerializer extends MythProtocolSerializable[Instant] {
    def deserialize(in: String): Instant = Instant.parse(in)
    def serialize(in: Instant): String = in.toString
  }

  implicit object MythDateTimeSerializer extends MythProtocolSerializable[MythDateTime] {
    // TODO FIXME eliminate this pyramid of doom. Also what exception to keep if they all fail?
    def deserialize(in: String): MythDateTime = {
      try {
        MythDateTime.fromTimestamp(in.toLong)
      } catch {
        case _ : NumberFormatException =>
          try {
            MythDateTime.fromIso(in)
          } catch {
            case _ : java.time.format.DateTimeParseException =>
              MythDateTime.fromIsoLoose(in)
          }
      }
    }
    def serialize(in: MythDateTime): String = in.toTimestamp.toString
    override def serialize(in: MythDateTime, builder: StringBuilder): StringBuilder =
      { builder.append(in.toTimestamp); builder }
  }

  implicit object MythDateTimeStringSerializer extends MythProtocolSerializable[MythDateTimeString] {
    def deserialize(in: String): MythDateTimeString = MythDateTime.fromMythFormat(in)
    def serialize(in: MythDateTimeString): String = in.toString
  }

  /**** BACKEND OBJECTS (protocol version neutral)  ***/

  implicit object RecordedMarkupSerializer extends MythProtocolSerializable[RecordedMarkup] {
    def deserialize(in: String): RecordedMarkup = deserialize(in split MythProtocol.SPLIT_PATTERN)
    override def deserialize(in: Seq[String]): RecordedMarkup = {
      assert(in.lengthCompare(1) > 0)
      BackendRecordedMarkup(
        Markup(MythProtocolSerializer.deserialize[Int](in(0))),
        MythProtocolSerializer.deserialize[VideoPosition](in(1))
      )
    }
    def serialize(in: RecordedMarkup) = throw new UnsupportedOperationException
  }
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
// TODO deserialize also may vary across versions! Depends on FIELD_ORDER definition, anything else ...
// TODO generalize this over different potential FIELD_ORDERs for different protocol versions
object ProgramInfoSerializerCurrent extends MythProtocolSerializable[Recording] {
  // TODO might like to be able to serialize a Program or Recordable...
  //       ... we would need some defaults for the missing fields ...
  //       ... is this advisable? we should probably only ever pass back records we got from the backend
  //def serialize(in: Recording): String = ???

  def deserialize(in: String): Recording = new BackendProgram(in split MythProtocol.SPLIT_PATTERN)

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
