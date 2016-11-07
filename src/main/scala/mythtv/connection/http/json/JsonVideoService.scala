package mythtv
package connection
package http
package json

import util.OptionalCount
import services.{ VideoService, PagedList }
import model.{ BlurayInfo, Video, VideoId, VideoLookup }
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

  def lookupVideo(title: String, subtitle: String, inetRef: String, season: Int, episode: Int,
    grabberType: String, allowGeneric: Boolean): List[VideoLookup] = {
    var params: Map[String, Any] = Map.empty
    if (title.nonEmpty)    params += "Title" -> title
    if (subtitle.nonEmpty) params += "Subtitle" -> subtitle
    if (inetRef.nonEmpty)  params += "Inetref" -> inetRef
    if (season != 0)       params += "Season" -> season
    if (episode != 0)      params += "Episode" -> episode
    if (grabberType.nonEmpty) params += "GrabberType" -> grabberType
    if (allowGeneric)      params += "AllowGeneric" -> allowGeneric
    val response = request("LookupVideo", params)
    val root = responseRoot(response, "VideoLookupList")
    val list = root.convertTo[MythJsonObjectList[VideoLookup]]
    list.data
  }

  /* mutating POST methods */

  def addVideo(fileName: String, hostName: String): Boolean = {
    val params: Map[String, Any] = Map(
      "FileName" -> fileName,
      "HostName" -> hostName
    )
    val response = post("AddVideo", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  def removeVideoFromDb(videoId: VideoId): Boolean = {
    val params: Map[String, Any] = Map("Id" -> videoId.id)
    val response = post("RemoveVideoFromDB", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  /* Added to API on 6 Apr 2016 */
  def updateVideoWatchedStatus(videoId: VideoId, watched: Boolean): Boolean = {
    val params: Map[String, Any] = Map("Id" -> videoId.id, "Watched" -> watched)
    val response = post("UpdateVideoWatchedStatus", params)
    val root = responseRoot(response)
    root.booleanField("bool")   // TODO test
  }
}
