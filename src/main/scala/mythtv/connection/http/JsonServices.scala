package mythtv
package connection
package http

import spray.json.DefaultJsonProtocol

import model._
import services.{ Service, CaptureService, ChannelService, ContentService, DvrService,
  GuideService, MythService, VideoService }
import services.PagedList
import util.{ MythDateTime, OptionalCount, OptionalCountSome, MythFileHash }

import services.DataBytes // FIXME temporary placeholder

abstract class JsonService(conn: BackendJsonConnection)
  extends Service
     with BackendServiceProtocol
     with MythJsonProtocol {

  def request(endpoint: String, params: Map[String, Any] = Map.empty): JsonResponse =
    conn.request(buildPath(endpoint, params))

  def responseRoot(response: JsonResponse) =
    response.json.asJsObject

  def responseRoot(response: JsonResponse, fieldName: String) =
    response.json.asJsObject.fields(fieldName)
}


class JsonCaptureService(conn: BackendJsonConnection)
  extends JsonService(conn)
     with CaptureService {
  def getCaptureCard(cardId: CaptureCardId): CaptureCard = {
    val params: Map[String, Any] = Map("CardId" -> cardId.id)
    val response = request("GetCaptureCard", params)
    val root = responseRoot(response, "CaptureCard")
    root.convertTo[CaptureCard]
  }

  def getCaptureCardList(hostName: String, cardType: String): List[CaptureCard] = {
    var params: Map[String, Any] = Map.empty
    if (hostName.nonEmpty) params += "HostName" -> hostName
    if (cardType.nonEmpty) params += "CardType" -> cardType
    val response = request("GetCaptureCardList", params)
    val root = responseRoot(response, "CaptureCardList")
    root.convertTo[List[CaptureCard]]
  }
}

class JsonChannelService(conn: BackendJsonConnection)
  extends JsonService(conn)
     with ChannelService {
  def getChannelInfo(chanId: ChanId): ChannelDetails = {
    val params: Map[String, Any] = Map("ChanID" -> chanId.id)
    val response = request("GetChannelInfo", params)
    val root = responseRoot(response, "ChannelInfo")
    root.convertTo[ChannelDetails]
  }

  def getChannelInfoList(sourceId: ListingSourceId): PagedList[ChannelDetails] = {
    val params: Map[String, Any] = Map("SourceID" -> sourceId.id)
    val response = request("GetChannelInfoList", params)
    val root = responseRoot(response, "ChannelInfoList")
    root.convertTo[MythJsonPagedObjectList[ChannelDetails]]
  }

  def getVideoSource(sourceId: ListingSourceId): ListingSource = {
    val params: Map[String, Any] = Map("SourceID" -> sourceId.id)
    val response = request("GetVideoSource", params)
    val root = responseRoot(response, "VideoSource")
    root.convertTo[ListingSource]
  }

  def getVideoSourceList: List[ListingSource] = {
    val response = request("GetVideoSourceList")
    val root = responseRoot(response, "VideoSourceList")
    val list = root.convertTo[MythJsonObjectList[ListingSource]]
    list.items
  }

  def getVideoMultiplex(mplexId: MultiplexId): VideoMultiplex = {
    val params: Map[String, Any] = Map("MplexID" -> mplexId.id)
    val response = request("GetVideoMultiplex", params)
    val root = responseRoot(response, "VideoMultiplex")
    root.convertTo[VideoMultiplex]
  }

  def getVideoMultiplexList(sourceId: ListingSourceId, startIndex: Int, count: OptionalCount[Int]
  ): PagedList[VideoMultiplex] = {
    val params = buildStartCountParams(startIndex, count) + ("SourceID" -> sourceId.id)
    val response = request("GetVideoMultiplexList", params)
    val root = responseRoot(response, "VideoMultiplexList")
    root.convertTo[MythJsonPagedObjectList[VideoMultiplex]]
  }

  def getXmltvIdList(sourceId: ListingSourceId): List[String] = {
    val params: Map[String, Any] = Map("SourceID" -> sourceId.id)
    val response = request("GetXMLTVIdList", params)
    val root = responseRoot(response)
    root.convertTo[List[String]]
  }
}

