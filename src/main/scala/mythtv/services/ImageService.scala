package mythtv
package services

import model.ImageId

trait ImageInfo // TODO TEMP
trait ImageSyncInfo // TODO TEMP

// ImageService is new with MythTV 0.28
trait ImageService extends BackendService {
  final def serviceName: String = "Image"

  def getImageInfo(imageId: ImageId, exifTag: String): ServiceResult[String]  // NOT YET IMPLEMENTED (NOP) on the backend?

  def getImageInfoList(imageId: ImageId): ServiceResult[List[ImageInfo]]

  def removeImage(imageId: ImageId): ServiceResult[Boolean]

  def renameImage(imageId: ImageId, newName: String): ServiceResult[Boolean]

  def startSync(): ServiceResult[Boolean]
  def stopSync(): ServiceResult[Boolean]

  def getSyncStatus: ServiceResult[ImageSyncInfo]

  def createThumbnail(imageId: ImageId): ServiceResult[Boolean]
}
