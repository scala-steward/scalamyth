package mythtv
package model

import util.LooseEnum
import EnumTypes.MusicImageType

final case class SongId(id: Int) extends AnyVal with IntegerIdentifier

final case class MusicImageId(id: Int) extends AnyVal with IntegerIdentifier

trait RemoteSong {
  val hostName: String
  val songId: SongId
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
