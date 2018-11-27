package mythtv
package connection
package http

import model.{ Song, SongId }
import services.{ MusicService, PagedList, ServiceResult }
import util.OptionalCount

trait AbstractMusicService extends ServiceProtocol with MusicService {

  def getTrack(songId: SongId): ServiceResult[Song] = {
    val params: Map[String, Any] = Map("Id" -> songId.id)
    request("GetTrack", params)("MusicMetadataInfo")
  }

  def getTrackList(startIndex: Int, count: OptionalCount[Int]): ServiceResult[PagedList[Song]] = {
    val params = buildStartCountParams(startIndex, count)
    request("GetTrackList", params)("MusicMetadataInfoList")
  }
}
