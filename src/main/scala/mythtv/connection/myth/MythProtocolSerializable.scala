package mythtv
package connection
package myth

import java.time.{ Instant, LocalDate, Year }

import scala.util.Try

import data._
import util._
import model._
import model.EnumTypes._
import EnumTypes.SeekWhence

// Type class for serializable objects in the MythProtocol stream
trait MythProtocolSerializable[T] {
  def deserialize(in: String): T
  def deserialize(in: Seq[String]): T = throw new UnsupportedOperationException
  def serialize(in: T): String
  def serialize(in: T, builder: StringBuilder): StringBuilder = builder.append(serialize(in))
}

object MythProtocolSerializable {
  implicit object StringSerializer extends MythProtocolSerializable[String] {
    def deserialize(in: String): String = in
    def serialize(in: String): String = in
    override def serialize(in: String, builder: StringBuilder): StringBuilder = builder.append(in)
  }

  implicit object IntSerializer extends MythProtocolSerializable[Int] {
    def deserialize(in: String): Int = in.toInt
    def serialize(in: Int): String = in.toString
    override def serialize(in: Int, builder: StringBuilder): StringBuilder = builder.append(in)
  }

  implicit object LongSerializer extends MythProtocolSerializable[Long] {
    def deserialize(in: String): Long = in.toLong
    def serialize(in: Long): String = in.toString
    override def serialize(in: Long, builder: StringBuilder): StringBuilder = builder.append(in)
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
    override def serialize(in: CaptureCardId, builder: StringBuilder): StringBuilder = builder.append(in.id)
  }

  implicit object ChanIdSerializer extends MythProtocolSerializable[ChanId] {
    def deserialize(in: String): ChanId = ChanId(in.toInt)
    def serialize(in: ChanId): String = in.id.toString
    override def serialize(in: ChanId, builder: StringBuilder): StringBuilder = builder.append(in.id)
  }

  implicit object ChannelNumberSerializer extends MythProtocolSerializable[ChannelNumber] {
    def deserialize(in: String): ChannelNumber = ChannelNumber(in)
    def serialize(in: ChannelNumber): String = in.num
    override def serialize(in: ChannelNumber, builder: StringBuilder): StringBuilder = builder.append(in.num)
  }

  implicit object FileTranferIdSerializer extends MythProtocolSerializable[FileTransferId] {
    def deserialize(in: String): FileTransferId = FileTransferId(in.toInt)
    def serialize(in: FileTransferId): String = in.id.toString
    override def serialize(in: FileTransferId, builder: StringBuilder): StringBuilder = builder.append(in.id)
  }

  implicit object InputIdSerializer extends MythProtocolSerializable[InputId] {
    def deserialize(in: String): InputId = InputId(in.toInt)
    def serialize(in: InputId): String = in.id.toString
    override def serialize(in: InputId, builder: StringBuilder): StringBuilder = builder.append(in.id)
  }

  implicit object ListingSourceIdSerializer extends MythProtocolSerializable[ListingSourceId] {
    def deserialize(in: String): ListingSourceId = ListingSourceId(in.toInt)
    def serialize(in: ListingSourceId): String = in.id.toString
    override def serialize(in: ListingSourceId, builder: StringBuilder): StringBuilder = builder.append(in.id)
  }

  implicit object MultiplexIdSerializer extends MythProtocolSerializable[MultiplexId] {
    def deserialize(in: String): MultiplexId = MultiplexId(in.toInt)
    def serialize(in: MultiplexId): String = in.id.toString
    override def serialize(in: MultiplexId, builder: StringBuilder): StringBuilder = builder.append(in.id)
  }

  implicit object RecordRuleIdSerializer extends MythProtocolSerializable[RecordRuleId] {
    def deserialize(in: String): RecordRuleId = RecordRuleId(in.toInt)
    def serialize(in: RecordRuleId): String = in.id.toString
    override def serialize(in: RecordRuleId, builder: StringBuilder): StringBuilder = builder.append(in.id)
  }

