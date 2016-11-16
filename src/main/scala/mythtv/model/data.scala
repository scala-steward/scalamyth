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
final case class VideoPositionFrame(pos: Long) extends AnyVal

object VideoPositionFrame {
  object VideoPositionFrameOrdering extends Ordering[VideoPositionFrame] {
    def compare(x: VideoPositionFrame, y: VideoPositionFrame): Int = x.pos compare y.pos
  }
  implicit def ordering: Ordering[VideoPositionFrame] = VideoPositionFrameOrdering
}

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

trait ArtworkInfo {
  def url: String
  def fileName: String
  def storageGroup: String
  def artworkType: String

  override def toString: String =
    if (url.nonEmpty) url
    else s"$storageGroup:$fileName"
}

// included in VideoService.lookupVideo results
trait ArtworkItem {
  def url: String
  def thumbnail: String
  def artworkType: String
  def width: Option[Int]
  def height: Option[Int]

  override def toString: String = s"$artworkType: $url"
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
