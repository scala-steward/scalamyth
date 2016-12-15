package mythtv
package connection
package http

import model.EnumTypes.MetadataGrabberType
import model.{ BlurayInfo, MetadataGrabberType, Video, VideoId, VideoLookup }
import services.{ PagedList, ServiceResult, VideoService }
import services.Service.ServiceFailure.ServiceNoResult
import util.OptionalCount

trait AbstractVideoService extends ServiceProtocol with VideoService {

  def getVideo(videoId: VideoId): ServiceResult[Video] = {
    val params: Map[String, Any] = Map("Id" -> videoId.id)
    val vidTry = request[Video]("GetVideo", params)("VideoMetadataInfo")
    if (vidTry.isSuccess && vidTry.get.id.id == 0) Left(ServiceNoResult)
    else vidTry
  }

  def getVideoByFileName(fileName: String): ServiceResult[Video] = {
    val params: Map[String, Any] = Map("FileName" -> fileName)
    request("GetVideoByFileName", params)("VideoMetadataInfo")
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
    request("GetVideoList", params)("VideoMetadataInfoList")
  }

  def getBluray(path: String): ServiceResult[BlurayInfo] = {
    val params: Map[String, Any] = Map("Path" -> path)
    val bdTry = request[BlurayInfo]("GetBluray", params)("BlurayInfo")
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
    request("LookupVideo", params)("VideoLookupList")
  }

  /* mutating POST methods */

  def addVideo(fileName: String, hostName: String): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "FileName" -> fileName,
      "HostName" -> hostName
    )
    post("AddVideo", params)()
  }

  def removeVideoFromDb(videoId: VideoId): ServiceResult[Boolean] = {
    post("RemoveVideoFromDB", Map("Id" -> videoId.id))()
  }

  /* Added to API on 6 Apr 2016 */
  def updateVideoWatchedStatus(videoId: VideoId, watched: Boolean): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("Id" -> videoId.id, "Watched" -> watched)
    post("UpdateVideoWatchedStatus", params)()
  }
}
