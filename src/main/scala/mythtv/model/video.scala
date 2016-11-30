package mythtv
package model

import java.time.{ Duration, Instant, LocalDate }

import util.MythFileHash
import EnumTypes.VideoContentType

final case class VideoId(id: Int) extends AnyVal with IntegerIdentifier

trait Video extends ProgramVideoBase with InternetMetadata {
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
  def userRating: Double
  def rating: String       // This is MPPA or some such rating/certification
  def collectionRef: Option[Int]
  def releasedDate: Option[LocalDate]

  // TODO various artworks   These are common to many elements, no?  What should a "HasArtwork" (or may have artwork, really...) trait be called?
  def artworkInfo: List[ArtworkInfo]  // TODO probably belongs in a different, HasArtworkInfo (?) trait

  // Fields in the 'videometadata' DB table but not serialized? (at least not directly)
  // def showLevel: Int        // not serialized in JSON ?
  // def childId: Int          // not serialized in JSON ?
  // def browse: Boolean       // not serialized in JSON ?
  // def processed: Boolean    // not serialized in JSON ?
  // def playCommand: String   // not serialized in JSON ?
  // def category: Int         // not serialized in JSON ?
  // def trailer: String       // not serialized in JSON ?
  // def coverfile: String     // not serialized in JSON ?
  // def screenshot: String    // not serialized in JSON ?
  // def banner: String        // not serialized in JSON ?
  // def fanart: String        // not serialized in JSON ?

  override def toString: String = s"<Video $id $combinedTitle>"
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
