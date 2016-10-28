package mythtv
package model

import java.time.{ Instant, ZoneOffset }

import EnumTypes.Markup

trait Backend extends BackendOperations
trait Frontend extends FrontendOperations

/* We define the Ordering objects in companion objects to the case classes
   because nested objects are currently prohibited inside value classes. */

/**
 * Represents the position of a video stream as a frame number.
 */
final case class VideoPosition(pos: Long) extends AnyVal

object VideoPosition {
  object VideoPositionOrdering extends Ordering[VideoPosition] {
    def compare(x: VideoPosition, y: VideoPosition): Int = x.pos compare y.pos
  }
  implicit def ordering: Ordering[VideoPosition] = VideoPositionOrdering
}

trait VideoSegment {
  def start: VideoPosition
  def end: VideoPosition
  override def toString: String = start.pos + ":" + end.pos
}

trait RecordedMarkup {
  def tag: Markup
  def position: VideoPosition
}

trait Settings {
  def hostName: String
  def settings: Map[String, String]

  override def toString: String = s"<Settings for $hostName (${settings.size})>"
}

trait TimeZoneInfo {
  def tzName: String
  def offset: ZoneOffset
  def currentTime: Instant

  override def toString: String = s"<TimeZoneInfo $tzName $offset>"
}

// TODO make a tuple type for (chanid, starttime) to shorten parameter lists?
//        and to ease switchover to 0.28+ recordedId in places?
trait PlayableMedia {
  // TODO what are subclasses?  Program(?), Recording, Video, music?
  // TODO methods on PlayableMedia
  def playOnFrontend(fe: Frontend): Boolean
}

trait ProgramLike extends PlayableMedia {
  // TODO
}

trait RecordingLike extends ProgramLike {
  // TODO
}

trait VideoLike extends PlayableMedia {
  // TODO
}

// TODO from services
trait LiveStreamInfo
trait FrontendStatus
trait FrontendAction
trait TitleInfo
