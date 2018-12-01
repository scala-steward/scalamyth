// SPDX-License-Identifier: LGPL-2.1-only
/*
 * AbstractImageService.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package http

import model.{ ImageId, ImageSyncStatus }
import services.{ ImageService, ServiceResult }

trait AbstractImageService extends ServiceProtocol with ImageService {
  /*
  def getImageInfo(imageId: ImageFileId, exifTag: String): ServiceResult[String] = {
    val params: Map[String, Any] = Map("Id" -> imageId.id, "Tag" -> exifTag)
    request("GetImageInfo", params)()
  }

  def getImageInfoList(imageId: ImageFileId): ServiceResult[List[ImageInfo]] = ???
  */

  def removeImage(imageId: ImageId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("Id" -> imageId.id)
    post("RemoveImage", params)()
  }

  def renameImage(imageId: ImageId, newName: String): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("Id" -> imageId.id, "NewName" -> newName)
    post("RenameImage", params)()
  }

  def startSync(): ServiceResult[Boolean] = {
    post("StartSync")()
  }

  def stopSync(): ServiceResult[Boolean] = {
    post("StopSync")()
  }

  def getSyncStatus: ServiceResult[ImageSyncStatus] = {
    request("GetSyncStatus")("ImageSyncInfo")
  }

  def createThumbnail(imageId: ImageId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("Id" -> imageId.id)
    post("CreateThumbnail", params)()
  }
}
