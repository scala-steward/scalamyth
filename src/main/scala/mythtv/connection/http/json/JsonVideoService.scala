package mythtv
package connection
package http
package json

import util.OptionalCount
import services.{ VideoService, PagedList }
import model.{ BlurayInfo, Video, VideoId }
import RichJsonObject._

class JsonVideoService(conn: BackendJsonConnection)
  extends JsonBackendService(conn)
     with VideoService {
  def getVideo(videoId: VideoId): Video = {
    val params: Map[String, Any] = Map("Id" -> videoId.id)
    val response = request("GetVideo", params)
    val root = responseRoot(response, "VideoMetadataInfo")
    root.convertTo[Video]
  }

  def getVideoByFileName(fileName: String): Video = {
    val params: Map[String, Any] = Map("FileName" -> fileName)
    val response = request("GetVideoByFileName", params)
    val root = responseRoot(response, "VideoMetadataInfo")
    root.convertTo[Video]
  }

  def getVideoList(startIndex: Int, count: OptionalCount[Int], descending: Boolean): PagedList[Video] = {
    var params = buildStartCountParams(startIndex, count)
    if (descending) params += "Descending" -> descending
    val response = request("GetVideoList", params)
    val root = responseRoot(response, "VideoMetadataInfoList")
    root.convertTo[MythJsonPagedObjectList[Video]]
  }

  def getBluray(path: String): BlurayInfo = {
    val params: Map[String, Any] = Map("Path" -> path)
    val response = request("GetBluray", params)
    val root = responseRoot(response, "BlurayInfo")
    root.convertTo[BlurayInfo]
  }

  /* mutating POST methods */

  def addVideo(fileName: String, hostName: String): Boolean = {
    val params: Map[String, Any] = Map(
      "FileName" -> fileName,
      "HostName" -> hostName
    )
    val response = post("AddVideo", params)
    val root = responseRoot(response)
    root.booleanField("bool")   // TODO test
  }

  def removeVideoFromDb(videoId: VideoId): Boolean = {
    val params: Map[String, Any] = Map("Id" -> videoId.id)
    val response = post("RemoveVideoFromDB", params)
    val root = responseRoot(response)
    root.booleanField("bool")   // TODO test
  }

  /* Added to API on 6 Apr 2016 */
  def updateVideoWatchedStatus(videoId: VideoId, watched: Boolean): Boolean = {
    val params: Map[String, Any] = Map("Id" -> videoId.id, "Watched" -> watched)
    val response = post("UpdateVideoWatchedStatus", params)
    val root = responseRoot(response)
    root.booleanField("bool")   // TODO test
  }
}
