package mythtv
package model

import java.net.URI

trait ArtworkBase {
  def uri: URI
  def artworkType: String
}

// returned by Services API
trait ArtworkInfo extends ArtworkBase {
  def fileName: String
  def storageGroup: String

  override def toString: String = {
    val uristring = uri.toString
    if (uristring.nonEmpty) uristring
    else s"$storageGroup:$fileName"
  }
}

// included in VideoService.lookupVideo results
trait ArtworkItem extends ArtworkBase {
  def thumbnail: String
  def width: Option[Int]
  def height: Option[Int]

  override def toString: String = s"$artworkType: $uri"
}

trait HasArtworkInfo {
  def artworkInfo: List[ArtworkInfo]  // TODO Better as a MAP keyed by artworkType?
}
