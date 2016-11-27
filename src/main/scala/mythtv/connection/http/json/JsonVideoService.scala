package mythtv
package connection
package http
package json

import scala.util.Try

import util.OptionalCount
import services.{ VideoService, PagedList, ServiceResult }
import model.{ BlurayInfo, Video, VideoId, VideoLookup }
import RichJsonObject._

class JsonVideoService(conn: BackendJsonConnection)
  extends JsonBackendService(conn)
     with VideoService {

  def getVideo(videoId: VideoId): ServiceResult[Video] = {
    val params: Map[String, Any] = Map("Id" -> videoId.id)
    for {
      response <- Try( request("GetVideo", params) )
      root     <- Try( responseRoot(response, "VideoMetadataInfo") )
      result   <- Try( root.convertTo[Video] )
    } yield result
  }

  def getVideoByFileName(fileName: String): ServiceResult[Video] = {
    val params: Map[String, Any] = Map("FileName" -> fileName)
    for {
      response <- Try( request("GetVideoByFileName", params) )
      root     <- Try( responseRoot(response, "VideoMetadataInfo") )
      result   <- Try( root.convertTo[Video] )
    } yield result
  }

  def getVideoList(startIndex: Int, count: OptionalCount[Int], descending: Boolean): ServiceResult[PagedList[Video]] = {
    var params = buildStartCountParams(startIndex, count)
    if (descending) params += "Descending" -> descending
    for {
      response <- Try( request("GetVideoList", params) )
      root     <- Try( responseRoot(response, "VideoMetadataInfoList") )
      result   <- Try( root.convertTo[MythJsonPagedObjectList[Video]] )
    } yield result
  }

  def getBluray(path: String): ServiceResult[BlurayInfo] = {
    val params: Map[String, Any] = Map("Path" -> path)
    for {
      response <- Try( request("GetBluray", params) )
      root     <- Try( responseRoot(response, "BlurayInfo") )
      result   <- Try( root.convertTo[BlurayInfo] )
    } yield result
  }

  def lookupVideo(title: String, subtitle: String, inetRef: String, season: Int, episode: Int,
    grabberType: String, allowGeneric: Boolean): ServiceResult[List[VideoLookup]] = {
    var params: Map[String, Any] = Map.empty
    if (title.nonEmpty)    params += "Title" -> title
    if (subtitle.nonEmpty) params += "Subtitle" -> subtitle
    if (inetRef.nonEmpty)  params += "Inetref" -> inetRef
    if (season != 0)       params += "Season" -> season
    if (episode != 0)      params += "Episode" -> episode
    if (grabberType.nonEmpty) params += "GrabberType" -> grabberType
    if (allowGeneric)      params += "AllowGeneric" -> allowGeneric
    for {
      response <- Try( request("LookupVideo", params) )
      root     <- Try( responseRoot(response, "VideoLookupList") )
      result   <- Try( root.convertTo[MythJsonObjectList[VideoLookup]] )
    } yield result.data
  }

  /* mutating POST methods */

  def addVideo(fileName: String, hostName: String): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "FileName" -> fileName,
      "HostName" -> hostName
    )
    for {
      response <- Try( post("AddVideo", params) )
      root     <- Try(  responseRoot(response) )
      result   <- Try( root.booleanField("bool") )
    } yield result
  }

  def removeVideoFromDb(videoId: VideoId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("Id" -> videoId.id)
    for {
      response <- Try( post("RemoveVideoFromDB", params) )
      root     <- Try( responseRoot(response) )
      result   <- Try( root.booleanField("bool") )
    } yield result
  }

  /* Added to API on 6 Apr 2016 */
  def updateVideoWatchedStatus(videoId: VideoId, watched: Boolean): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("Id" -> videoId.id, "Watched" -> watched)
    for {
      response <- Try( post("UpdateVideoWatchedStatus", params) )
      root     <- Try( responseRoot(response) )
      result   <- Try( root.booleanField("bool") )  // TODO test
    } yield result
  }
}
