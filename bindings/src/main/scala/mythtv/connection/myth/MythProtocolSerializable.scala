// SPDX-License-Identifier: LGPL-2.1-only
/*
 * MythProtocolSerializable.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth

import java.time.{ Instant, LocalDate, Year }

import scala.util.Try
import scala.collection.compat.immutable.ArraySeq.{ unsafeWrapArray }

import EnumTypes.SeekWhence
import data._
import util._
import model._
import model.EnumTypes._

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
      try in.toInt != 0  // handles "1" / "0"
      catch {
        case _: NumberFormatException => in.toBoolean // handles "True" and "False" (case-insensitive)
      }
    }
    def serialize(in: Boolean): String = if (in) "1" else "0"
  }

  trait IntegerIdentifierSerializer[T <: IntegerIdentifier] extends MythProtocolSerializable[T] {
    def factory: Int => T
    def deserialize(in: String): T = factory(in.toInt)
    def serialize(in: T): String = in.id.toString
    override def serialize(in: T, builder: StringBuilder): StringBuilder = builder.append(in.id)
  }

  trait IntBitmaskEnumSerializer[T <: IntBitmaskEnum#Base] extends MythProtocolSerializable[T] {
    def factory: Int => T
    def deserialize(in: String): T = factory(in.toInt)
    def serialize(in: T): String = in.id.toString
    override def serialize(in: T, builder: StringBuilder): StringBuilder = builder.append(in.id)
  }

  trait EnumerationSerializer[T <: Enumeration#Value] extends MythProtocolSerializable[T] {
    def factory: Int => T
    def deserialize(in: String): T = factory(in.toInt)
    def serialize(in: T): String = in.id.toString
    override def serialize(in: T, builder: StringBuilder): StringBuilder = builder.append(in.id)
  }

  implicit object CaptureCardIdSerializer extends IntegerIdentifierSerializer[CaptureCardId] {
    def factory = CaptureCardId.apply
  }

  implicit object CategoryTypeSerializer extends EnumerationSerializer[CategoryType] {
    def factory = CategoryType.applyOrUnknown
  }

  implicit object ChanIdSerializer extends IntegerIdentifierSerializer[ChanId] {
    def factory = ChanId.apply
  }

  implicit object ChannelNumberSerializer extends MythProtocolSerializable[ChannelNumber] {
    def deserialize(in: String): ChannelNumber = ChannelNumber(in)
    def serialize(in: ChannelNumber): String = in.num
    override def serialize(in: ChannelNumber, builder: StringBuilder): StringBuilder = builder.append(in.num)
  }

  implicit object FileTranferIdSerializer extends IntegerIdentifierSerializer[FileTransferId] {
    def factory = FileTransferId.apply
  }

  implicit object ImageIdSerializer extends IntegerIdentifierSerializer[ImageId] {
    def factory = ImageUnknownId.apply
  }

  implicit object ImageDirIdSerializer extends IntegerIdentifierSerializer[ImageDirId] {
    def factory = ImageDirId.apply
  }

  implicit object ImageFileIdSerializer extends IntegerIdentifierSerializer[ImageFileId] {
    def factory = ImageFileId.apply
  }

  implicit object ImageFileTransformSerializer extends EnumerationSerializer[ImageFileTransform] {
    def factory = ImageFileTransform.applyOrUnknown
  }

  implicit object InputIdSerializer extends IntegerIdentifierSerializer[InputId] {
    def factory = InputId.apply
  }

  implicit object ListingSourceIdSerializer extends IntegerIdentifierSerializer[ListingSourceId] {
    def factory = ListingSourceId.apply
  }

  implicit object LiveTvChainIdSerializer extends MythProtocolSerializable[LiveTvChainId] {
    def deserialize(in: String): LiveTvChainId = LiveTvChainId(in)
    def serialize(in: LiveTvChainId): String = in.id
  }

  implicit object MultiplexIdSerializer extends IntegerIdentifierSerializer[MultiplexId] {
    def factory = MultiplexId.apply
  }

  implicit object MusicImageIdSerializer extends IntegerIdentifierSerializer[MusicImageId] {
    def factory = MusicImageId.apply
  }

  implicit object MusicImageTypeSerializer extends EnumerationSerializer[MusicImageType] {
    def factory = MusicImageType.applyOrUnknown
  }

  implicit object RecordRuleIdSerializer extends IntegerIdentifierSerializer[RecordRuleId] {
    def factory = RecordRuleId.apply
  }

  implicit object RecStatusSerializer extends EnumerationSerializer[RecStatus] {
    def factory = RecStatus.applyOrUnknown
  }

  implicit object SleepStatusSerializer extends EnumerationSerializer[SleepStatus] {
    def factory = SleepStatus.applyOrUnknown
  }

  implicit object SongIdSerialzier extends IntegerIdentifierSerializer[SongId] {
    def factory = SongId.apply
  }

  implicit object TvStateSerializer extends EnumerationSerializer[TvState] {
    def factory = TvState.applyOrUnknown
  }

  implicit object VideoPositionFrameSerializer extends MythProtocolSerializable[VideoPositionFrame] {
    def deserialize(in: String): VideoPositionFrame = VideoPositionFrame(in.toLong)
    def serialize(in: VideoPositionFrame): String = in.pos.toString
    override def serialize(in: VideoPositionFrame, builder: StringBuilder): StringBuilder = builder.append(in.pos)
  }

  implicit object VideoFrameSecondsSerializer extends MythProtocolSerializable[VideoPositionSeconds] {
    def deserialize(in: String): VideoPositionSeconds = VideoPositionSeconds(in.toLong)
    def serialize(in: VideoPositionSeconds): String = in.pos.toString
    override def serialize(in: VideoPositionSeconds, builder: StringBuilder): StringBuilder = builder.append(in.pos)
  }

  implicit object DupInSerializer extends IntBitmaskEnumSerializer[DupCheckIn] {
    def factory = DupCheckIn.apply
  }

  implicit object DupCheckMethodSerializer extends IntBitmaskEnumSerializer[DupCheckMethod] {
    def factory = DupCheckMethod.apply
  }

  implicit object AudioPropertiesSerializer extends IntBitmaskEnumSerializer[AudioProperties] {
    def factory = AudioProperties.apply
  }

  implicit object VideoPropertiesSerializer extends IntBitmaskEnumSerializer[VideoProperties] {
    def factory = VideoProperties.apply
  }

  implicit object SubtitleTypeSerializer extends IntBitmaskEnumSerializer[SubtitleType] {
    def factory = SubtitleType.apply
  }

  implicit object ProgramFlagsSerializer extends IntBitmaskEnumSerializer[ProgramFlags] {
    def factory = ProgramFlags.apply
  }

  implicit object RecTypeSerializer extends EnumerationSerializer[RecType] {
    def factory = RecType.applyOrUnknown
  }

  implicit object SeekWhenceSerializer extends EnumerationSerializer[SeekWhence] {
    def factory = SeekWhence.apply
  }

  implicit object InstantSerializer extends MythProtocolSerializable[Instant] {
    def deserialize(in: String): Instant = Instant.parse(in)
    def serialize(in: Instant): String = in.toString
  }

  implicit object LocalDateOptionSerializer extends MythProtocolSerializable[Option[LocalDate]] {
    def deserialize(in: String): Option[LocalDate] = Try(LocalDate.parse(in)).toOption
    def serialize(in: Option[LocalDate]): String = if (in.isEmpty) "" else in.get.toString
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

  implicit object StringOptionSerializer extends MythProtocolSerializable[Option[String]] {
    def deserialize(in: String): Option[String] = if (in.isEmpty) None else Some(in)
    def serialize(in: Option[String]): String = if (in.isEmpty) "" else in.get
  }

  trait IntegerIdentifierOptionSerializer[T <: IntegerIdentifier] extends MythProtocolSerializable[Option[T]] {
    def factory: Int => T
    def deserialize(in: String): Option[T] = { Try(in.toInt) filter (_ != 0) }.toOption map factory
    def serialize(in: Option[T]): String = if (in.isEmpty) "0" else in.get.id.toString
  }

  implicit object RecordRuleIdOptionSerializer extends IntegerIdentifierOptionSerializer[RecordRuleId] {
    def factory = RecordRuleId.apply
  }

  implicit object MythDateTimeSerializer extends MythProtocolSerializable[MythDateTime] {
    def deserialize(in: String): MythDateTime = {
      try MythDateTime.fromTimestamp(in.toLong)
      catch {
        case _ : NumberFormatException =>
          try MythDateTime.fromNaiveIso(in)
          catch {
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

  implicit object ImageIdListSerializer extends MythProtocolSerializable[Seq[ImageId]] {
    def deserialize(in: String): Seq[ImageId] =
      if (in.isEmpty) Nil
      else unsafeWrapArray(in split ',' map (s => ImageUnknownId(s.trim.toInt)))
    def serialize(in: Seq[ImageId]): String = in.map(_.id).mkString(",")
  }

  implicit object RecordedIdSerializer extends MythProtocolSerializable[RecordedId] {
    import RecordedId.{ RecordedIdInt, RecordedIdChanTime }
    def deserialize(in: String): RecordedId = RecordedIdInt(in.toInt)
    def serialize(in: RecordedId): String = in match {
      case RecordedIdInt(id) => id.toString
      case RecordedIdChanTime(chanId, recStartTs) =>  // is this case useful? somewhat arbitrary definition...
        MythProtocol.serialize(chanId) + "_" + MythProtocol.serialize(recStartTs)
    }
  }

  implicit object StorageGroupInfoSerializer extends MythProtocolSerializable[StorageGroupInfo] {
    def deserialize(in: String): StorageGroupInfo = deserialize(in split "::")
    private def deserialize(in: Array[String]): StorageGroupInfo = deserialize(unsafeWrapArray(in))
    override def deserialize(in: Seq[String]): StorageGroupInfo = {
      in(0) match {
        case "sgdir" => StorageGroupInfoRoot(in(1))
        case "dir"   => StorageGroupInfoDir(in(1))
        case "file"  => StorageGroupInfoFile(in(1), DecimalByteCount(MythProtocol.deserialize[Long](in(2))), in(3))
        case _       => ???
      }
    }
    def serialize(in: StorageGroupInfo): String = {
      val parts = in match {
        case StorageGroupInfoRoot(rootDir) => List("sgdir", rootDir)
        case StorageGroupInfoDir(dirName) => List("dir", dirName, "0")
        case StorageGroupInfoFile(fileName, size, fullPath) =>
          List("file", fileName, size.b.toString, fullPath)
      }
      parts mkString "::"
    }
  }

  /**** UTILITY OBJECTS (not MythTV specific) ******/

  implicit object FileStatsSerializer extends MythProtocolSerializable[FileStats] {
    def deserialize(in: String): FileStats = deserialize(in split MythProtocol.SplitPattern)
    private def deserialize(in: Array[String]): FileStats = deserialize(unsafeWrapArray(in))
    override def deserialize(in: Seq[String]): FileStats = FileStats(
      MythProtocol.deserialize[Long](in(0)),                         // st_dev
      MythProtocol.deserialize[Long](in(1)),                         // st_ino
      MythProtocol.deserialize[Int](in(2)),                          // st_mode
      MythProtocol.deserialize[Long](in(3)),                         // st_nlink
      MythProtocol.deserialize[Int](in(4)),                          // st_uid
      MythProtocol.deserialize[Int](in(5)),                          // st_gid
      MythProtocol.deserialize[Long](in(6)),                         // st_rdev
      DecimalByteCount(MythProtocol.deserialize[Long](in(7))),       // st_size
      BinaryByteCount(MythProtocol.deserialize[Long](in(8))),        // st_blksize
      MythProtocol.deserialize[Long](in(9)),                         // st_blocks
      Instant.ofEpochSecond(MythProtocol.deserialize[Long](in(10))), // st_atim
      Instant.ofEpochSecond(MythProtocol.deserialize[Long](in(11))), // st_mtim
      Instant.ofEpochSecond(MythProtocol.deserialize[Long](in(12)))  // st_ctim
    )
    def serialize(in: FileStats): String = throw new UnsupportedOperationException
  }

  /**** BACKEND OBJECTS (protocol version neutral)  ***/

  implicit object RecordedMarkupSerializer extends MythProtocolSerializable[RecordedMarkupFrame] {
    def deserialize(in: String): RecordedMarkupFrame = deserialize(in split MythProtocol.SplitPattern)
    private def deserialize(in: Array[String]): RecordedMarkupFrame = deserialize(unsafeWrapArray(in))
    override def deserialize(in: Seq[String]): RecordedMarkupFrame = {
      assert(in.lengthCompare(1) > 0)
      BackendRecordedMarkup(
        Markup.applyOrUnknown(MythProtocol.deserialize[Int](in(0))),
        MythProtocol.deserialize[VideoPositionFrame](in(1))
      )
    }
    def serialize(in: RecordedMarkupFrame): String = throw new UnsupportedOperationException
  }
}

