package mythtv
package connection
package myth
package data

import model.{ AlbumArtImage, MusicImageId, MusicImageType }
import model.EnumTypes.MusicImageType

private[myth] class BackendAlbumArtImage(data: Seq[String], fieldOrder: IndexedSeq[String])
    extends GenericBackendObject(data, fieldOrder) with AlbumArtImage {

  override def toString: String = s"<BackendAlbumArtImage $id $imageType $embedded $fileName>"

  def id: MusicImageId = MusicImageId(fields("imageId").toInt)
  def imageType: MusicImageType = MusicImageType.applyOrUnknown(fields("imageType").toInt)
  def embedded: Boolean = fields("embedded").toInt != 0
  def description: String = fields("description")
  def fileName: String = fields("fileName")
  def hostName: String = fields("hostName")
}

private[myth] trait BackendAlbumArtImageFactory extends GenericBackendObjectFactory[AlbumArtImage]
private[myth] trait AlbumArtImageOtherSerializer extends BackendTypeSerializer[AlbumArtImage]

private[myth] object BackendAlbumArtImage extends BackendAlbumArtImageFactory {
  final val FIELD_ORDER = IndexedSeq(
    "imageId", "imageType", "embedded", "description", "fileName", "hostName"
  )
  def apply(data: Seq[String]): BackendAlbumArtImage = new BackendAlbumArtImage(data, FIELD_ORDER)
}