class JsonDvrService(conn: BackendJsonConnection)
  extends JsonService(conn)
     with DvrService {
  // TODO catch when we get bogus data back and don't return an object?
  /* NB the 'ProgramFlags' field that we get back from GetRecorded seems to be wacky, whereas the same
     field for the same program in GetRecordedList seems to be correct. For an example, see the recording
     Martha Bakes: Bake it Dark, where flags are 0x2ff0f004 vs 0x1004

     We see the same discrepancy in the MythProtocol API queryRecorings() vs queryRecording()

     The root cause for this discrepancy seems to be that in ProgramInfo::LoadProgramFromRecorded,
     the programflags field is not initialized, and the ProgramInfo variable is allocated on the stack
     at the call site. Compare this to the implementation of LoadFromRecorded */
  def getRecorded(chanId: ChanId, startTime: MythDateTime): Recording = {
    val params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    val response = request("GetRecorded", params)
    val root = responseRoot(response, "Program")
    root.convertTo[Recording]
  }

  def getRecordedList(startIndex: Int, count: OptionalCount[Int], descending: Boolean): PagedList[Recording] = {
    var params = buildStartCountParams(startIndex, count)
    if (descending) params += "Descending" -> descending
    val response = request("GetRecordedList", params)
    val root = responseRoot(response, "ProgramList")
    root.convertTo[MythJsonPagedObjectList[Recording]]
  }

  def getExpiringList(startIndex: Int, count: OptionalCount[Int]): PagedList[Recording] = {
    val params = buildStartCountParams(startIndex, count)
    val response = request("GetExpiringList", params)
    val root = responseRoot(response, "ProgramList")
    root.convertTo[MythJsonPagedObjectList[Recording]]
  }

  def getUpcomingList(startIndex: Int, count: OptionalCount[Int], showAll: Boolean): PagedList[Recordable] = {
    var params = buildStartCountParams(startIndex, count)
    if (showAll) params += "ShowAll" -> showAll
    val response = request("GetUpcomingList", params)
    val root = responseRoot(response, "ProgramList")
    root.convertTo[MythJsonPagedObjectList[Recordable]]
  }

  def getConflictList(startIndex: Int, count: OptionalCount[Int]): PagedList[Recordable] = {
    val params = buildStartCountParams(startIndex, count)
    val response = request("GetConflictList", params)
    val root = responseRoot(response, "ProgramList")
    root.convertTo[MythJsonPagedObjectList[Recordable]]
  }

  def getEncoderList: List[RemoteEncoderState] = {
    val response = request("GetEncoderList")
    val root = responseRoot(response, "EncoderList")
    root.convertTo[List[RemoteEncoderState]]
  }

  def getRecordScheduleList(startIndex: Int, count: OptionalCount[Int]): PagedList[RecordRule] = {
    val params = buildStartCountParams(startIndex, count)
    val response = request("GetRecordScheduleList", params)
    val root = responseRoot(response, "RecRuleList")
    root.convertTo[MythJsonPagedObjectList[RecordRule]]
  }

  def getRecordSchedule(recordId: RecordRuleId): RecordRule = {
    val params: Map[String, Any] = Map("RecordId" -> recordId.id)
    val response = request("GetRecordSchedule", params)
    val root = responseRoot(response, "RecRule")
    root.convertTo[RecordRule]
  }

  def getRecGroupList: List[String] = {
    val response = request("GetRecGroupList")
    val root = responseRoot(response)
    root.convertTo[List[String]]
  }

  def getTitleList: List[String] = {
    val response = request("GetTitleList")
    val root = responseRoot(response)
    root.convertTo[List[String]]
  }

  def getTitleInfoList: List[TitleInfo] = ???

}

