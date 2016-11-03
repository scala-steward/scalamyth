package mythtv
package services

import model.{ ArtworkInfo, ChanId, LiveStreamId, LiveStream, VideoId }
import util.{ MythDateTime, MythFileHash }

trait ContentService extends BackendService {
  def serviceName: String = "Content"

  def getFile(storageGroup: String, fileName: String): DataBytes
  def getFileList(storageGroup: String): List[String]
  def getHash(storageGroup: String, fileName: String): MythFileHash
  def getImageFile(storageGroup: String, fileName: String, width: Int, height: Int): DataBytes  // optional width, height

  def getLiveStream(id: LiveStreamId): LiveStream
  def getLiveStreamList(fileName: String): List[LiveStream]

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

  def addLiveStream(
    storageGroup: String,
    fileName: String,
    hostName: String = "",
    maxSegments: Int = LiveStream.DefaultMaxSegments,
    width: Int = LiveStream.DefaultWidth,
    height: Int = LiveStream.DefaultHeight,
    bitrate: Int = LiveStream.DefaultBitrate,
    audioBitrate: Int = LiveStream.DefaultAudioBitrate,
    sampleRate: Int = LiveStream.DefaultSampleRate
  ): LiveStream

  def addRecordingLiveStream(
    chanId: ChanId,
    startTime: MythDateTime,
    maxSegments: Int = LiveStream.DefaultMaxSegments,
    width: Int = LiveStream.DefaultWidth,
    height: Int = LiveStream.DefaultHeight,
    bitrate: Int = LiveStream.DefaultBitrate,
    audioBitrate: Int = LiveStream.DefaultAudioBitrate,
    sampleRate: Int = LiveStream.DefaultSampleRate
  ): LiveStream

  def addVideoLiveStream(
    videoId: VideoId,
    maxSegments: Int = LiveStream.DefaultMaxSegments,
    width: Int = LiveStream.DefaultWidth,
    height: Int = LiveStream.DefaultHeight,
    bitrate: Int = LiveStream.DefaultBitrate,
    audioBitrate: Int = LiveStream.DefaultAudioBitrate,
    sampleRate: Int = LiveStream.DefaultSampleRate
  ): LiveStream

  def stopLiveStream(id: LiveStreamId): LiveStream

  def removeLiveStream(id: LiveStreamId): Boolean
}