private[myth] trait BackendTypeSerializer[T] {
  def serialize(in: T): String
  def serialize(in: T, builder: StringBuilder): StringBuilder
}

// Idea here is a generic de-/serializer for backend objects that can be
//  - constructed from a Seq[String] and a FIELD_ORDER
//  - deconstructed using FIELD_ORDER and the apply-based fields accessor
private[myth] trait GenericBackendObjectSerializer[T, F <: GenericBackendObjectFactory[T], S <: BackendTypeSerializer[T]]
    extends MythProtocolSerializable[T] {
  def newFactory: F
  def otherSerializer: S

  def fieldCount: Int = fieldCount(newFactory)

  private def fieldCount(factory: F): Int = factory.FIELD_ORDER.length

  // What about deserialization of an adjacent sequence of like-typed objects?

  def deserialize(in: String): T = {
    val factory = newFactory
    val data: Seq[String] = unsafeWrapArray(in split MythProtocol.SplitPattern)
    factory(data take fieldCount(factory))
  }

  override def deserialize(in: Seq[String]): T = {
    val factory = newFactory
    factory(in take fieldCount(factory))
  }

  def serialize(in: T): String = in match {
    case g: GenericBackendObject =>
      val factory = newFactory
      factory.FIELD_ORDER map (g(_)) mkString MythProtocol.Separator
    case _ => otherSerializer.serialize(in)
  }

  override def serialize(in: T, builder: StringBuilder): StringBuilder = in match {
    case g: GenericBackendObject =>
      val factory = newFactory
      (factory.FIELD_ORDER map (g(_))).addString(builder, MythProtocol.Separator)
    case _ => otherSerializer.serialize(in, builder)
  }
}

