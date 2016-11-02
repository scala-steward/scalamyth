package mythtv
package connection
package http

import util.OptionalCount
import services.{ VideoService, PagedList }
import model.{ Video, VideoId }

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


  //def getBluray(path: String): BlurayInfo

  /* mutating POST methods */

  def addVideo(fileName: String, hostName: String): Boolean = ???

  def removeVideoFromDb(videoId: VideoId): Boolean = ???

  /* Added to API on 6 Apr 2016 */
  def updateVideoWatchedStatus(videoId: VideoId, watched: Boolean): Boolean = ???
}
