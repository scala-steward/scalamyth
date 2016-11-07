package mythtv
package connection
package http
package json

import spray.json.DefaultJsonProtocol

import services.ContentService
import model.{ ArtworkInfo, ChanId, LiveStreamId, LiveStream, VideoId }
import util.{ MythFileHash, MythDateTime }
import RichJsonObject._

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

  def getLiveStream(id: LiveStreamId): LiveStream = {
    val params: Map[String, Any] = Map("Id" -> id.id)
    val response = request("GetLiveStream", params)
    val root = responseRoot(response, "LiveStreamInfo")
    root.convertTo[LiveStream]
  }

  def getLiveStreamList(fileName: String): List[LiveStream] = {
    var params: Map[String, Any] = Map.empty
    if (fileName.nonEmpty) params += "FileName" -> fileName
    val response = request("GetLiveStreamList", params)
    val root = responseRoot(response, "LiveStreamInfoList")
    root.convertTo[List[LiveStream]]
  }

  def getFile(storageGroup: String, fileName: String): HttpStreamResponse = {
    val params: Map[String, Any] = Map(
      "StorageGroup" -> storageGroup,
      "FileName"     -> fileName
    )
    requestStream("GetFile", params)
  }

  def getImageFile(storageGroup: String, fileName: String, width: Int, height: Int): HttpStreamResponse = {
    var params: Map[String, Any] = Map(
      "StorageGroup" -> storageGroup,
      "FileName"     -> fileName
    )
    if (width != 0)  params += "Width" -> width
    if (height != 0) params += "Height" -> height
    requestStream("GetImageFile", params)
  }

  def getMusic(id: Int): HttpStreamResponse = {
    val params: Map[String, Any] = Map("Id" -> id)
    requestStream("GetMusic", params)
  }

  def getRecording(chanId: ChanId, startTime: MythDateTime): HttpStreamResponse = {
    val params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    requestStream("GetRecording", params)
  }

  def getVideo(videoId: VideoId): HttpStreamResponse = {
    val params: Map[String, Any] = Map("Id" -> videoId.id)
    requestStream("GetVideo", params)
  }

  def getVideoArtwork(artType: String, videoId: VideoId, width: Int, height: Int): HttpStreamResponse = {
    var params: Map[String, Any] = Map("Type" -> artType, "Id" -> videoId.id)
    if (width != 0)  params += "Width" -> width
    if (height != 0) params += "Height" -> height
    requestStream("GetVideoArtwork", params)
  }

  def getAlbumArt(id: Int, width: Int, height: Int): HttpStreamResponse = {
    var params: Map[String, Any] = Map("Id" -> id)
    if (width != 0)  params += "Width" -> width
    if (height != 0) params += "Height" -> height
    requestStream("GetAlbumArt", params)
  }

  def getPreviewImage(chanId: ChanId, startTime: MythDateTime, width: Int, height: Int, secsIn: Int): HttpStreamResponse = {
    var params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    if (width != 0)  params += "Width" -> width
    if (height != 0) params += "Height" -> height
    if (secsIn > 0)  params += "SecsIn" -> secsIn
    requestStream("GetPreviewImage", params)
  }

  def getRecordingArtwork(artType: String, inetRef: String, season: Int, width: Int, height: Int): HttpStreamResponse = {
    var params: Map[String, Any] = Map("Type" -> artType, "Inetref" -> inetRef, "Season" -> season)
    if (width != 0)  params += "Width" -> width
    if (height != 0) params += "Height" -> height
    requestStream("GetRecordingArtwork", params)
  }

  def getRecordingArtworkList(chanId: ChanId, startTime: MythDateTime): List[ArtworkInfo] = {
    val params: Map[String, Any] = Map(
      "ChanId" -> chanId.id,
      "StartTime" -> startTime.toIsoFormat
    )
    val response = request("GetRecordingArtworkList", params)
    val root = responseRoot(response, "ArtworkInfoList")
    root.convertTo[List[ArtworkInfo]]
  }

  def getProgramArtworkList(inetRef: String, season: Int): List[ArtworkInfo] = {
    val params: Map[String, Any] = Map(
      "Inetref" -> inetRef,
      "Season" -> season
    )
    val response = request("GetProgramArtworkList", params)
    val root = responseRoot(response, "ArtworkInfoList")
    root.convertTo[List[ArtworkInfo]]
  }

  def downloadFile(url: String, storageGroup: String): Boolean = {
    val params: Map[String, Any] = Map(
      "URL" -> url,
      "StorageGroup" -> storageGroup
    )
    val response = post("DownloadFile", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  def addLiveStream(storageGroup: String, fileName: String, hostName: String, maxSegments: Int,
    width: Int, height: Int, bitrate: Int, audioBitrate: Int, sampleRate: Int): LiveStream = {
    var params: Map[String, Any] = Map(
      "StorageGroup" -> storageGroup,
      "FileName"     -> fileName
    )
    if (hostName.nonEmpty) params += "HostName"     -> hostName
    if (maxSegments != 0)  params += "MaxSegments"  -> maxSegments
    if (width != 0)        params += "Width"        -> width
    if (height != 0)       params += "Height"       -> height
    if (bitrate != 0)      params += "Bitrate"      -> bitrate
    if (audioBitrate != 0) params += "AudioBitrate" -> audioBitrate
    if (sampleRate != 0)   params += "SampleRate"   -> sampleRate
    val response = post("AddLiveStream", params)
    val root = responseRoot(response, "LiveStreamInfo")
    root.convertTo[LiveStream]
  }

  def addRecordingLiveStream(chanId: ChanId, startTime: MythDateTime, maxSegments: Int,
    width: Int, height: Int, bitrate: Int, audioBitrate: Int, sampleRate: Int): LiveStream = {
    var params: Map[String, Any] = Map(
      "ChanId"       -> chanId.id,
      "StartTime"    -> startTime.toIsoFormat
    )
    if (maxSegments != 0)  params += "MaxSegments"  -> maxSegments
    if (width != 0)        params += "Width"        -> width
    if (height != 0)       params += "Height"       -> height
    if (bitrate != 0)      params += "Bitrate"      -> bitrate
    if (audioBitrate != 0) params += "AudioBitrate" -> audioBitrate
    if (sampleRate != 0)   params += "SampleRate"   -> sampleRate
    val response = post("AddRecordingLiveStream", params)
    val root = responseRoot(response, "LiveStreamInfo")
    root.convertTo[LiveStream]
  }

  def addVideoLiveStream(videoId: VideoId, maxSegments: Int,
    width: Int, height: Int, bitrate: Int, audioBitrate: Int, sampleRate: Int): LiveStream = {
    var params: Map[String, Any] = Map("Id" -> videoId.id)
    if (maxSegments != 0)  params += "MaxSegments"  -> maxSegments
    if (width != 0)        params += "Width"        -> width
    if (height != 0)       params += "Height"       -> height
    if (bitrate != 0)      params += "Bitrate"      -> bitrate
    if (audioBitrate != 0) params += "AudioBitrate" -> audioBitrate
    if (sampleRate != 0)   params += "SampleRate"   -> sampleRate
    val response = post("AddVideoLiveStream", params)
    val root = responseRoot(response, "LiveStreamInfo")
    root.convertTo[LiveStream]
  }

  def stopLiveStream(id: LiveStreamId): LiveStream = {
    val params: Map[String, Any] = Map("Id" -> id.id)
    val response = post("StopLiveStream", params)
    val root = responseRoot(response, "LiveStreamInfo")
    root.convertTo[LiveStream]
  }

  def removeLiveStream(id: LiveStreamId): Boolean = {
    val params: Map[String, Any] = Map("Id" -> id.id)
    val response = post("RemoveLiveStream", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }
}
