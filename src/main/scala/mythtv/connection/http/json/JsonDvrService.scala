package mythtv
package connection
package http
package json

import java.time.LocalTime

import model._
import util.{ MythDateTime, OptionalCount }
import services.{ DvrService, PagedList }
import RichJsonObject._

class JsonDvrService(conn: BackendJsonConnection)
  extends JsonBackendService(conn)
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

  def getTitleInfoList: List[TitleInfo] = {
    val response = request("GetTitleInfoList")
    val root = responseRoot(response, "TitleInfoList")
    root.convertTo[List[TitleInfo]]
  }

  /* POST methods */

  def removeRecorded(chanId: ChanId, startTime: MythDateTime): Boolean = {
    val params: Map[String, Any] = Map(
      "ChanId" -> chanId.id,
      "StartTime" -> startTime.toIsoFormat
    )
    val response = post("RemoveRecorded", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  def addRecordSchedule(rule: RecordRule): RecordRuleId = {
    val params: Map[String, Any] = Map(
      "Title"          -> rule.title,
      "Subtitle"       -> rule.subtitle,
      "Description"    -> rule.description,
      "Category"       -> rule.category,
      "StartTime"      -> rule.startTime.toIsoFormat,
      "EndTime"        -> rule.endTime.toIsoFormat,
      "SeriesId"       -> rule.seriesId.getOrElse(""),
      "ProgramId"      -> rule.programId.getOrElse(""),
      "ChanId"         -> rule.chanId.map(_.id).getOrElse(0),
      "Station"        -> rule.callsign,
      "FindDay"        -> rule.findDay,
      "FindTime"       -> rule.findTime.getOrElse(LocalTime.MIN),
      "ParentId"       -> rule.parentId.map(_.id).getOrElse(0),
      "Inactive"       -> rule.inactive,
      "Season"         -> rule.season.getOrElse(0),
      "Episode"        -> rule.episode.getOrElse(0),
      "Inetref"        -> rule.inetRef.getOrElse(""),
      "Type"           -> RecTypeJsonFormat.id2Description(rule.recType),
      "SearchType"     -> RecSearchTypeJsonFormat.id2Description(rule.searchType),
      "RecPriority"    -> rule.recPriority,
      "PreferredInput" -> rule.preferredInput.map(_.id).getOrElse(0),
      "StartOffset"    -> rule.startOffset,
      "EndOffset"      -> rule.endOffset,
      "DupMethod"      -> DupCheckMethodJsonFormat.id2Description(rule.dupMethod),
      "DupIn"          -> DupCheckInJsonFormat.id2Description(rule.dupIn),
      "Filter"         -> rule.filter.getOrElse(0),
      "RecProfile"     -> rule.recProfile,
      "RecGroup"       -> rule.recGroup,
      "StorageGroup"   -> rule.storageGroup,
      "PlayGroup"      -> rule.playGroup,
      "AutoExpire"     -> rule.autoExpire,
      "MaxEpisodes"    -> rule.maxEpisodes,
      "MaxNewest"      -> rule.maxNewest,
      "AutoCommflag"   -> rule.autoCommFlag,
      "AutoTranscode"  -> rule.autoTranscode,
      "AutoMetaLookup" -> rule.autoMetadata,
      "AutoUserJob1"   -> rule.autoUserJob1,
      "AutoUserJob2"   -> rule.autoUserJob2,
      "AutoUserJob3"   -> rule.autoUserJob3,
      "AutoUserJob4"   -> rule.autoUserJob4,
      "Transcoder"     -> rule.transcoder.getOrElse(0)
    )
    val response = post("UpdateRecordSchedule", params)
    val root = responseRoot(response)
    RecordRuleId(0) // FIXME
  }

  def updateRecordSchedule(rule: RecordRule): Boolean = {
    val params: Map[String, Any] = Map(
      "RecordId"       -> rule.id,
      "Title"          -> rule.title,
      "Subtitle"       -> rule.subtitle,
      "Description"    -> rule.description,
      "Category"       -> rule.category,
      "StartTime"      -> rule.startTime.toIsoFormat,
      "EndTime"        -> rule.endTime.toIsoFormat,
      "SeriesId"       -> rule.seriesId.getOrElse(""),
      "ProgramId"      -> rule.programId.getOrElse(""),
      "ChanId"         -> rule.chanId.map(_.id).getOrElse(0),
      "Station"        -> rule.callsign,
      "FindDay"        -> rule.findDay,
      "FindTime"       -> rule.findTime.getOrElse(LocalTime.MIN),
      "Inactive"       -> rule.inactive,
      "Season"         -> rule.season.getOrElse(0),
      "Episode"        -> rule.episode.getOrElse(0),
      "Inetref"        -> rule.inetRef.getOrElse(""),
      "Type"           -> RecTypeJsonFormat.id2Description(rule.recType),
      "SearchType"     -> RecSearchTypeJsonFormat.id2Description(rule.searchType),
      "RecPriority"    -> rule.recPriority,
      "PreferredInput" -> rule.preferredInput.map(_.id).getOrElse(0),
      "StartOffset"    -> rule.startOffset,
      "EndOffset"      -> rule.endOffset,
      "DupMethod"      -> DupCheckMethodJsonFormat.id2Description(rule.dupMethod),
      "DupIn"          -> DupCheckInJsonFormat.id2Description(rule.dupIn),
      "Filter"         -> rule.filter.getOrElse(0),
      "RecProfile"     -> rule.recProfile,
      "RecGroup"       -> rule.recGroup,
      "StorageGroup"   -> rule.storageGroup,
      "PlayGroup"      -> rule.playGroup,
      "AutoExpire"     -> rule.autoExpire,
      "MaxEpisodes"    -> rule.maxEpisodes,
      "MaxNewest"      -> rule.maxNewest,
      "AutoCommflag"   -> rule.autoCommFlag,
      "AutoTranscode"  -> rule.autoTranscode,
      "AutoMetaLookup" -> rule.autoMetadata,
      "AutoUserJob1"   -> rule.autoUserJob1,
      "AutoUserJob2"   -> rule.autoUserJob2,
      "AutoUserJob3"   -> rule.autoUserJob3,
      "AutoUserJob4"   -> rule.autoUserJob4,
      "Transcoder"     -> rule.transcoder.getOrElse(0)
    )
    val response = post("UpdateRecordSchedule", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  def removeRecordSchedule(recordId: RecordRuleId): Boolean = {
    val params: Map[String, Any] = Map("RecordId" -> recordId.id)
    val response = post("RemoveRecordSchedule", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  def disableRecordSchedule(recordId: RecordRuleId): Boolean = {
    val params: Map[String, Any] = Map("RecordId" -> recordId.id)
    val response = post("DisableRecordSchedule", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  def enableRecordSchedule(recordId: RecordRuleId): Boolean = {
    val params: Map[String, Any] = Map("RecordId" -> recordId.id)
    val response = post("EnableRecordSchedule", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  def updateRecordedWatchedStatus(chanId: ChanId, startTime: MythDateTime, watched: Boolean): Boolean = {
    val params: Map[String, Any] = Map(
      "ChanId" -> chanId.id,
      "StartTime" -> startTime.toIsoFormat,
      "Watched" -> watched
    )
    val response = post("UpdateRecordedWatchedStatus", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

}