  implicit object RecStatusSerializer extends MythProtocolSerializable[RecStatus] {
    def deserialize(in: String): RecStatus = RecStatus.applyOrUnknown(in.toInt)
    def serialize(in: RecStatus): String = in.id.toString
    override def serialize(in: RecStatus, builder: StringBuilder): StringBuilder = builder.append(in.id)
  }

  implicit object VideoPositionSerializer extends MythProtocolSerializable[VideoPosition] {
    def deserialize(in: String): VideoPosition = VideoPosition(in.toLong)
    def serialize(in: VideoPosition): String = in.pos.toString
    override def serialize(in: VideoPosition, builder: StringBuilder): StringBuilder = builder.append(in.pos)
  }

  implicit object DupInSerializer extends MythProtocolSerializable[DupCheckIn] {
    def deserialize(in: String): DupCheckIn = DupCheckIn.apply(in.toInt)
    def serialize(in: DupCheckIn): String = in.id.toString
  }

  implicit object DupCheckMethodSerializer extends MythProtocolSerializable[DupCheckMethod] {
    def deserialize(in: String): DupCheckMethod = DupCheckMethod.apply(in.toInt)
    def serialize(in: DupCheckMethod): String = in.id.toString
  }

  implicit object AudioPropertiesSerializer extends MythProtocolSerializable[AudioProperties] {
    def deserialize(in: String): AudioProperties = AudioProperties.apply(in.toInt)
    def serialize(in: AudioProperties): String = in.id.toString
  }

  implicit object VideoPropertiesSerializer extends MythProtocolSerializable[VideoProperties] {
    def deserialize(in: String): VideoProperties = VideoProperties.apply(in.toInt)
    def serialize(in: VideoProperties): String = in.id.toString
  }

  implicit object SubtitleTypeSerializer extends MythProtocolSerializable[SubtitleType] {
    def deserialize(in: String): SubtitleType = SubtitleType.apply(in.toInt)
    def serialize(in: SubtitleType): String = in.id.toString
  }

  implicit object ProgramFlagsSerializer extends MythProtocolSerializable[ProgramFlags] {
    def deserialize(in: String): ProgramFlags = ProgramFlags(in.toInt)
    def serialize(in: ProgramFlags): String = in.id.toString
  }

  implicit object RecTypeSerializer extends MythProtocolSerializable[RecType] {
    def deserialize(in: String): RecType = RecType.applyOrUnknown(in.toInt)
    def serialize(in: RecType): String = in.id.toString
  }

  implicit object SeekWhenceSerializer extends MythProtocolSerializable[SeekWhence] {
    def deserialize(in: String): SeekWhence = SeekWhence(in.toInt)
    def serialize(in: SeekWhence): String = in.id.toString
    override def serialize(in: SeekWhence, builder: StringBuilder): StringBuilder = builder.append(in.id)
  }

  implicit object InstantSerializer extends MythProtocolSerializable[Instant] {
    def deserialize(in: String): Instant = Instant.parse(in)
    def serialize(in: Instant): String = in.toString
  }

  implicit object LocalDateOptionSerializer extends MythProtocolSerializable[Option[LocalDate]] {
    def deserialize(in: String): Option[LocalDate] = Try(LocalDate.parse(in)).toOption
    def serialize(in: Option[LocalDate]): String = if (in.isEmpty) ??? else in.get.toString
  }

  implicit object YearOptionSerializer extends MythProtocolSerializable[Option[Year]] {
    def deserialize(in: String): Option[Year] = { Try(in.toInt) filter (_ != 0) }.toOption map Year.of
    def serialize(in: Option[Year]): String = if (in.isEmpty) "0" else in.get.toString
  }

  implicit object IntOptionSerializer extends MythProtocolSerializable[Option[Int]] {
    def deserialize(in: String): Option[Int] = { Try(in.toInt) filter (_ != 0) }.toOption
    def serialize(in: Option[Int]): String = if (in.isEmpty) "0" else in.get.toString
  }

