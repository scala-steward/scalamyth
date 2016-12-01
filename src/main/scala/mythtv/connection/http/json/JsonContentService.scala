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

  def getFile[U](storageGroup: String, fileName: String)(f: (HttpStreamResponse) => U): ServiceResult[Unit] = {
    val params: Map[String, Any] = Map(
      "StorageGroup" -> storageGroup,
      "FileName"     -> fileName
    )
    Try {
      val response = requestStream("GetFile", params)
      streamResponse(response, f)
    }
  }

  def getImageFile[U](storageGroup: String, fileName: String, width: Int, height: Int)(f: (HttpStreamResponse) => U): ServiceResult[Unit] = {
    var params: Map[String, Any] = Map(
      "StorageGroup" -> storageGroup,
      "FileName"     -> fileName
    )
    if (width != 0)  params += "Width" -> width
    if (height != 0) params += "Height" -> height
    Try {
      val response = requestStream("GetImageFile", params)
      streamResponse(response, f)
    }
  }

  def getMusic[U](id: Int)(f: (HttpStreamResponse) => U): ServiceResult[Unit] = {
    val params: Map[String, Any] = Map("Id" -> id)
    Try {
      val response = requestStream("GetMusic", params)
      streamResponse(response, f)
    }
  }

  def getRecording[U](chanId: ChanId, startTime: MythDateTime)(f: (HttpStreamResponse) => U): ServiceResult[Unit] = {
    val params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    Try {
      val response = requestStream("GetRecording", params)
      streamResponse(response, f)
    }
  }

  def getVideo[U](videoId: VideoId)(f: (HttpStreamResponse) => U): ServiceResult[Unit] = {
    val params: Map[String, Any] = Map("Id" -> videoId.id)
    Try {
      val response = requestStream("GetVideo", params)
      streamResponse(response, f)
    }
  }

  def getVideoArtwork[U](artType: String, videoId: VideoId, width: Int, height: Int)(f: (HttpStreamResponse) => U): ServiceResult[Unit] = {
    var params: Map[String, Any] = Map("Type" -> artType, "Id" -> videoId.id)
    if (width != 0)  params += "Width" -> width
    if (height != 0) params += "Height" -> height
    Try {
      val response = requestStream("GetVideoArtwork", params)
      streamResponse(response, f)
    }
  }

  def getAlbumArt[U](id: Int, width: Int, height: Int)(f: (HttpStreamResponse) => U): ServiceResult[Unit] = {
    var params: Map[String, Any] = Map("Id" -> id)
    if (width != 0)  params += "Width" -> width
    if (height != 0) params += "Height" -> height
    Try {
      val response = requestStream("GetAlbumArt", params)
      streamResponse(response, f)
    }
  }

  def getPreviewImage[U](chanId: ChanId, startTime: MythDateTime, width: Int, height: Int, secsIn: Int)(f: (HttpStreamResponse) => U): ServiceResult[Unit] = {
    var params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    if (width != 0)  params += "Width" -> width
    if (height != 0) params += "Height" -> height
    if (secsIn > 0)  params += "SecsIn" -> secsIn
    Try {
      val response = requestStream("GetPreviewImage", params)
      streamResponse(response, f)
    }
  }

  def getRecordingArtwork[U](artType: String, inetRef: String, season: Int, width: Int, height: Int)(f: (HttpStreamResponse) => U): ServiceResult[Unit] = {
    var params: Map[String, Any] = Map("Type" -> artType, "Inetref" -> inetRef, "Season" -> season)
    if (width != 0)  params += "Width" -> width
    if (height != 0) params += "Height" -> height
    Try {
      val response = requestStream("GetRecordingArtwork", params)
      streamResponse(response, f)
    }
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