class JsonGuideService(conn: BackendJsonConnection)
  extends JsonService(conn)
     with GuideService {
  def getProgramGuide(
    startTime: MythDateTime,
    endTime: MythDateTime,
    startChanId: ChanId,
    numChannels: OptionalCount[Int],
    details: Boolean
  ): Guide[Channel, Program] = {
    // TODO there seems to be an off-by-one error on my 0.27 backend implementation of NumChannels
    //      NumChannels=1 returns 1 channels, NumChannels={n|n>1} returns n-1 channels
    var params: Map[String, Any] = Map("StartTime" -> startTime.toIsoFormat, "EndTime" -> endTime.toIsoFormat)
    if (startChanId.id != 0) params += "StartChanId" -> startChanId.id
    if (details) params += "Details" -> details
    numChannels match {
      case OptionalCountSome(n) => params += "NumChannels" -> n
      case _ => ()
    }
    val response = request("GetProgramGuide", params)
    val root = responseRoot(response, "ProgramGuide")
    root.convertTo[Guide[Channel, Program]]
  }

  /*
   * Note: for a program currently recording, the StartTS field inside the Recording
   * object will contain the actual time the recording started (which may not be the
   * same as the program start time). However, once the recording is completed, this
   * field reverts to containing the actual start time of the program, and thus is
   * unsuitable for looking up the actual recording in all cases.  TODO example
   */
  def getProgramDetails(chanId: ChanId, startTime: MythDateTime): Program = {
    val params: Map[String, Any] = Map("StartTime" -> startTime.toIsoFormat, "ChanId" -> chanId.id)
    val response = request("GetProgramDetails", params)
    val root = responseRoot(response, "Program")
    root.convertTo[Program]
  }

  def getChannelIcon(chanId: ChanId) = ???
}

class JsonMythService(conn: BackendJsonConnection)
  extends JsonService(conn)
     with MythService {
  def getHostName: String = {
    import DefaultJsonProtocol.StringJsonFormat
    val response = request("GetHostName")
    val root = responseRoot(response, "String")
    root.convertTo[String]
  }

  def getHosts: List[String] = {
    val response = request("GetHosts")
    val root = responseRoot(response)
    root.convertTo[List[String]]
  }

  def getKeys: List[String] = {
    val response = request("GetKeys")
    val root = responseRoot(response)
    root.convertTo[List[String]]
  }

  def getSetting(hostName: String, key: String): Settings = {
    var params: Map[String, Any] = Map("HostName" -> hostName)
    if (key.nonEmpty) params += "Key" -> key
    val response = request("GetSetting", params)
    val root = responseRoot(response, "SettingList")
    root.convertTo[Settings]
  }

  def getStorageGroupDirs(hostName: String, groupName: String): List[StorageGroupDir] = ???

  def getTimeZone: TimeZoneInfo = {
    val response = request("GetTimeZone")
    val root = responseRoot(response, "TimeZoneInfo")
    root.convertTo[TimeZoneInfo]
  }
}

class JsonContentService(conn: BackendJsonConnection)
  extends JsonService(conn)
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

  def getAlbumArt(id: Int): DataBytes = ???
  def getFile(storageGroup: String, fileName: String): DataBytes = ???
  def getImageFile(storageGroup: String, fileName: String): DataBytes = ???
  def getLiveStream(id: String): LiveStreamInfo = ???
  def getLiveStreamList: List[LiveStreamInfo] = ???
  def getMusic(id: String): DataBytes = ???
  def getPreviewImage(chanId: ChanId, startTime: MythDateTime): DataBytes = ???
  def getRecording(chanId: ChanId, startTime: MythDateTime): DataBytes = ???
  def getVideo(id: Int): DataBytes = ???
}

class JsonVideoService(conn: BackendJsonConnection)
  extends JsonService(conn)
     with VideoService {
  def getVideo(videoId: VideoId): Video = {
    val params: Map[String, Any] = Map("Id" -> videoId.id)
    val response = request("GetVideo", params)
    val root = responseRoot(response, "VideoMetadataInfo")
    root.convertTo[Video]
  }

  def getVideoByFileName(fileName: String): Video = {
    val params: Map[String, Any] = Map("FileName" -> fileName)
    val response = request("GetVideoByFileName", params)
    val root = responseRoot(response, "VideoMetadataInfo")
    root.convertTo[Video]
  }

  def getVideoList(startIndex: Int, count: OptionalCount[Int], descending: Boolean): PagedList[Video] = {
    var params = buildStartCountParams(startIndex, count)
    if (descending) params += "Descending" -> descending
    val response = request("GetVideoList", params)
    val root = responseRoot(response, "VideoMetadataInfoList")
    root.convertTo[MythJsonPagedObjectList[Video]]
  }
}
