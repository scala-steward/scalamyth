package mythtv
package connection
package http
package json

import java.time.LocalTime
import java.time.format.DateTimeFormatter

import scala.util.Try

import spray.json.DefaultJsonProtocol.StringJsonFormat

import model._
import util.{ MythDateTime, OptionalCount }
import services.{ DvrService, PagedList, ServiceResult }
import services.Service.ServiceFailure.ServiceNoResult
import EnumTypes.{ DupCheckIn, DupCheckMethod, RecStatus, RecType }
import RichJsonObject._
import RecordedId._

class JsonDvrService(conn: BackendJsonConnection)
  extends JsonBackendService(conn)
     with DvrService {

  private def timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

  /* NB the 'ProgramFlags' field that we get back from GetRecorded seems to be wacky, whereas the same
     field for the same program in GetRecordedList seems to be correct. For an example, see the recording
     Martha Bakes: Bake it Dark, where flags are 0x2ff0f004 vs 0x1004

     We see the same discrepancy in the MythProtocol API queryRecorings() vs queryRecording()

     The root cause for this discrepancy seems to be that in ProgramInfo::LoadProgramFromRecorded,
     the programflags field is not initialized, and the ProgramInfo variable is allocated on the stack
     at the call site. Compare this to the implementation of LoadFromRecorded FIXME UPSTREAM */
  def getRecorded(chanId: ChanId, startTime: MythDateTime): ServiceResult[Recording] = {
    val params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    val recTry = for {
      response <- request("GetRecorded", params)
      root     <- responseRoot(response, "Program")
      result   <- Try(root.convertTo[Recording])
    } yield result
    if (recTry.isSuccess && recTry.get.isDummy) Left(ServiceNoResult)
    else recTry
  }

  def getRecorded(recordedId: RecordedId): ServiceResult[Recording] = recordedId match {
    case RecordedIdInt(id) =>
      val params: Map[String, Any] = Map("RecordedId" -> id)
      val recTry = for {
        response <- request("GetRecorded", params)
        root     <- responseRoot(response, "Program")
        result   <- Try(root.convertTo[Recording])
      } yield result
      if (recTry.isSuccess && recTry.get.isDummy) Left(ServiceNoResult)
      else recTry
    case RecordedIdChanTime(chanId, startTime) => getRecorded(chanId, startTime)
  }

  def getRecordedList(
    startIndex: Int,
    count: OptionalCount[Int],
    descending: Boolean,
    titleRegex: String,
    recGroup: String,
    storageGroup: String
  ): ServiceResult[PagedList[Recording]] = {
    var params = buildStartCountParams(startIndex, count)
    if (descending)            params += "Descending"   -> descending
    if (titleRegex.nonEmpty)   params += "TitleRegEx"   -> titleRegex
    if (recGroup.nonEmpty)     params += "RecGroup"     -> recGroup
    if (storageGroup.nonEmpty) params += "StorageGroup" -> storageGroup
    for {
      response <- request("GetRecordedList", params)
      root     <- responseRoot(response, "ProgramList")
      result   <- Try(root.convertTo[MythJsonPagedObjectList[Recording]])
    } yield result
  }

  def getExpiringList(startIndex: Int, count: OptionalCount[Int]): ServiceResult[PagedList[Recording]] = {
    val params = buildStartCountParams(startIndex, count)
    for {
      response <- request("GetExpiringList", params)
      root     <- responseRoot(response, "ProgramList")
      result   <- Try(root.convertTo[MythJsonPagedObjectList[Recording]])
    } yield result
  }

  def getUpcomingList(
    startIndex: Int,
    count: OptionalCount[Int],
    showAll: Boolean,
    recordRuleId: RecordRuleId,
    recStatus: RecStatus
  ): ServiceResult[PagedList[Recordable]] = {
    var params = buildStartCountParams(startIndex, count)
    if (showAll)                        params += "ShowAll" -> showAll
    if (recordRuleId.id != 0)           params += "RecordId" -> recordRuleId.id
    if (recStatus != RecStatus.Unknown) params += "RecStatus" -> recStatus.id
    for {
      response <- request("GetUpcomingList", params)
      root     <- responseRoot(response, "ProgramList")
      result   <- Try(root.convertTo[MythJsonPagedObjectList[Recordable]])
    } yield result
  }

  def getConflictList(
    startIndex: Int,
    count: OptionalCount[Int],
    recordRuleId: RecordRuleId
  ): ServiceResult[PagedList[Recordable]] = {
    var params = buildStartCountParams(startIndex, count)
    if (recordRuleId.id != 0) params += "RecordId" -> recordRuleId.id
    for {
      response <- request("GetConflictList", params)
      root     <- responseRoot(response, "ProgramList")
      result   <- Try(root.convertTo[MythJsonPagedObjectList[Recordable]])
    } yield result
  }

  def getEncoderList: ServiceResult[List[RemoteEncoderState]] = {
    for {
      response <- request("GetEncoderList")
      root     <- responseRoot(response, "EncoderList")
      result   <- Try(root.convertTo[List[RemoteEncoderState]])
    } yield result
  }

  def getRecordScheduleList(
    startIndex: Int,
    count: OptionalCount[Int],
    sortBy: String,
    descending: Boolean
  ): ServiceResult[PagedList[RecordRule]] = {
    var params = buildStartCountParams(startIndex, count)
    if (sortBy.nonEmpty) params += "Sort" -> sortBy
    if (descending)      params += "Descending" -> descending
    for {
      response <- request("GetRecordScheduleList", params)
      root     <- responseRoot(response, "RecRuleList")
      result   <- Try(root.convertTo[MythJsonPagedObjectList[RecordRule]])
    } yield result
  }

  def getRecordSchedule(recordId: RecordRuleId): ServiceResult[RecordRule] = {
    val params: Map[String, Any] = Map("RecordId" -> recordId.id)
    for {
      response <- request("GetRecordSchedule", params)
      root     <- responseRoot(response, "RecRule")
      result   <- Try(root.convertTo[RecordRule])
    } yield result
  }

  def getRecordSchedule(recordedId: RecordedId): ServiceResult[RecordRule] = recordedId match {
    case RecordedIdInt(id) =>
      val params: Map[String, Any] = Map("RecordedId" -> id)
      for {
        response <- request("GetRecordSchedule", params)
        root     <- responseRoot(response, "RecRule")
        result   <- Try(root.convertTo[RecordRule])
      } yield result
    case RecordedIdChanTime(chanId, startTime) =>
      // we can't use getRecordSchedule(chanId, startTime) on an existing recording,
      // so query the record rule id of the recording and use that as our parameter
      for {
        rec  <- getRecorded(chanId, startTime)
        rule <- getRecordSchedule(rec.recordId)
      } yield rule
  }

  def getRecordSchedule(template: String): ServiceResult[RecordRule] = {
    val params: Map[String, Any] = Map("Template" -> template)
    for {
      response <- request("GetRecordSchedule", params)
      root     <- responseRoot(response, "RecRule")
      result   <- Try(root.convertTo[RecordRule])
    } yield result
  }

  def getRecordSchedule(chanId: ChanId, startTime: MythDateTime, makeOverride: Boolean): ServiceResult[RecordRule] = {
    var params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    if (makeOverride) params += "MakeOverride" -> makeOverride
    for {
      response <- request("GetRecordSchedule", params)
      root     <- responseRoot(response, "RecRule")
      result   <- Try(root.convertTo[RecordRule])
    } yield result
  }

  def getRecGroupList: ServiceResult[List[String]] = {
    for {
      response <- request("GetRecGroupList")
      root     <- responseRoot(response)
      result   <- Try(root.convertTo[List[String]])
    } yield result
  }

  def getTitleList(recGroup: String): ServiceResult[List[String]] = {
    var params: Map[String, Any] = Map.empty
    if (recGroup.nonEmpty) params += "RecGroup" -> recGroup
    for {
      response <- request("GetTitleList", params)
      root     <- responseRoot(response)
      result   <- Try(root.convertTo[List[String]])
    } yield result
  }

  def getTitleInfoList: ServiceResult[List[TitleInfo]] = {
    for {
      response <- request("GetTitleInfoList")
      root     <- responseRoot(response, "TitleInfoList")
      result   <- Try(root.convertTo[List[TitleInfo]])
    } yield result
  }

  /* POST methods */

  private def internalRemoveRecorded(
    partialParams: Map[String, Any],
    forceDelete: Boolean,
    allowReRecord: Boolean
  ): ServiceResult[Boolean] = {
    var params = partialParams
    if (forceDelete)   params += "ForceDelete" -> forceDelete
    if (allowReRecord) params += "AllowRerecord" -> allowReRecord
    for {
      response <- post("RemoveRecorded", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def removeRecorded(
    chanId: ChanId,
    startTime: MythDateTime,
    forceDelete: Boolean,
    allowReRecord: Boolean
  ): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    internalRemoveRecorded(params, forceDelete, allowReRecord)
  }

  def removeRecorded(
    recordedId: RecordedId,
    forceDelete: Boolean = false,
    allowReRecord: Boolean = false
  ): ServiceResult[Boolean] = recordedId match {
    case RecordedIdInt(id) => internalRemoveRecorded(Map("RecordedId" -> id), forceDelete, allowReRecord)
    case RecordedIdChanTime(chanId, startTime) => removeRecorded(chanId, startTime)
  }

  private def internalDeleteRecording(
    partialParams: Map[String, Any],
    forceDelete: Boolean,
    allowReRecord: Boolean
  ): ServiceResult[Boolean] = {
    var params = partialParams
    if (forceDelete)   params += "ForceDelete" -> forceDelete
    if (allowReRecord) params += "AllowRerecord" -> allowReRecord
    for {
      response <- post("DeleteRecording", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def deleteRecording(
    chanId: ChanId,
    startTime: MythDateTime,
    forceDelete: Boolean,
    allowReRecord: Boolean
  ): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    internalDeleteRecording(params, forceDelete, allowReRecord)
  }

  def deleteRecording(
    recordedId: RecordedId,
    forceDelete: Boolean,
    allowReRecord: Boolean
  ): ServiceResult[Boolean] = recordedId match {
    case RecordedIdInt(id) => internalDeleteRecording(Map("RecordedId" -> id), forceDelete, allowReRecord)
    case RecordedIdChanTime(chanId, startTime) => deleteRecording(chanId, startTime, forceDelete, allowReRecord)
  }

  def undeleteRecording(chanId: ChanId, startTime: MythDateTime): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    for {
      response <- post("UnDeleteRecording", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def undeleteRecording(recordedId: RecordedId): ServiceResult[Boolean] = recordedId match {
    case RecordedIdInt(id) =>
      val params: Map[String, Any] = Map("RecordedId" -> id)
      for {
        response <- post("UnDeleteRecording", params)
        root     <- responseRoot(response)
        result   <- Try(root.booleanField("bool"))
      } yield result
    case RecordedIdChanTime(chanId, startTime) => undeleteRecording(chanId, startTime)
  }

  // All parameters are technically optional and will be filled in with defaults?
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
      "FindTime"       -> rule.findTime.getOrElse(LocalTime.MIN).format(timeFormatter),
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
      response <- post("AddRecordSchedule", params)
      root     <- responseRoot(response)
      result   <- Try(RecordRuleId(root.intField("uint")))
    } yield result
  }

  def updateRecordSchedule(rule: RecordRule): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "RecordId"       -> rule.id.id,
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
      "FindTime"       -> rule.findTime.getOrElse(LocalTime.MIN).format(timeFormatter),
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
      response <- post("UpdateRecordSchedule", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def removeRecordSchedule(recordId: RecordRuleId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("RecordId" -> recordId.id)
    for {
      response <- post("RemoveRecordSchedule", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def disableRecordSchedule(recordId: RecordRuleId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("RecordId" -> recordId.id)
    for {
      response <- post("DisableRecordSchedule", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def enableRecordSchedule(recordId: RecordRuleId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("RecordId" -> recordId.id)
    for {
      response <- post("EnableRecordSchedule", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def addDontRecordSchedule(chanId: ChanId, startTime: MythDateTime, neverRecord: Boolean): ServiceResult[Boolean] = {
    var params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    if (neverRecord) params += "NeverRecord" -> neverRecord
    for {
      response <- post("AddDontRecordSchedule", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  private def internalUpdateRecordedWatchedStatus(partialParams: Map[String, Any], watched: Boolean): ServiceResult[Boolean] = {
    val params: Map[String, Any] = partialParams + ("Watched" -> watched)
    for {
      response <- post("UpdateRecordedWatchedStatus", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def updateRecordedWatchedStatus(chanId: ChanId, startTime: MythDateTime, watched: Boolean): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "ChanId" -> chanId.id,
      "StartTime" -> startTime.toIsoFormat
    )
    internalUpdateRecordedWatchedStatus(params, watched)
  }

  def updateRecordedWatchedStatus(recordedId: RecordedId, watched: Boolean): ServiceResult[Boolean] = recordedId match {
    case RecordedIdInt(id) => internalUpdateRecordedWatchedStatus(Map("RecordedId" -> id), watched)
    case RecordedIdChanTime(chanId, startTime) => updateRecordedWatchedStatus(chanId, startTime, watched)
  }

  def getInputList: ServiceResult[List[Input]] = {
    for {
      response <- request("GetInputList")
      root     <- responseRoot(response, "InputList")
      result   <- Try(root.convertTo[List[Input]])
    } yield result
  }

  def getRecStorageGroupList: ServiceResult[List[String]] = {
    for {
      response <- request("GetRecStorageGroupList")
      root     <- responseRoot(response)
      result   <- Try(root.convertTo[List[String]])
    } yield result
  }

  def getPlayGroupList: ServiceResult[List[String]] = {
    for {
      response <- request("GetPlayGroupList")
      root     <- responseRoot(response)
      result   <- Try(root.convertTo[List[String]])
    } yield result
  }

  def getRecRuleFilterList: ServiceResult[List[RecRuleFilter]] = {
    for {
      response <- request("GetRecRuleFilterList")
      root     <- responseRoot(response, "RecRuleFilterList")
      result   <- Try(root.convertTo[MythJsonPagedObjectList[RecRuleFilter]].items)
    } yield result
  }

  def recStatusToString(recStatus: RecStatus): ServiceResult[String] = {
    val params: Map[String, Any] = Map("RecStatus" -> recStatus.id)
    for {
      response <- request("RecStatusToString", params)
      root     <- responseRoot(response, "String")
      result   <- Try(root.convertTo[String])
    } yield result
  }

  def recStatusToDescription(recStatus: RecStatus, recType: RecType, recStartTs: MythDateTime): ServiceResult[String] = {
    val params: Map[String, Any] = Map(
      "RecStatus" -> recStatus.id,
      "RecType"   -> recType.id,
      "StartTime" -> recStartTs.toIsoFormat
    )
    for {
      response <- request("RecStatusToDescription", params)
      root     <- responseRoot(response, "String")
      result   <- Try(root.convertTo[String])
    } yield result
  }


  def recTypeToString(recType: RecType): ServiceResult[String] = {
    val params: Map[String, Any] = Map("RecType" -> recType.id)
    for {
      response <- request("RecTypeToString", params)
      root     <- responseRoot(response, "String")
      result   <- Try(root.convertTo[String])
    } yield result
  }

  def recTypeToDescription(recType: RecType): ServiceResult[String] = {
    val params: Map[String, Any] = Map("RecType" -> recType.id)
    for {
      response <- request("RecTypeToDescription", params)
      root     <- responseRoot(response, "String")
      result   <- Try(root.convertTo[String])
    } yield result
  }

  // NB the DupMethodToXXXX service expects a *String* parameter, not an enum int id
  def dupMethodToString(dupMethod: DupCheckMethod): ServiceResult[String] = {
    val params: Map[String, Any] = Map(
      "DupMethod" -> DupCheckMethodJsonFormat.id2Description(dupMethod)
    )
    for {
      response <- request("DupMethodToString", params)
      root     <- responseRoot(response, "String")
      result   <- Try(root.convertTo[String])
    } yield result
  }

  // NB the DupMethodToXXXX service expects a *String* parameter, not an enum int id
  def dupMethodToDescription(dupMethod: DupCheckMethod): ServiceResult[String] = {
    val params: Map[String, Any] = Map(
      "DupMethod" -> DupCheckMethodJsonFormat.id2Description(dupMethod)
    )
    for {
      response <- request("DupMethodToDescription", params)
      root     <- responseRoot(response, "String")
      result   <- Try(root.convertTo[String])
    } yield result
  }

  // NB the DupInToXXXX service expects a *String* parameter, not an enum int id
  def dupInToString(dupIn: DupCheckIn): ServiceResult[String] = {
    val params: Map[String, Any] = Map(
      "DupIn" -> DupCheckInJsonFormat.id2Description(dupIn)
    )
    for {
      response <- request("DupInToString", params)
      root     <- responseRoot(response, "String")
      result   <- Try(root.convertTo[String])
    } yield result
  }

  // NB the DupInToXXXX service expects a *String* parameter, not an enum int id
  def dupInToDescription(dupIn: DupCheckIn): ServiceResult[String] = {
    val params: Map[String, Any] = Map(
      "DupIn" -> DupCheckInJsonFormat.id2Description(dupIn)
    )
    for {
      response <- request("DupInToDescription", params)
      root     <- responseRoot(response, "String")
      result   <- Try(root.convertTo[String])
    } yield result
  }
}
