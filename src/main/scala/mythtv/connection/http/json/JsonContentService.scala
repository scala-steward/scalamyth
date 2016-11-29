package mythtv
package connection
package http
package json

import scala.util.Try

import spray.json.DefaultJsonProtocol

import services.{ ContentService, ServiceResult }
import model.{ ArtworkInfo, ChanId, LiveStreamId, LiveStream, VideoId }
import util.{ MythFileHash, MythDateTime }
import RichJsonObject._

class JsonContentService(conn: BackendJsonConnection)
  extends JsonBackendService(conn)
     with ContentService {

  def getFileList(storageGroup: String): ServiceResult[List[String]] = {
    val params: Map[String, Any] = Map("StorageGroup" -> storageGroup)
    for {
      response <- request("GetFileList", params)
      root     <- responseRoot(response)
      result   <- Try(root.convertTo[List[String]])
    } yield result
  }

  def getHash(storageGroup: String, fileName: String): ServiceResult[MythFileHash] = {
    import DefaultJsonProtocol.StringJsonFormat
    val params: Map[String, Any] = Map("StorageGroup" -> storageGroup, "FileName" -> fileName)
    for {
      response <- request("GetHash", params)
      root     <- responseRoot(response, "String")
      result   <- Try {
        // Services API translates hash of "NULL" to empty string, so we translate it back
        val hash = root.convertTo[String]
        new MythFileHash(if (hash.nonEmpty) hash else "NULL")
      }
    } yield result
  }

  def getLiveStream(id: LiveStreamId): ServiceResult[LiveStream] = {
    val params: Map[String, Any] = Map("Id" -> id.id)
    for {
      response <- request("GetLiveStream", params)
      root     <- responseRoot(response, "LiveStreamInfo")
      result   <- Try(root.convertTo[LiveStream])
    } yield result
  }

  def getLiveStreamList(fileName: String): ServiceResult[List[LiveStream]] = {
    var params: Map[String, Any] = Map.empty
    if (fileName.nonEmpty) params += "FileName" -> fileName
    for {
      response <- request("GetLiveStreamList", params)
      root     <- responseRoot(response, "LiveStreamInfoList")
      result   <- Try(root.convertTo[List[LiveStream]])
    } yield result
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

  def getRecordingArtworkList(chanId: ChanId, startTime: MythDateTime): ServiceResult[List[ArtworkInfo]] = {
    val params: Map[String, Any] = Map(
      "ChanId" -> chanId.id,
      "StartTime" -> startTime.toIsoFormat
    )
    for {
      response <- request("GetRecordingArtworkList", params)
      root     <- responseRoot(response, "ArtworkInfoList")
      result   <- Try(root.convertTo[List[ArtworkInfo]])
    } yield result
  }

  def getProgramArtworkList(inetRef: String, season: Int): ServiceResult[List[ArtworkInfo]] = {
    val params: Map[String, Any] = Map(
      "Inetref" -> inetRef,
      "Season" -> season
    )
    for {
      response <- request("GetProgramArtworkList", params)
      root     <- responseRoot(response, "ArtworkInfoList")
      result   <- Try(root.convertTo[List[ArtworkInfo]])
    } yield result
  }

  def downloadFile(url: String, storageGroup: String): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "URL" -> url,
      "StorageGroup" -> storageGroup
    )
    for {
      response <- post("DownloadFile", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def addLiveStream(storageGroup: String, fileName: String, hostName: String, maxSegments: Int,
    width: Int, height: Int, bitrate: Int, audioBitrate: Int, sampleRate: Int): ServiceResult[LiveStream] = {
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
    for {
      response <- post("AddLiveStream", params)
      root     <- responseRoot(response, "LiveStreamInfo")
      result   <- Try(root.convertTo[LiveStream])
    } yield result
  }

  def addRecordingLiveStream(chanId: ChanId, startTime: MythDateTime, maxSegments: Int,
    width: Int, height: Int, bitrate: Int, audioBitrate: Int, sampleRate: Int): ServiceResult[LiveStream] = {
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
    for {
      response <- post("AddRecordingLiveStream", params)
      root     <- responseRoot(response, "LiveStreamInfo")
      result   <- Try(root.convertTo[LiveStream])
    } yield result
  }

  def addVideoLiveStream(videoId: VideoId, maxSegments: Int,
    width: Int, height: Int, bitrate: Int, audioBitrate: Int, sampleRate: Int): ServiceResult[LiveStream] = {
    var params: Map[String, Any] = Map("Id" -> videoId.id)
    if (maxSegments != 0)  params += "MaxSegments"  -> maxSegments
    if (width != 0)        params += "Width"        -> width
    if (height != 0)       params += "Height"       -> height
    if (bitrate != 0)      params += "Bitrate"      -> bitrate
    if (audioBitrate != 0) params += "AudioBitrate" -> audioBitrate
    if (sampleRate != 0)   params += "SampleRate"   -> sampleRate
    for {
      response <- post("AddVideoLiveStream", params)
      root     <- responseRoot(response, "LiveStreamInfo")
      result   <- Try(root.convertTo[LiveStream])
    } yield result
  }

  def stopLiveStream(id: LiveStreamId): ServiceResult[LiveStream] = {
    val params: Map[String, Any] = Map("Id" -> id.id)
    for {
      response <- post("StopLiveStream", params)
      root     <- responseRoot(response, "LiveStreamInfo")
      result   <- Try(root.convertTo[LiveStream])
    } yield result
  }

  def removeLiveStream(id: LiveStreamId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("Id" -> id.id)
    for {
      response <- post("RemoveLiveStream", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }
}