private[myth] object ProgramInfoSerializer75
  extends GenericBackendObjectSerializer[Recording, BackendProgramFactory, ProgramOtherSerializer] {
  def newFactory = BackendProgram75
  def otherSerializer = BackendProgram75
}

private[myth] object ProgramInfoSerializer77
  extends GenericBackendObjectSerializer[Recording, BackendProgramFactory, ProgramOtherSerializer] {
  def newFactory = BackendProgram77
  def otherSerializer = BackendProgram77
}

private[myth] object ProgramInfoSerializer88
  extends GenericBackendObjectSerializer[Recording, BackendProgramFactory, ProgramOtherSerializer] {
  def newFactory = BackendProgram88
  def otherSerializer = BackendProgram88
}

private[myth] object AlbumArtImageSerializerRef
  extends GenericBackendObjectSerializer[AlbumArtImage, BackendAlbumArtImageFactory, AlbumArtImageOtherSerializer] {
  def newFactory = BackendAlbumArtImage
  def otherSerializer = ???
}

private[myth] object FreeSpaceSerializerRef
  extends GenericBackendObjectSerializer[FreeSpace, BackendFreeSpaceFactory, FreeSpaceOtherSerializer] {
  def newFactory = BackendFreeSpace
  def otherSerializer = ???
}

