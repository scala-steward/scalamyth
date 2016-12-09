package mythtv
package connection
package http
package json

import scala.util.Try

import spray.json.DefaultJsonProtocol.StringJsonFormat

import model.{ ImageId, ImageFileId, ImageSyncStatus }
import services.{ ImageService, ServiceResult }
import services.ImageInfo // TEMP
import RichJsonObject._

class JsonImageService(conn: BackendJsonConnection)
  extends JsonBackendService(conn)
     with ImageService {

  def getImageInfo(imageId: ImageFileId, exifTag: String): ServiceResult[String] = {
    val params: Map[String, Any] = Map("Id" -> imageId.id, "Tag" -> exifTag)
    for {
      response <- request("GetImageInfo", params)
      root     <- responseRoot(response, "String")
      result   <- Try(root.convertTo[String])
    } yield result
  }

  def getImageInfoList(imageId: ImageFileId): ServiceResult[List[ImageInfo]] = ???

  def removeImage(imageId: ImageId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("Id" -> imageId.id)
    for {
      response <- post("RemoveImage", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def renameImage(imageId: ImageId, newName: String): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("Id" -> imageId.id, "NewName" -> newName)
    for {
      response <- post("RenameImage", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def startSync(): ServiceResult[Boolean] = {
    for {
      response <- post("StartSync")
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def stopSync(): ServiceResult[Boolean] = {
    for {
      response <- post("StopSync")
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def getSyncStatus: ServiceResult[ImageSyncStatus] = {
    for {
      response <- request("GetSyncStatus")
      root     <- responseRoot(response, "ImageSyncInfo")
      result   <- Try(root.convertTo[ImageSyncStatus])
    } yield result
  }

  def createThumbnail(imageId: ImageId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("Id" -> imageId.id)
    for {
      response <- post("CreateThumbnail", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }
}
