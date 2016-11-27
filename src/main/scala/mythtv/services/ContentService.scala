package mythtv
package services

import model.{ ArtworkInfo, ChanId, LiveStreamId, LiveStream, VideoId }
import util.{ MythDateTime, MythFileHash }
import connection.http.HttpStreamResponse

trait ContentService extends BackendService {
  def serviceName: String = "Content"

  def getFileList(storageGroup: String): ServiceResult[List[String]]
  def downloadFile(url: String, storageGroup: String): ServiceResult[Boolean]
  def getHash(storageGroup: String, fileName: String): ServiceResult[MythFileHash]

  def getRecordingArtworkList(chanId: ChanId, startTime: MythDateTime): ServiceResult[List[ArtworkInfo]]
  def getProgramArtworkList(inetRef: String, season: Int): ServiceResult[List[ArtworkInfo]]

  def getMusic(id: Int): HttpStreamResponse

  def getRecording(chanId: ChanId, startTime: MythDateTime): HttpStreamResponse

  def getVideo(id: VideoId): HttpStreamResponse

  def getFile(storageGroup: String, fileName: String): HttpStreamResponse

  def getImageFile(storageGroup: String, fileName: String, width: Int = 0, height: Int = 0): HttpStreamResponse

  def getRecordingArtwork(artType: String, inetRef: String, season: Int, width: Int = 0, height: Int = 0): HttpStreamResponse

  def getVideoArtwork(artType: String, videoId: VideoId, width: Int = 0, height: Int = 0): HttpStreamResponse

  def getAlbumArt(id: Int, width: Int = 0, height: Int = 0): HttpStreamResponse

  def getPreviewImage(chanId: ChanId, startTime: MythDateTime, width: Int = 0, height: Int = 0, secsIn: Int = -1): HttpStreamResponse

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
  ): ServiceResult[LiveStream]

  def addRecordingLiveStream(
    chanId: ChanId,
    startTime: MythDateTime,
    maxSegments: Int = LiveStream.DefaultMaxSegments,
    width: Int = LiveStream.DefaultWidth,
    height: Int = LiveStream.DefaultHeight,
    bitrate: Int = LiveStream.DefaultBitrate,
    audioBitrate: Int = LiveStream.DefaultAudioBitrate,
    sampleRate: Int = LiveStream.DefaultSampleRate
  ): ServiceResult[LiveStream]

  def addVideoLiveStream(
    videoId: VideoId,
    maxSegments: Int = LiveStream.DefaultMaxSegments,
    width: Int = LiveStream.DefaultWidth,
    height: Int = LiveStream.DefaultHeight,
    bitrate: Int = LiveStream.DefaultBitrate,
    audioBitrate: Int = LiveStream.DefaultAudioBitrate,
    sampleRate: Int = LiveStream.DefaultSampleRate
  ): ServiceResult[LiveStream]

  def getLiveStream(id: LiveStreamId): ServiceResult[LiveStream]

  def getLiveStreamList(fileName: String = ""): ServiceResult[List[LiveStream]]

  def stopLiveStream(id: LiveStreamId): ServiceResult[LiveStream]

  def removeLiveStream(id: LiveStreamId): ServiceResult[Boolean]
}
