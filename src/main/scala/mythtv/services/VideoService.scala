package mythtv
package services

import model.{ BlurayInfo, VideoId, Video }
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

  /* mutating POST methods */

  def addVideo(fileName: String, hostName: String): Boolean

  def removeVideoFromDb(videoId: VideoId): Boolean

  /* Added to API on 6 Apr 2016 */
  def updateVideoWatchedStatus(videoId: VideoId, watched: Boolean): Boolean
}
