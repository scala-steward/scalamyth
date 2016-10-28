package mythtv
package model

import java.time.{ Duration, Instant, LocalDate, Year }

import util.MythFileHash

final case class VideoId(id: Int) extends AnyVal

trait Video {
  def id: VideoId
  def title: String
  def subtitle: String
  def director: String
  def year: Year
  // TODO more
  // TODO check these against database, they are from VideoMetadataInfo service
  def tagline: Option[String]
  def description: String
  def inetRef: String
  def homePage: Option[String]
  def studio: Option[String]
  def season: Option[Int]
  def episode: Option[Int]
  def length: Option[Duration]  // duration of video in minutes (may be zero == unknown)
  def playCount: Int
  def hash: MythFileHash
  def visible: Boolean
  def fileName: String
  def contentType: String  // enum? MOVIE, TELEVISION, ADULT, MUSICVIDEO, HOMEVIDEO
  def hostName: String
  def addDate: Option[Instant]
  def watched: Boolean
  def userRating: Double   // TODO rename to stars and converge with Program? Scale is always implied!
  def rating: String       // This is MPPA or some such rating, correct?  Should this really be a Map? (RatingBody -> Rating)
  def collectionRef: Int
  def releaseDate: LocalDate
  // TODO various artworks   These are common to many elements, no?  What should a "HasArtwork" (or may have artwork, really...) trait be called?

  def combinedTitle: String = combinedTitle(": ")
  def combinedTitle(sep: String): String =
    if (subtitle.nonEmpty) title + sep + subtitle
    else title
  override def toString: String = s"<Video $id $combinedTitle>"
}
