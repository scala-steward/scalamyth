package mythtv
package model

import java.time.{ Duration, Instant, LocalDate, Year }

import util.MythFileHash

final case class VideoId(id: Int) extends AnyVal

trait Video extends ProgramAndVideoBase with InternetMetadata {
  def id: VideoId
  def director: String
  // TODO more
  // TODO check these against database, they are from VideoMetadataInfo service
  def tagline: Option[String]
  def homePage: Option[String]
  def studio: Option[String]
  def length: Option[Duration]  // duration of video in minutes (may be zero == unknown)
  def playCount: Int
  def hash: MythFileHash
  def visible: Boolean
  def fileName: String
  def contentType: String  // enum? MOVIE, TELEVISION, ADULT, MUSICVIDEO, HOMEVIDEO
  def hostName: String
  def addedDate: Option[Instant]  // database stores timestamp; VideoService returns valid date portion only
  def watched: Boolean
  def userRating: Double   // TODO rename to stars and converge with Program? Scale is always implied!
  def rating: String       // This is MPPA or some such rating, correct?  Should this really be a Map? (RatingBody -> Rating)
  def collectionRef: Option[Int]
  def releasedDate: Option[LocalDate]
  // TODO various artworks   These are common to many elements, no?  What should a "HasArtwork" (or may have artwork, really...) trait be called?

  def artworkInfo: List[ArtworkInfo]  // TODO probably belongs in a different, HasArtworkInfo (?) trait

  def combinedTitle: String = combinedTitle(": ")
  def combinedTitle(sep: String): String =
    if (subtitle.nonEmpty) title + sep + subtitle
    else title
  override def toString: String = s"<Video $id $combinedTitle>"
}

trait ArtworkItem // TODO different that ArtworkInfo

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
