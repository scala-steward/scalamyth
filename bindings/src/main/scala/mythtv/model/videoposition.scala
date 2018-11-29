package mythtv
package model

import util.{ BinaryByteCount, ByteCount, DecimalByteCount, DecimalByteCounter, LooseEnum }
import EnumTypes.Markup

/* We define the Ordering objects in companion objects to the case classes
   because nested objects are currently prohibited inside value classes. */

sealed trait VideoPosition extends Any {
  def pos: Long
  def units: String
}

/**
 * Represents the position of a video stream as a frame number.
 */
final case class VideoPositionFrame(pos: Long) extends AnyVal with VideoPosition {
  def units = "f"
}

/**
  * Represents the position of a video stream as number of seconds from the beginning.
  */
final case class VideoPositionSeconds(pos: Long) extends AnyVal with VideoPosition {
  def units = "s"
}

/**
  * Represents the position of a video stream as number of milliseconds from the beginning.
  */
final case class VideoPositionMilliseconds(pos: Long) extends AnyVal with VideoPosition {
  def units = "ms"
}

final case class VideoPositionBytes(pos: Long) extends AnyVal with VideoPosition with ByteCount with DecimalByteCounter {
  def units = "b"
  def bytes = pos

  def toBinaryByteCount = BinaryByteCount(pos)
  def toDecimalByteCount = DecimalByteCount(pos)

  def + (that: ByteCount): ByteCount = VideoPositionBytes(bytes + that.bytes)
  def - (that: ByteCount): ByteCount = VideoPositionBytes(bytes - that.bytes)
  def * (that: ByteCount): ByteCount = VideoPositionBytes(bytes * that.bytes)
  def / (that: ByteCount): ByteCount = VideoPositionBytes(bytes / that.bytes)
}

private[model] trait GenericVideoPositionCompanion[T <: VideoPosition] {
  /* This ordering object is only intended to be used on like subclasses of VideoPosition,
     e.g. comparing a VideoPositionFrame with a VideoPositionFrame, or a VideoPositionSeconds
     with a VideoPositionSeconds. If used on a heterogeous sequence of VideoPosition, the
     results may not be as expected. */
  object VideoPositionOrdering extends Ordering[T] {
    def compare(x: T, y: T): Int = x.pos compare y.pos
  }
  implicit def ordering: Ordering[T] = VideoPositionOrdering
}

object VideoPosition extends GenericVideoPositionCompanion[VideoPosition]
object VideoPositionFrame extends GenericVideoPositionCompanion[VideoPositionFrame]
object VideoPositionSeconds extends GenericVideoPositionCompanion[VideoPositionSeconds]
object VideoPositionMilliseconds extends GenericVideoPositionCompanion[VideoPositionMilliseconds]
object VideoPositionBytes extends GenericVideoPositionCompanion[VideoPositionBytes]

trait VideoSegment[VP <: VideoPosition] {
  def start: VP
  def end: VP
  override def toString: String = start.pos + ":" + end.pos + start.units
}

trait RecordedMarkup[VP <: VideoPosition] {
  def tag: Markup
  def position: VP

  override def toString: String = s"<RecordedMarkup $tag $position>"
}

// NB Only VideoPositionBytes and VideoPositionMilliseconds are valid with RecordedSeek
trait RecordedSeek[VP <: VideoPosition] {
  def mark: VideoPositionFrame
  def offset: VP

  override def toString: String = s"<RecordedSeek $mark $offset>"
}

object Markup extends LooseEnum {
  type Markup = Value
  final val All           = Value(-100)
  final val Unset         = Value(-10)
  final val TmpCutEnd     = Value(-5)
  final val TmpCutStart   = Value(-4)
  final val UpdatedCut    = Value(-3)
  final val Placeholder   = Value(-2)
  final val CutEnd        = Value(0)
  final val CutStart      = Value(1)
  final val Bookmark      = Value(2)
  final val BlankFrame    = Value(3)
  final val CommStart     = Value(4)
  final val CommEnd       = Value(5)
  final val GopStart      = Value(6)
  final val KeyFrame      = Value(7)
  final val SceneChange   = Value(8)
  final val GopByFrame    = Value(9)
  @deprecated("", "")
  final val Aspect1x1     = Value(10)
  final val Aspect4x3     = Value(11)
  final val Aspect16x9    = Value(12)
  final val Aspect221x1   = Value(13)
  final val AspectCustom  = Value(14)
  @deprecated("", "")
  final val VideoWidthOld = Value(15)
  final val VideoWidth    = Value(30)
  final val VideoHeight   = Value(31)
  final val VideoRate     = Value(32)  // new in 0.27
  final val DurationMs    = Value(33)  // new in 0.27
  final val TotalFrame    = Value(34)
}
