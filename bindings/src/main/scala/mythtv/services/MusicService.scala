package mythtv
package services

import model.{ Song, SongId }
import util.OptionalCount

trait MusicService extends BackendService {
  final def serviceName: String = "Music"

  def getTrack(songId: SongId): ServiceResult[Song]

  def getTrackList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all
  ): ServiceResult[PagedList[Song]]
}
