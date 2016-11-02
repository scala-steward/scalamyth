package mythtv
package services

import model.{ ChanId, LiveStreamInfo, VideoId }
import util.{ MythDateTime, MythFileHash }

trait ArtworkInfo // TODO temporary placeholder

trait ContentService extends BackendService {
  def serviceName: String = "Content"

  def getFile(storageGroup: String, fileName: String): DataBytes
  def getFileList(storageGroup: String): List[String]
  def getHash(storageGroup: String, fileName: String): MythFileHash
  def getImageFile(storageGroup: String, fileName: String, width: Int, height: Int): DataBytes  // optional width, height
  def getLiveStream(id: Int): LiveStreamInfo
  def getLiveStreamList(fileName: String): List[LiveStreamInfo]
  def getMusic(id: Int): DataBytes
  def getRecording(chanId: ChanId, startTime: MythDateTime): DataBytes
  def getVideo(id: VideoId): DataBytes

  // TODO more methods

  def getRecordingArtwork(artType: String, inetRef: String, season: Int, width: Int, height: Int): DataBytes

  def getRecordingArtworkList(chanId: ChanId, startTime: MythDateTime): List[ArtworkInfo]

  def getProgramArtworkList(inetRef: String, season: Int): List[ArtworkInfo]

  def getVideoArtwork(artType: String, videoId: Int, width: Int, height: Int): DataBytes

  def getAlbumArt(id: Int, width: Int, height: Int): DataBytes

  def getPreviewImage(chanId: ChanId, startTime: MythDateTime, width: Int, height: Int, secsIn: Int): DataBytes


  def downloadFile(url: String, storageGroup: String): Boolean

  def addLiveStream(storageGroup: String, fileName: String, hostName: String, maxSegments: Int,
    width: Int, height: Int, bitrate: Int, audioBitrate: Int, sampleRate: Int): LiveStreamInfo

  def addRecordingLiveStream(chanId: ChanId, startTime: MythDateTime, maxSegments: Int,
    width: Int, height: Int, bitrate: Int, audioBitrate: Int, sampleRate: Int): LiveStreamInfo

  def addVideoLiveStream(videoId: VideoId, maxSegments: Int,
    width: Int, height: Int, bitrate: Int, audioBitrate: Int, sampleRate: Int): LiveStreamInfo

  def stopLiveStream(id: Int): LiveStreamInfo

  def removeLiveStream(id: Int): Boolean
}
