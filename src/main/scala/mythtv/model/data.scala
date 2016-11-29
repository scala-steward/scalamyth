package mythtv
package model

import java.time.{ Instant, ZoneOffset }

import EnumTypes.Markup

trait Backend extends BackendOperations
trait Frontend extends FrontendOperations

trait IntegerIdentifier extends Any {
  def id: Int
}

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

trait ProgramLike extends PlayableMedia
trait RecordingLike extends ProgramLike
trait VideoLike extends PlayableMedia

trait TitleInfo {
  def title: String
  def inetRef: String

  override def toString: String = s"<TitleInfo $title, $inetRef>"
}

trait MythTvVersionInfo {
  def fullVersion: String
  def branch: String
  def protocol: String
  def binary: String
  def schema: String

  override def toString: String = s"$fullVersion $binary $protocol $schema"
}

trait DatabaseConnectionInfo {
  def host: String
  def port: Int
  def ping: Boolean
  def userName: String
  def password: String
  def dbName: String
  def driver: String
  def localEnabled: Boolean
  def localHostName: String

  override def toString: String = s"$userName@$host:$port/$dbName"
}

trait WakeOnLanInfo {
  def enabled: Boolean
  def reconnect: Int
  def retry: Int
  def command: String

  override def toString: String = s"<WakeOnLanInfo $enabled $command>"
}

trait ConnectionInfo {
  def version: MythTvVersionInfo
  def database: DatabaseConnectionInfo
  def wakeOnLan: WakeOnLanInfo
}
