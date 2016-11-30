package mythtv
package model

import java.net.URI

trait ArtworkInfo {
  def uri: URI
  def fileName: String
  def storageGroup: String
  def artworkType: String

  override def toString: String = {
    val uristring = uri.toString
    if (uristring.nonEmpty) uristring
    else s"$storageGroup:$fileName"
  }
}

// included in VideoService.lookupVideo results
trait ArtworkItem {
  def uri: URI
  def thumbnail: String
  def artworkType: String
  def width: Option[Int]
  def height: Option[Int]

  override def toString: String = s"$artworkType: $uri"
}
