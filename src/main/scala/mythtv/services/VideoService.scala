package mythtv
package services

import model.{ BlurayInfo, VideoId, Video, VideoLookup }
import util.OptionalCount

trait VideoService extends BackendService {
  def serviceName: String = "Video"

  def getVideo(id: VideoId): Video
  def getVideoByFileName(fileName: String): Video
  def getVideoList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all,
    descending: Boolean = false
  ): PagedList[Video]

  def getBluray(path: String): BlurayInfo

  /*
     TODO UPSTREAM LookupVideo seems to fail or return unexpected results often on my 0.27 setup,
     epescially for movies. Try looking for "Juno" or "Gravity" and see if we get any results.
     Is there a problem with the TMDB grabber or processing its output? A query for Title=Gravity
     with the grabber directly fails with KeyError=25, while Juno returns results with the grabber
     but they are not reflected in the results of this service call.
   */

  // perform metadata lookup for a video
  // TODO enum or something for grabber type?
  def lookupVideo(
    title: String,
    subtitle: String,
    inetRef: String,
    season: Int = 0,
    episode: Int = 0,
    grabberType: String = "",
    allowGeneric: Boolean = false
  ): List[VideoLookup]

  def lookupVideoTitle(
    title: String,
    subtitle: String = "",
    season: Int = 0,
    episode: Int = 0,
    inetRef: String = "",
    grabberType: String = "",
    allowGeneric: Boolean = false
  ): List[VideoLookup] =
    lookupVideo(title, subtitle, inetRef, season, episode, grabberType, allowGeneric)

  def lookupVideoInetref(
    inetRef: String,
    season: Int = 0,
    episode: Int = 0,
    title: String = "",
    subtitle: String = "",
    grabberType: String = "",
    allowGeneric: Boolean = false
  ): List[VideoLookup] =
    lookupVideo(title, subtitle, inetRef, season, episode, grabberType, allowGeneric)

  /* mutating POST methods */

  def addVideo(fileName: String, hostName: String): Boolean

  def removeVideoFromDb(videoId: VideoId): Boolean

  /* Added to API on 6 Apr 2016 */
  def updateVideoWatchedStatus(videoId: VideoId, watched: Boolean): Boolean
}
