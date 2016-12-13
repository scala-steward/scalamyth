package mythtv
package connection
package http
package json

import scala.util.Try

import util.OptionalCount
import model.EnumTypes.MetadataGrabberType
import model.{ BlurayInfo, MetadataGrabberType, Video, VideoId, VideoLookup }
import services.{ PagedList, ServiceResult, ServicesObject, VideoService }
import services.Service.ServiceFailure.ServiceNoResult
import RichJsonObject._

class JsonVideoService(conn: BackendJsonConnection)
  extends JsonBackendService(conn)
     with VideoService {

  def getVideo(videoId: VideoId): ServiceResult[Video] = {
    val params: Map[String, Any] = Map("Id" -> videoId.id)
    val vidTry = for {
      response <- request("GetVideo", params)
      root     <- responseRoot(response, "VideoMetadataInfo")
      result   <- Try(root.convertTo[Video])
    } yield result
    if (vidTry.isSuccess && vidTry.get.id.id == 0) Left(ServiceNoResult)
    else vidTry
  }

  def getVideoByFileName(fileName: String): ServiceResult[Video] = {
    val params: Map[String, Any] = Map("FileName" -> fileName)
    for {
      response <- request("GetVideoByFileName", params)
      root     <- responseRoot(response, "VideoMetadataInfo")
      result   <- Try(root.convertTo[Video])
    } yield result
  }

  def getVideoList(
    folder: String,
    sortBy: String,
    startIndex: Int,
    count: OptionalCount[Int],
    descending: Boolean
  ): ServiceResult[PagedList[Video]] = {
    var params = buildStartCountParams(startIndex, count)
    if (folder.nonEmpty) params += "Folder"     -> folder
    if (sortBy.nonEmpty) params += "Sort"       -> sortBy
    if (descending)      params += "Descending" -> descending
    for {
      response <- request("GetVideoList", params)
      root     <- responseRoot(response, "VideoMetadataInfoList")
      result   <- Try(root.convertTo[ServicesPagedList[Video]])
    } yield result
  }

  def getBluray(path: String): ServiceResult[BlurayInfo] = {
    val params: Map[String, Any] = Map("Path" -> path)
    val bdTry = for {
      response <- request("GetBluray", params)
      root     <- responseRoot(response, "BlurayInfo")
      result   <- Try(root.convertTo[BlurayInfo])
    } yield result
    if (bdTry.isSuccess && bdTry.get.title.isEmpty) Left(ServiceNoResult)
    else bdTry
  }

  def lookupVideo(title: String, subtitle: String, inetRef: String, season: Int, episode: Int,
    grabberType: MetadataGrabberType, allowGeneric: Boolean): ServiceResult[List[VideoLookup]] = {
    var params: Map[String, Any] = Map.empty
    if (title.nonEmpty)    params += "Title"    -> title
    if (subtitle.nonEmpty) params += "Subtitle" -> subtitle
    if (inetRef.nonEmpty)  params += "Inetref"  -> inetRef
    if (season != 0)       params += "Season"   -> season
    if (episode != 0)      params += "Episode"  -> episode
    if (grabberType != MetadataGrabberType.Unknown) params += "GrabberType" -> grabberType.toString
    if (allowGeneric)      params += "AllowGeneric" -> allowGeneric
    for {
      response <- request("LookupVideo", params)
      root     <- responseRoot(response, "VideoLookupList")
      result   <- Try(root.convertTo[ServicesObject[List[VideoLookup]]])
    } yield result.data
  }

  /* mutating POST methods */

  def addVideo(fileName: String, hostName: String): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "FileName" -> fileName,
      "HostName" -> hostName
    )
    for {
      response <- post("AddVideo", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def removeVideoFromDb(videoId: VideoId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("Id" -> videoId.id)
    for {
      response <- post("RemoveVideoFromDB", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  /* Added to API on 6 Apr 2016 */
  def updateVideoWatchedStatus(videoId: VideoId, watched: Boolean): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("Id" -> videoId.id, "Watched" -> watched)
    for {
      response <- post("UpdateVideoWatchedStatus", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }
}
