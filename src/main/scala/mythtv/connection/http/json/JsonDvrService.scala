package mythtv
package connection
package http
package json

import java.time.LocalTime

import scala.util.Try

import model._
import util.{ MythDateTime, OptionalCount }
import services.{ DvrService, PagedList, ServiceResult }
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
  def getRecorded(chanId: ChanId, startTime: MythDateTime): ServiceResult[Recording] = {
    val params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    for {
      response <- Try( request("GetRecorded", params) )
      root     <- Try( responseRoot(response, "Program") )
      result   <- Try( root.convertTo[Recording] )
    } yield result
  }

  def getRecordedList(startIndex: Int, count: OptionalCount[Int], descending: Boolean): ServiceResult[PagedList[Recording]] = {
    var params = buildStartCountParams(startIndex, count)
    if (descending) params += "Descending" -> descending
    for {
      response <- Try( request("GetRecordedList", params) )
      root     <- Try( responseRoot(response, "ProgramList") )
      result   <- Try( root.convertTo[MythJsonPagedObjectList[Recording]] )
    } yield result
  }

  def getExpiringList(startIndex: Int, count: OptionalCount[Int]): ServiceResult[PagedList[Recording]] = {
    val params = buildStartCountParams(startIndex, count)
    for {
      response <- Try( request("GetExpiringList", params) )
      root     <- Try( responseRoot(response, "ProgramList") )
      result   <- Try( root.convertTo[MythJsonPagedObjectList[Recording]] )
    } yield result
  }

  def getUpcomingList(startIndex: Int, count: OptionalCount[Int], showAll: Boolean): ServiceResult[PagedList[Recordable]] = {
    var params = buildStartCountParams(startIndex, count)
    if (showAll) params += "ShowAll" -> showAll
    for {
      response <- Try( request("GetUpcomingList", params) )
      root     <- Try( responseRoot(response, "ProgramList") )
      result   <- Try( root.convertTo[MythJsonPagedObjectList[Recordable]] )
    } yield result
  }

  def getConflictList(startIndex: Int, count: OptionalCount[Int]): ServiceResult[PagedList[Recordable]] = {
    val params = buildStartCountParams(startIndex, count)
    for {
      response <- Try( request("GetConflictList", params) )
      root     <- Try( responseRoot(response, "ProgramList") )
      result   <- Try( root.convertTo[MythJsonPagedObjectList[Recordable]] )
    } yield result
  }

  def getEncoderList: ServiceResult[List[RemoteEncoderState]] = {
    for {
      response <- Try( request("GetEncoderList") )
      root     <- Try( responseRoot(response, "EncoderList") )
      result   <- Try( root.convertTo[List[RemoteEncoderState]] )
    } yield result
  }

  def getRecordScheduleList(startIndex: Int, count: OptionalCount[Int]): ServiceResult[PagedList[RecordRule]] = {
    val params = buildStartCountParams(startIndex, count)
    for {
      response <- Try( request("GetRecordScheduleList", params) )
      root     <- Try(  responseRoot(response, "RecRuleList") )
      result   <- Try( root.convertTo[MythJsonPagedObjectList[RecordRule]] )
    } yield result
  }

  def getRecordSchedule(recordId: RecordRuleId): ServiceResult[RecordRule] = {
    val params: Map[String, Any] = Map("RecordId" -> recordId.id)
    for {
      response <- Try( request("GetRecordSchedule", params) )
      root     <- Try( responseRoot(response, "RecRule") )
      result   <- Try( root.convertTo[RecordRule] )
    } yield result
  }

  def getRecGroupList: ServiceResult[List[String]] = {
    for {
      response <- Try( request("GetRecGroupList") )
      root     <- Try( responseRoot(response) )
      result   <- Try( root.convertTo[List[String]] )
    } yield result
  }

  def getTitleList: ServiceResult[List[String]] = {
    for {
      response <- Try( request("GetTitleList") )
      root     <- Try( responseRoot(response) )
      result   <- Try( root.convertTo[List[String]] )
    } yield result
  }

  def getTitleInfoList: ServiceResult[List[TitleInfo]] = {
    for {
      response <- Try( request("GetTitleInfoList") )
      root     <- Try( responseRoot(response, "TitleInfoList") )
      result   <- Try( root.convertTo[List[TitleInfo]] )
    } yield result
  }

  /* POST methods */

  def removeRecorded(chanId: ChanId, startTime: MythDateTime): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "ChanId" -> chanId.id,
      "StartTime" -> startTime.toIsoFormat
    )
    for {
      response <- Try( post("RemoveRecorded", params) )
      root     <- Try( responseRoot(response) )
      result   <- Try( root.booleanField("bool") )
    } yield result
  }

  def addRecordSchedule(rule: RecordRule): ServiceResult[RecordRuleId] = {
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
    for {
      response <- Try( post("UpdateRecordSchedule", params) )
      root     <- Try( responseRoot(response) )
      result   <- Try( RecordRuleId(0) ) // FIXME
    } yield result
  }

  def updateRecordSchedule(rule: RecordRule): ServiceResult[Boolean] = {
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
    for {
      response <- Try( post("UpdateRecordSchedule", params) )
      root     <- Try( responseRoot(response) )
      result   <- Try( root.booleanField("bool") )
    } yield result
  }

  def removeRecordSchedule(recordId: RecordRuleId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("RecordId" -> recordId.id)
    for {
      response <- Try( post("RemoveRecordSchedule", params) )
      root     <- Try( responseRoot(response) )
      result   <- Try( root.booleanField("bool") )
    } yield result
  }

  def disableRecordSchedule(recordId: RecordRuleId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("RecordId" -> recordId.id)
    for {
      response <- Try( post("DisableRecordSchedule", params) )
      root     <- Try( responseRoot(response) )
      result   <- Try( root.booleanField("bool") )
    } yield result
  }

  def enableRecordSchedule(recordId: RecordRuleId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("RecordId" -> recordId.id)
    for {
      response <- Try( post("EnableRecordSchedule", params) )
      root     <- Try( responseRoot(response) )
      result   <- Try( root.booleanField("bool") )
    } yield result
  }

  def updateRecordedWatchedStatus(chanId: ChanId, startTime: MythDateTime, watched: Boolean): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "ChanId" -> chanId.id,
      "StartTime" -> startTime.toIsoFormat,
      "Watched" -> watched
    )
    for {
      response <- Try( post("UpdateRecordedWatchedStatus", params) )
      root     <- Try( responseRoot(response) )
      result   <- Try( root.booleanField("bool") )
    } yield result
  }

}