private[myth] object CardInputSerializerRef
  extends GenericBackendObjectSerializer[CardInput, BackendCardInputFactory, CardInputOtherSerializer] {
  def newFactory = BackendCardInput
  def otherSerializer = ???
}

private[myth] object ChannelSerializerRef
  extends GenericBackendObjectSerializer[Channel, BackendChannelFactory, ChannelOtherSerializer] {
  def newFactory = BackendChannel
  def otherSerializer = ???
}

private[myth] object InputSerializerRef
  extends GenericBackendObjectSerializer[Input, BackendInputFactory, InputOtherSerializer] {
  def newFactory = BackendInput
  def otherSerializer = ???
}

private[myth] object InputSerializer91
  extends GenericBackendObjectSerializer[Input, BackendInputFactory, InputOtherSerializer] {
  def newFactory = BackendInput91
  def otherSerializer = ???
}

private[myth] object LiveTvChainSerializerRef
  extends GenericBackendObjectSerializer[LiveTvChain, BackendLiveTvChainFactory, LiveTvChainOtherSerializer] {
  def newFactory = BackendLiveTvChain
  def otherSerializer = ???
}

private[myth] object UpcomingProgramSerializerRef
  extends GenericBackendObjectSerializer[UpcomingProgram, BackendUpcomingProgramFactory, UpcomingProgramOtherSerializer] {
  def newFactory = BackendUpcomingProgram
  def otherSerializer = ???
}
