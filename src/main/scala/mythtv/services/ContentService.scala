package mythtv
package services

import model.{ ArtworkInfo, ChanId, LiveStreamId, LiveStream, VideoId }
import util.{ MythDateTime, MythFileHash }

trait ContentService extends BackendService {
  def serviceName: String = "Content"

  def getFileList(storageGroup: String): List[String]
  def downloadFile(url: String, storageGroup: String): Boolean
  def getHash(storageGroup: String, fileName: String): MythFileHash

  def getRecordingArtworkList(chanId: ChanId, startTime: MythDateTime): List[ArtworkInfo]
  def getProgramArtworkList(inetRef: String, season: Int): List[ArtworkInfo]

  def getMusic(id: Int): DataBytes
  def getRecording(chanId: ChanId, startTime: MythDateTime): DataBytes
  def getVideo(id: VideoId): DataBytes

  def getFile(storageGroup: String, fileName: String): DataBytes
  def getImageFile(storageGroup: String, fileName: String, width: Int, height: Int): DataBytes  // optional width, height

  def getRecordingArtwork(artType: String, inetRef: String, season: Int, width: Int, height: Int): DataBytes

  def getVideoArtwork(artType: String, videoId: Int, width: Int, height: Int): DataBytes

  def getAlbumArt(id: Int, width: Int, height: Int): DataBytes

  def getPreviewImage(chanId: ChanId, startTime: MythDateTime, width: Int, height: Int, secsIn: Int): DataBytes

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

  def getLiveStream(id: LiveStreamId): LiveStream

  def getLiveStreamList(fileName: String = ""): List[LiveStream]

  def stopLiveStream(id: LiveStreamId): LiveStream

  def removeLiveStream(id: LiveStreamId): Boolean
}
