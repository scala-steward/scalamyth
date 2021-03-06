// SPDX-License-Identifier: LGPL-2.1-only
/*
 * ImageService.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package services

import model.{ ImageId, ImageSyncStatus }

// ImageService is new with MythTV 0.28
trait ImageService extends BackendService {
  final def serviceName: String = "Image"

  //def getImageInfo(imageId: ImageFileId, exifTag: String): ServiceResult[String]  // NOT YET IMPLEMENTED (NOP) on the backend?

  //def getImageInfoList(imageId: ImageFileId): ServiceResult[List[ImageInfo]]      // NOT FULLY IMPLEMENTED on backend (no useful info?)

  def removeImage(imageId: ImageId): ServiceResult[Boolean]

  def renameImage(imageId: ImageId, newName: String): ServiceResult[Boolean]

  def startSync(): ServiceResult[Boolean]
  def stopSync(): ServiceResult[Boolean]

  def getSyncStatus: ServiceResult[ImageSyncStatus]

  def createThumbnail(imageId: ImageId): ServiceResult[Boolean]
}
