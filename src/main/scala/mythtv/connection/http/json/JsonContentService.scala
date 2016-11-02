package mythtv
package connection
package http
package json

import spray.json.DefaultJsonProtocol

import model.{ ChanId, LiveStreamInfo, VideoId }
import services.{ ContentService, ArtworkInfo }
import util.{ MythFileHash, MythDateTime }

import services.DataBytes // FIXME temporary placeholder

class JsonContentService(conn: BackendJsonConnection)
  extends JsonBackendService(conn)
     with ContentService {
  def getFileList(storageGroup: String): List[String] = {
    val params: Map[String, Any] = Map("StorageGroup" -> storageGroup)
    val response = request("GetFileList", params)
    val root = responseRoot(response)
    root.convertTo[List[String]]
  }

  // TODO handle error conditions, such as file not existing...
  def getHash(storageGroup: String, fileName: String): MythFileHash = {
    import DefaultJsonProtocol.StringJsonFormat
    val params: Map[String, Any] = Map("StorageGroup" -> storageGroup, "FileName" -> fileName)
    val response = request("GetHash", params)
    val root = responseRoot(response, "String")
    new MythFileHash(root.convertTo[String])
  }

  def getFile(storageGroup: String, fileName: String): DataBytes = ???
  def getImageFile(storageGroup: String, fileName: String, width: Int, height: Int): DataBytes = ???
  def getLiveStream(id: Int): LiveStreamInfo = ???
  def getLiveStreamList(fileName: String): List[LiveStreamInfo] = ???
  def getMusic(id: Int): DataBytes = ???
  def getRecording(chanId: ChanId, startTime: MythDateTime): DataBytes = ???
  def getVideo(id: VideoId): DataBytes = ???

  // TODO more methods

  def getRecordingArtwork(artType: String, inetRef: String, season: Int, width: Int, height: Int): DataBytes = ???

  def getRecordingArtworkList(chanId: ChanId, startTime: MythDateTime): List[ArtworkInfo] = ???

  def getProgramArtworkList(inetRef: String, season: Int): List[ArtworkInfo] = ???

  def getVideoArtwork(artType: String, videoId: Int, width: Int, height: Int): DataBytes = ???

  def getAlbumArt(id: Int, width: Int, height: Int): DataBytes = ???

  def getPreviewImage(chanId: ChanId, startTime: MythDateTime, width: Int, height: Int, secsIn: Int): DataBytes = ???


  def downloadFile(url: String, storageGroup: String): Boolean = ???

  def addLiveStream(storageGroup: String, fileName: String, hostName: String, maxSegments: Int,
    width: Int, height: Int, bitrate: Int, audioBitrate: Int, sampleRate: Int): LiveStreamInfo = ???

  def addRecordingLiveStream(chanId: ChanId, startTime: MythDateTime, maxSegments: Int,
    width: Int, height: Int, bitrate: Int, audioBitrate: Int, sampleRate: Int): LiveStreamInfo = ???

  def addVideoLiveStream(videoId: VideoId, maxSegments: Int,
    width: Int, height: Int, bitrate: Int, audioBitrate: Int, sampleRate: Int): LiveStreamInfo = ???

  def stopLiveStream(id: Int): LiveStreamInfo = ???

  def removeLiveStream(id: Int): Boolean = ???
}
