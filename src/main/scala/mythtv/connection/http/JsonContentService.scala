package mythtv
package connection
package http

import spray.json.DefaultJsonProtocol

import model.{ ChanId, LiveStreamInfo }
import services.ContentService
import util.{ MythFileHash, MythDateTime }

import services.DataBytes // FIXME temporary placeholder

class JsonContentService(conn: BackendJsonConnection)
  extends JsonBackendService(conn)
     with ContentService {
  def getFileList(storageGroup: String): List[String] = {
    val params: Map[String, Any] = Map("StorageGroup" -> storageGroup)
    val response = request("GetFileList", params)
    val root = responseRoot(response)
    root.convertTo[List[String]]
  }

  // TODO handle error conditions, such as file not existing...
  def getHash(storageGroup: String, fileName: String): MythFileHash = {
    import DefaultJsonProtocol.StringJsonFormat
    val params: Map[String, Any] = Map("StorageGroup" -> storageGroup, "FileName" -> fileName)
    val response = request("GetHash", params)
    val root = responseRoot(response, "String")
    new MythFileHash(root.convertTo[String])
  }

  def getAlbumArt(id: Int): DataBytes = ???
  def getFile(storageGroup: String, fileName: String): DataBytes = ???
  def getImageFile(storageGroup: String, fileName: String): DataBytes = ???
  def getLiveStream(id: String): LiveStreamInfo = ???
  def getLiveStreamList: List[LiveStreamInfo] = ???
  def getMusic(id: String): DataBytes = ???
  def getPreviewImage(chanId: ChanId, startTime: MythDateTime): DataBytes = ???
  def getRecording(chanId: ChanId, startTime: MythDateTime): DataBytes = ???
  def getVideo(id: Int): DataBytes = ???
}
