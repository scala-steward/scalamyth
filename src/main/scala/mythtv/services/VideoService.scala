package mythtv
package services

import model.{ VideoId, Video }
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
}
