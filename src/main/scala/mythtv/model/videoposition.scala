package mythtv
package model

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

trait VideoSegment {
  def start: VideoPositionFrame
  def end: VideoPositionFrame
  override def toString: String = start.pos + ":" + end.pos
}

trait RecordedMarkup {
  def tag: Markup
  def position: VideoPositionFrame
}
