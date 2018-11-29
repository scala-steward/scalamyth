package mythtv
package model

import java.time.{ Duration, Instant, LocalDate }

import util.{ LooseEnum, MythFileHash }
import EnumTypes.{ ParentalLevel, VideoContentType }

final case class VideoId(id: Int) extends AnyVal with IntegerIdentifier

trait Video extends ProgramVideoBase with InternetMetadata with HasArtworkInfo {
  def id: VideoId
  def director: String
  def tagline: Option[String]
  def homePage: Option[String]
  def studio: Option[String]
  def length: Option[Duration]  // duration of video in minutes (may be zero == unknown)
  def playCount: Int
  def hash: MythFileHash
  def visible: Boolean
  def fileName: String
  def contentType: VideoContentType
  def hostName: String
  def addedDate: Option[Instant]  // database stores timestamp; VideoService returns valid date portion only
  def watched: Boolean
  def processed: Boolean
  def userRating: Double
  def rating: String       // This is MPPA or some such rating/certification
  def parentalLevel: ParentalLevel
  def collectionRef: Option[Int]
  def releasedDate: Option[LocalDate]
  def trailer: String
  def coverFile: String
  def screenshot: String
  def banner: String
  def fanart: String

  // Fields in the 'videometadata' DB table but not serialized? (at least not directly)
  // def childId: Int          // not serialized in JSON ?
  // def browse: Boolean       // not serialized in JSON ?
  // def playCommand: String   // not serialized in JSON ?
  // def category: Int         // not serialized in JSON ?

  // Cast list element present in services results beginning with MythTV 0.28 (but doesn't seem to be populated)
  //    --> was added to the MythTV codebase in commit 7f0a6196bd993408addeda439c4bbe08d5713fb3

  // Genres list present in services API results beginning with MythTV 29
  def genres: Set[String]

  override def toString: String = s"<Video $id $combinedTitle>"
}

private[mythtv] trait VideoGenre {
  def name: String
}

trait VideoLookup {
  def title: String
  def subtitle: String
  def season: Int
  def episode: Option[Int]
  def year: Int
  def tagline: String
  def description: String
  def certification: Option[String]
  def inetRef: String
  def collectionRef: String
  def homePage: String
  def releasedDate: Instant
  def userRating: Option[Double]
  def length: Option[Int]
  def language: String
  def countries: List[String]
  def popularity: Option[Int]
  def budget: Option[Int]
  def revenue: Option[Int]
  def imdb: Option[String]
  def tmsRef: Option[String]
  def artwork: List[ArtworkItem]

  override def toString: String = s"<VideoLookup $title $inetRef>"
}

trait BlurayInfo {
  def path: String
  def title: String
  def altTitle: String
  def discLang: String
  def discNumber: Int
  def totalDiscNumber: Int
  def titleCount: Int
  def thumbCount: Int
  def thumbPath: String
  def topMenuSupported: Boolean
  def firstPlaySupported: Boolean
  def numHdmvTitles: Int
  def numBdJTitles: Int
  def numUnsupportedTitles: Int
  def aacsDetected: Boolean
  def libaacsDetected: Boolean
  def aacsHandled: Boolean
  def bdplusDetected: Boolean
  def libbdplusDetected: Boolean
  def bdplusHandled: Boolean
}

object ParentalLevel extends LooseEnum {
  type ParentalLevel = Value
  final val None   = Value(0)
  final val Lowest = Value(1)
  final val Low    = Value(2)
  final val Medium = Value(3)
  final val High   = Value(4)
}

object VideoContentType extends Enumeration {
  type VideoContentType = Value
  final val Unknown    = Value("UNKNOWN")
  final val Movie      = Value("MOVIE")
  final val Television = Value("TELEVISION")
  final val Adult      = Value("ADULT")
  final val MusicVideo = Value("MUSICVIDEO")
  final val HomeVideo  = Value("HOMEVIDEO")
}
