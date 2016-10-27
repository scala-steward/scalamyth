package mythtv
package connection
package http

import spray.json.DefaultJsonProtocol

import model._
import services.{ CaptureService, ChannelService, GuideService, DvrService, MythService }
import util.{ MythDateTime, OptionalCount }

class JsonCaptureService(conn: BackendJSONConnection)
  extends BackendServiceProtocol
     with MythJsonProtocol
     with CaptureService {
  def getCaptureCard(cardId: CaptureCardId): CaptureCard = {
    val params: Map[String, Any] = Map("CardId" -> cardId.id)
    val response = conn.request(buildPath("GetCaptureCard", params))
    val root = response.json.asJsObject.fields("CaptureCard")
    root.convertTo[CaptureCard]
  }

  def getCaptureCardList(hostName: String, cardType: String): List[CaptureCard] = {
    // TODO support parameters
    val response = conn.request(buildPath("GetCaptureCardList"))
    val root = response.json.asJsObject.fields("CaptureCardList")
    root.convertTo[List[CaptureCard]]
  }
}

class JsonChannelService(conn: BackendJSONConnection)
  extends BackendServiceProtocol
     with MythJsonProtocol
     with ChannelService {
  def getChannelInfo(chanId: ChanId): Channel = {
    val params: Map[String, Any] = Map("ChanID" -> chanId.id)
    val response = conn.request(buildPath("GetChannelInfo", params))
    val root = response.json.asJsObject.fields("ChannelInfo")
    root.convertTo[ChannelDetails]
  }

  def getChannelInfoList: List[Channel] = {
    val response = conn.request(buildPath("GetChannelInfoList"))
    val root = response.json.asJsObject.fields("ChannelInfoList")
    val list = root.convertTo[MythJsonPagedObjectList[ChannelDetails]]
    list.items
  }

  def getVideoSource(sourceId: ListingSourceId): ListingSource = {
    val params: Map[String, Any] = Map("SourceID" -> sourceId.id)
    val response = conn.request(buildPath("GetVideoSource", params))
    val root = response.json.asJsObject.fields("VideoSource")
    root.convertTo[ListingSource]
  }

  def getVideoSourceList: List[ListingSource] = {
    val response = conn.request(buildPath("GetVideoSourceList"))
    val root = response.json.asJsObject.fields("VideoSourceList")
    val list = root.convertTo[MythJsonObjectList[ListingSource]]
    list.items
  }

  def getVideoMultiplex(mplexId: Int): VideoMultiplex = ???

  def getVideoMultiplexList(sourceId: ListingSourceId): List[VideoMultiplex] = ???

  def getXmltvIdList(sourceId: ListingSourceId): List[String] = {
    val params: Map[String, Any] = Map("SourceID" -> sourceId.id)
    val response = conn.request(buildPath("GetXMLTVIdList", params))
    val root = response.json.asJsObject
    root.convertTo[List[String]]
  }
}

