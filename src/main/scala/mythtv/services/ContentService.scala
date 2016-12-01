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

  def getMusic[U](id: Int)(f: (HttpStreamResponse) => U): ServiceResult[Unit]

  def getRecording[U](chanId: ChanId, startTime: MythDateTime)(f: (HttpStreamResponse) => U): ServiceResult[Unit]

  def getVideo[U](id: VideoId)(f: (HttpStreamResponse) => U): ServiceResult[Unit]

  def getFile[U](storageGroup: String, fileName: String)(f: (HttpStreamResponse) => U): ServiceResult[Unit]

  def getImageFile[U](storageGroup: String, fileName: String, width: Int = 0, height: Int = 0)(f: (HttpStreamResponse) => U): ServiceResult[Unit]

  def getRecordingArtwork[U](artType: String, inetRef: String, season: Int, width: Int = 0, height: Int = 0)(f: (HttpStreamResponse) => U): ServiceResult[Unit]

  def getVideoArtwork[U](artType: String, videoId: VideoId, width: Int = 0, height: Int = 0)(f: (HttpStreamResponse) => U): ServiceResult[Unit]

  def getAlbumArt[U](id: Int, width: Int = 0, height: Int = 0)(f: (HttpStreamResponse) => U): ServiceResult[Unit]

  def getPreviewImage[U](chanId: ChanId, startTime: MythDateTime, width: Int = 0, height: Int = 0, secsIn: Int = -1)(f: (HttpStreamResponse) => U): ServiceResult[Unit]

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