  implicit object DoubleOptionSerializer extends MythProtocolSerializable[Option[Double]] {
    def deserialize(in: String): Option[Double] = if (in == "0") None else Try(in.toDouble).toOption
    def serialize(in: Option[Double]): String = if (in.isEmpty) "0" else in.get.toString
  }

  implicit object MythDateTimeSerializer extends MythProtocolSerializable[MythDateTime] {
    // FIXME eliminate this pyramid of doom. Also what exception to keep if they all fail?
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
    override def serialize(in: MythDateTime, builder: StringBuilder): StringBuilder = builder.append(in.toTimestamp)
  }

  implicit object MythDateTimeStringSerializer extends MythProtocolSerializable[MythDateTimeString] {
    def deserialize(in: String): MythDateTimeString = MythDateTime.fromMythFormat(in)
    def serialize(in: MythDateTimeString): String = in.toString
  }

  /**** UTILITY OBJECTS (not MythTV specific) ******/

  implicit object FileStatsSerializer extends MythProtocolSerializable[FileStats] {
    def deserialize(in: String): FileStats = deserialize(in split MythProtocol.SplitPattern)
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
    def deserialize(in: String): RecordedMarkup = deserialize(in split MythProtocol.SplitPattern)
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

trait BackendTypeSerializer[T] {
  def serialize(in: T): String
  def serialize(in: T, builder: StringBuilder): StringBuilder
}

// Idea here is a generic de-/serializer for backend objects that can be
//  - constructed from a Seq[String] and a FIELD_ORDER
//  - deconstructed using FIELD_ORDER and the apply-based fields accessor
trait GenericBackendObjectSerializer[T, F <: GenericBackendObjectFactory[T], S <: BackendTypeSerializer[T]]
    extends MythProtocolSerializable[T] {
  def newFactory: F
  def otherSerializer: S

  // TODO pass FIELD_ORDER into factory apply/constructor...

  // TODO do we really need to call take() in deserialize? Use a view slice instead?
  // What about deserialization of a adjacent sequence of like-typed objects?

  def deserialize(in: String): T = {
    val factory = newFactory
    val data: Seq[String] = in split MythProtocol.SplitPattern
    factory(data take factory.FIELD_ORDER.length)
  }

  override def deserialize(in: Seq[String]): T = {
    val factory = newFactory
    factory(in take factory.FIELD_ORDER.length)
  }

  def serialize(in: T): String = in match {
    case g: GenericBackendObject =>
      val factory = newFactory
      factory.FIELD_ORDER map (g(_)) mkString MythProtocol.BackendSeparator
    case _ => otherSerializer.serialize(in)
  }

  override def serialize(in: T, builder: StringBuilder): StringBuilder = in match {
    case g: GenericBackendObject =>
      val factory = newFactory
      (factory.FIELD_ORDER map (g(_))).addString(builder, MythProtocol.BackendSeparator)
    case _ => otherSerializer.serialize(in, builder)
  }
}

object ProgramInfoSerializerGeneric
  extends GenericBackendObjectSerializer[Recording, BackendProgramFactory, ProgramOtherSerializer] {
  def newFactory = BackendProgram
  def otherSerializer = BackendProgram
}

object FreeSpaceSerializerGeneric
  extends GenericBackendObjectSerializer[FreeSpace, BackendFreeSpaceFactory, FreeSpaceOtherSerializer] {
  def newFactory = BackendFreeSpace
  def otherSerializer = ???
}

object CardInputSerializerGeneric
  extends GenericBackendObjectSerializer[CardInput, BackendCardInputFactory, CardInputOtherSerializer] {
  def newFactory = BackendCardInput
  def otherSerializer = ???
}

object ChannelSerializerGeneric
  extends GenericBackendObjectSerializer[Channel, BackendChannelFactory, ChannelOtherSerializer] {
  def newFactory = BackendChannel
  def otherSerializer = ???
}

object UpcomingProgramSerializerGeneric
  extends GenericBackendObjectSerializer[UpcomingProgram, BackendUpcomingProgramFactory, UpcomingProgramOtherSerializer] {
  def newFactory = BackendUpcomingProgram
  def otherSerializer = ???
}
