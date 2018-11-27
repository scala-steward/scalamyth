package mythtv
package model

import java.time.{ Duration, Year }

import util.{ LooseEnum, MythDateTime }
import EnumTypes.MusicImageType

final case class SongId(id: Int) extends AnyVal with IntegerIdentifier

final case class MusicImageId(id: Int) extends AnyVal with IntegerIdentifier

// Based on MusicMetadataInfo used by services
trait Song {
  def songId: SongId
  def title: String
  def artist: String
  def compilationArtist: String
  def album: String
  def trackNumber: Int
  def genre: String
  def year: Option[Year]
  def isCompilation: Boolean
  def length: Duration
  def rating: Int     // what units?

  def fileName: String
  def hostName: String

  def playCount: Int
  def lastPlayed: Option[MythDateTime]

  override def toString: String = s"<Song $songId $title [$artist/$album]>"
}

trait RemoteSong {
  def hostName: String
  def songId: SongId
}

object MusicImageType extends LooseEnum {
  type MusicImageType = Value
  val Unknown    = Value(0)
  val FrontCover = Value(1)
  val BackCover  = Value(2)
  val CD         = Value(3)
  val Inlay      = Value(4)
  val Artist     = Value(5)
}

trait AlbumArtImage {
  def id: MusicImageId
  def imageType: MusicImageType
  def embedded: Boolean
  def description: String
  def fileName: String
  def hostName: String
}
