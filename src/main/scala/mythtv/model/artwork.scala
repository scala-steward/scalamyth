package mythtv
package model

trait ArtworkInfo {
  def url: String   // TODO use URI class? can it handle empty string?
  def fileName: String
  def storageGroup: String
  def artworkType: String

  override def toString: String =
    if (url.nonEmpty) url
    else s"$storageGroup:$fileName"
}

// included in VideoService.lookupVideo results
trait ArtworkItem {
  def url: String   // TODO use URI class?
  def thumbnail: String
  def artworkType: String
  def width: Option[Int]
  def height: Option[Int]

  override def toString: String = s"$artworkType: $url"
}
