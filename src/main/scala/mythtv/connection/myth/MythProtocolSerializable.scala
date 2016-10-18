package mythtv
package connection
package myth

import java.time.Instant

import data._
import util._
import model._
import model.EnumTypes.RecStatus

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

  implicit object ChannelNumberSerializer extends MythProtocolSerializable[ChannelNumber] {
    def deserialize(in: String): ChannelNumber = ChannelNumber(in)
    def serialize(in: ChannelNumber): String = in.num
    override def serialize(in: ChannelNumber, builder: StringBuilder): StringBuilder = { builder.append(in.num); builder }
  }

  implicit object ListingSourceIdSerializer extends MythProtocolSerializable[ListingSourceId] {
    def deserialize(in: String): ListingSourceId = ListingSourceId(in.toInt)
    def serialize(in: ListingSourceId): String = in.id.toString
    override def serialize(in: ListingSourceId, builder: StringBuilder): StringBuilder = { builder.append(in.id); builder }
  }

  implicit object RecordRuleIdSerializer extends MythProtocolSerializable[RecordRuleId] {
    def deserialize(in: String): RecordRuleId = RecordRuleId(in.toInt)
    def serialize(in: RecordRuleId): String = in.id.toString
    override def serialize(in: RecordRuleId, builder: StringBuilder): StringBuilder = { builder.append(in.id); builder }
  }

  implicit object RecStatusSerializer extends MythProtocolSerializable[RecStatus] {
    def deserialize(in: String): RecStatus = RecStatus.applyOrUnknown(in.toInt)
    def serialize(in: RecStatus): String = in.id.toString
    override def serialize(in: RecStatus, builder: StringBuilder): StringBuilder = { builder.append(in.id); builder }
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
            MythDateTime.fromNaiveIso(in)
          } catch {
            case _ : java.time.format.DateTimeParseException =>
              MythDateTime.fromNaiveIsoLoose(in)
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

  /**** UTILITY OBJECTS (not MythTV specific) ******/

  implicit object FileStatsSerializer extends MythProtocolSerializable[FileStats] {
    def deserialize(in: String): FileStats = deserialize(in split MythProtocol.SPLIT_PATTERN)
    override def deserialize(in: Seq[String]): FileStats = FileStats(
      MythProtocol.deserialize[Long](in(0)),    // st_dev
      MythProtocol.deserialize[Long](in(1)),    // st_ino
      MythProtocol.deserialize[Int](in(2)),     // st_mode
      MythProtocol.deserialize[Long](in(3)),    // st_nlink
      MythProtocol.deserialize[Int](in(4)),     // st_uid
      MythProtocol.deserialize[Int](in(5)),     // st_gid
      MythProtocol.deserialize[Long](in(6)),    // st_rdev
      DecimalByteCount(MythProtocol.deserialize[Long](in(7))),       // st_size
      BinaryByteCount(MythProtocol.deserialize[Long](in(8))),        // st_blksize
      MythProtocol.deserialize[Long](in(9)),    // st_blocks
      Instant.ofEpochSecond(MythProtocol.deserialize[Long](in(10))), // st_atim
      Instant.ofEpochSecond(MythProtocol.deserialize[Long](in(11))), // st_mtim
      Instant.ofEpochSecond(MythProtocol.deserialize[Long](in(12)))  // st_ctim
    )
    def serialize(in: FileStats): String = throw new UnsupportedOperationException
  }

  /**** BACKEND OBJECTS (protocol version neutral)  ***/

  implicit object RecordedMarkupSerializer extends MythProtocolSerializable[RecordedMarkup] {
    def deserialize(in: String): RecordedMarkup = deserialize(in split MythProtocol.SPLIT_PATTERN)
    override def deserialize(in: Seq[String]): RecordedMarkup = {
      assert(in.lengthCompare(1) > 0)
      BackendRecordedMarkup(
        Markup.applyOrUnknown(MythProtocol.deserialize[Int](in(0))),
        MythProtocol.deserialize[VideoPosition](in(1))
      )
    }
    def serialize(in: RecordedMarkup): String = throw new UnsupportedOperationException
  }
}

// Idea here is a generic de-/serializer for backend objects that can be
//  - constructed from a Seq[String] and a FIELD_ORDER
//  - deconstructed using FIELD_ORDER and the apply-based fields accessor
trait GenericBackendObjectSerializer[T, F <: GenericBackendObjectFactory[T]]
    extends MythProtocolSerializable[T] {
  def newFactory: F

  // TODO pass FIELD_ORDER into factory apply/constructor...

  // TODO do we really need to call take() in deserialize? Use a view slice instead?
  // What about deserialization of a adjacent sequence of like-typed objects?

  def deserialize(in: String): T = {
    val factory = newFactory
    val data: Seq[String] = in split MythProtocol.SPLIT_PATTERN
    factory(data take factory.FIELD_ORDER.length)
  }

  override def deserialize(in: Seq[String]): T = {
    val factory = newFactory
    factory(in take factory.FIELD_ORDER.length)
  }

  def serialize(in: T): String = in match {
    case g: GenericBackendObject =>
      val factory = newFactory
      factory.FIELD_ORDER map (g(_)) mkString MythProtocol.BACKEND_SEP
    case _ => ???
  }

  override def serialize(in: T, builder: StringBuilder): StringBuilder = in match {
    case g: GenericBackendObject =>
      val factory = newFactory
      (factory.FIELD_ORDER map (g(_))).addString(builder, MythProtocol.BACKEND_SEP)
    case _ => ???
  }
}

object ProgramInfoSerializerGeneric extends GenericBackendObjectSerializer[Recording, BackendProgramFactory] {
  def newFactory = BackendProgram
}

object FreeSpaceSerializerGeneric extends GenericBackendObjectSerializer[FreeSpace, BackendFreeSpaceFactory] {
  def newFactory = BackendFreeSpace
}

object CardInputSerializerGeneric extends GenericBackendObjectSerializer[CardInput, BackendCardInputFactory] {
  def newFactory = BackendCardInput
}

object ChannelSerializerGeneric extends GenericBackendObjectSerializer[Channel, BackendChannelFactory] {
  def newFactory = BackendChannel
}

object UpcomingProgramSerializerGeneric extends GenericBackendObjectSerializer[UpcomingProgram, BackendUpcomingProgramFactory] {
  def newFactory = BackendUpcomingProgram
}