class JsonDvrService(conn: BackendJSONConnection)
  extends BackendServiceProtocol
     with MythJsonProtocol
     with DvrService {
  // TODO catch when we get bogus data back and don't return an object?
  def getRecorded(chanId: ChanId, startTime: MythDateTime): Program = {
    val params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    val response = conn.request(buildPath("GetRecorded", params))
    val root = response.json.asJsObject.fields("Program")
    root.convertTo[Program]
  }

  def getRecordedList(startIndex: Int, count: OptionalCount[Int], descending: Boolean): List[Program] = {
    // TODO support parameters
    val response = conn.request(buildPath("GetRecordedList"))
    val root = response.json.asJsObject.fields("ProgramList")
    val list = root.convertTo[MythJsonPagedObjectList[Program]]
    list.items
  }

  def getExpiringList(startIndex: Int, count: OptionalCount[Int]): List[Program] = {
    // TODO support parameters
    val response = conn.request(buildPath("GetExpiringList"))
    val root = response.json.asJsObject.fields("ProgramList")
    val list = root.convertTo[MythJsonPagedObjectList[Program]]
    list.items
  }

  def getUpcomingList(startIndex: Int, count: OptionalCount[Int], showAll: Boolean): List[Program] = {
    val response = conn.request(buildPath("GetUpcomingList"))
    val root = response.json.asJsObject.fields("ProgramList")
    val list = root.convertTo[MythJsonPagedObjectList[Program]]
    list.items
  }

  def getConflictList(startIndex: Int, count: OptionalCount[Int]): List[Program] = ???

  def getEncoderList: List[RemoteEncoderState] = {
    val response = conn.request(buildPath("GetEncoderList"))
    val root = response.json.asJsObject.fields("EncoderList")
    root.convertTo[List[RemoteEncoderState]]
  }

  def getRecordScheduleList(startIndex: Int, count: OptionalCount[Int]): List[RecordRule] = {
    // TODO support parameters
    val response = conn.request(buildPath("GetRecordScheduleList"))
    val root = response.json.asJsObject.fields("RecRuleList")
    val list = root.convertTo[MythJsonPagedObjectList[RecordRule]]
    list.items
  }

  def getRecordSchedule(recordId: RecordRuleId): RecordRule = {
    val params: Map[String, Any] = Map("RecordId" -> recordId.id)
    val response = conn.request(buildPath("GetRecordSchedule", params))
    val root = response.json.asJsObject.fields("RecRule")
    root.convertTo[RecordRule]
  }

  def getRecGroupList: List[String] = {
    val response = conn.request(buildPath("GetRecGroupList"))
    val root = response.json.asJsObject
    root.convertTo[List[String]]
  }

  def getTitleList: List[String] = {
    val response = conn.request(buildPath("GetTitleList"))
    val root = response.json.asJsObject
    root.convertTo[List[String]]
  }

  def getTitleInfoList: List[TitleInfo] = ???

}

class JsonGuideService(conn: BackendJSONConnection)
  extends BackendServiceProtocol
     with MythJsonProtocol
     with GuideService {
  def getProgramGuide(
    startTime: MythDateTime,
    endTime: MythDateTime,
    startChanId: ChanId,
    numChannels: OptionalCount[Int],
    details: Boolean
  ): Guide[Channel, Program] = {
    // TODO support parameters
    val params: Map[String, Any] = Map("StartTime" -> startTime.toIsoFormat, "EndTime" -> endTime.toIsoFormat)
    val response = conn.request(buildPath("GetProgramGuide", params))
    val root = response.json.asJsObject.fields("ProgramGuide")
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
    val response = conn.request(buildPath("GetProgramDetails", params))
    val root = response.json.asJsObject.fields("Program")
    root.convertTo[Program]
  }

  def getChannelIcon(chanId: ChanId) = ???
}

class JsonMythService(conn: BackendJSONConnection)
  extends BackendServiceProtocol
     with MythJsonProtocol
     with MythService {
  def getHostName: String = {
    import DefaultJsonProtocol.StringJsonFormat
    val response = conn.request(buildPath("GetHostName"))
    val root = response.json.asJsObject.fields("String")
    root.convertTo[String]
  }

  def getHosts: List[String] = {
    val response = conn.request(buildPath("GetHosts"))
    val root = response.json.asJsObject
    root.convertTo[List[String]]
  }

  def getKeys: List[String] = {
    val response = conn.request(buildPath("GetKeys"))
    val root = response.json.asJsObject
    root.convertTo[List[String]]
  }

  def getSetting(hostName: String, key: String): Settings = {
    var params: Map[String, Any] = Map("HostName" -> hostName)
    if (key.nonEmpty) params += "Key" -> key
    val response = conn.request(buildPath("GetSetting", params))
    val root = response.json.asJsObject.fields("SettingList")
    root.convertTo[Settings]
  }

  def getStorageGroupDirs(hostName: String, groupName: String): List[StorageGroupDir] = ???

  def getTimeZone: TimeZoneInfo = {
    val response = conn.request(buildPath("GetTimeZone"))
    val root = response.json.asJsObject.fields("TimeZoneInfo")
    root.convertTo[TimeZoneInfo]
  }

}
