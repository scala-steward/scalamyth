package mythtv
package connection
package http

import java.time.LocalTime
import java.time.format.DateTimeFormatter

import scala.util.Try

import model._
import services.{ DvrService, PagedList, ServiceResult }
import services.Service.ServiceFailure.ServiceNoResult
import util.{ MythDateTime, OptionalCount }
import EnumTypes.{ DupCheckIn, DupCheckMethod, Markup, RecStatus, RecType }
import RecordedId.{ RecordedIdChanTime, RecordedIdInt }

trait AbstractDvrService extends ServiceProtocol with DvrService {
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
    val recTry = request[Recording]("GetRecorded", params)("Program")
    if (recTry.isSuccess && recTry.get.isDummy) Left(ServiceNoResult)
    else recTry
  }

  def getRecorded(recordedId: RecordedId): ServiceResult[Recording] = recordedId match {
    case RecordedIdInt(id) =>
      val params: Map[String, Any] = Map("RecordedId" -> id)
      val recTry = request[Recording]("GetRecorded", params)("Program")
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
    request("GetRecordedList", params)("ProgramList")
  }

  def recordedIdForPathname(pathName: String): ServiceResult[RecordedId] = {
    val params: Map[String, Any] = Map("Pathname" -> pathName)
    request("RecordedIdForPathname", params)()
  }

  def getExpiringList(startIndex: Int, count: OptionalCount[Int]): ServiceResult[PagedList[Recording]] = {
    val params = buildStartCountParams(startIndex, count)
    request("GetExpiringList", params)("ProgramList")
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
    request("GetUpcomingList", params)("ProgramList")
  }

  def getConflictList(
    startIndex: Int,
    count: OptionalCount[Int],
    recordRuleId: RecordRuleId
  ): ServiceResult[PagedList[Recordable]] = {
    var params = buildStartCountParams(startIndex, count)
    if (recordRuleId.id != 0) params += "RecordId" -> recordRuleId.id
    request("GetConflictList", params)("ProgramList")
  }

  def getEncoderList: ServiceResult[List[RemoteEncoderState]] = {
    request("GetEncoderList")("EncoderList", "Encoders")
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
    request("GetRecordScheduleList", params)("RecRuleList")
  }

  def getRecordSchedule(recordId: RecordRuleId): ServiceResult[RecordRule] = {
    val params: Map[String, Any] = Map("RecordId" -> recordId.id)
    request("GetRecordSchedule", params)("RecRule")
  }

  def getRecordSchedule(recordedId: RecordedId): ServiceResult[RecordRule] = recordedId match {
    case RecordedIdInt(id) =>
      val params: Map[String, Any] = Map("RecordedId" -> id)
      request("GetRecordSchedule", params)("RecRule")
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
    request("GetRecordSchedule", params)("RecRule")
  }

  def getRecordSchedule(chanId: ChanId, startTime: MythDateTime, makeOverride: Boolean): ServiceResult[RecordRule] = {
    var params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    if (makeOverride) params += "MakeOverride" -> makeOverride
    request("GetRecordSchedule", params)("RecRule")
  }

  def getRecGroupList: ServiceResult[List[String]] = {
    request("GetRecGroupList")("StringList")
  }

  def getTitleList(recGroup: String): ServiceResult[List[String]] = {
    var params: Map[String, Any] = Map.empty
    if (recGroup.nonEmpty) params += "RecGroup" -> recGroup
    request("GetTitleList", params)()
  }

  def getTitleInfoList: ServiceResult[List[TitleInfo]] = {
    request("GetTitleInfoList")("TitleInfoList", "TitleInfos")
  }

  def getSavedBookmark(recordedId: RecordedId): ServiceResult[VideoPositionFrame] = recordedId match {
    case RecordedIdChanTime(chanId, startTime) => getSavedBookmark(chanId, startTime)
    case RecordedIdInt(id) =>
      val params: Map[String, Any] = Map("RecordedId" -> id)
      request("GetSavedBookmark", params)()
  }

  def getSavedBookmark(chanId: ChanId, startTime: MythDateTime): ServiceResult[VideoPositionFrame] = {
    val params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    request("GetSavedBookmark", params)()
  }

  def getSavedBookmarkMilliseconds(recordedId: RecordedId): ServiceResult[VideoPositionMilliseconds] = recordedId match {
    case RecordedIdChanTime(chanId, startTime) => getSavedBookmarkMilliseconds(chanId, startTime)
    case RecordedIdInt(id) =>
      val params: Map[String, Any] = Map("RecordedId" -> id, "OffsetType" -> "duration")
      request("GetSavedBookmark", params)()
  }

  def getSavedBookmarkMilliseconds(chanId: ChanId, startTime: MythDateTime): ServiceResult[VideoPositionMilliseconds] = {
    val params: Map[String, Any] = Map(
      "ChanId" -> chanId.id,
      "StartTime" -> startTime.toIsoFormat,
      "OffsetType" -> "duration"
    )
    request("GetSavedBookmark", params)()
  }

  def getSavedBookmarkByteOffset(recordedId: RecordedId): ServiceResult[VideoPositionBytes] = recordedId match {
    case RecordedIdChanTime(chanId, startTime) => getSavedBookmarkByteOffset(chanId, startTime)
    case RecordedIdInt(id) =>
      val params: Map[String, Any] = Map("RecordedId" -> id, "OffsetType" -> "position")
      request("GetSavedBookmark", params)()
  }

  def getSavedBookmarkByteOffset(chanId: ChanId, startTime: MythDateTime): ServiceResult[VideoPositionBytes] = {
    val params: Map[String, Any] = Map(
      "ChanId" -> chanId.id,
      "StartTime" -> startTime.toIsoFormat,
      "OffsetType" -> "position"
    )
    request("GetSavedBookmark", params)()
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
    post("RemoveRecorded", params)()
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
    post("DeleteRecording", params)()
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
    post("UnDeleteRecording", params)()
  }

  def undeleteRecording(recordedId: RecordedId): ServiceResult[Boolean] = recordedId match {
    case RecordedIdInt(id) => post("UnDeleteRecording", Map("RecordedId" -> id))("bool")
    case RecordedIdChanTime(chanId, startTime) => undeleteRecording(chanId, startTime)
  }

  // Verify that the RecordedId of this Recording is of type RecordedIdInt
  private def validRecordedId(rec: Recording): ServiceResult[RecordedId] = Try {
    rec.recordedId match {
      case RecordedIdInt(_) => rec.recordedId
      case RecordedIdChanTime(_, _) =>
        throw new IllegalArgumentException("recording does not have a valid integer recordedId")
    }
  }

  def stopRecording(recordedId: RecordedId): ServiceResult[Boolean] = recordedId match {
    case RecordedIdInt(id) => post("StopRecording", Map("RecordedId" -> id))()
    case RecordedIdChanTime(chanId, startTime) =>
      for {
        rec <- getRecorded(chanId, startTime) // TODO does this work for an in progress recording?
        recordedId <- validRecordedId(rec)
        result <- stopRecording(recordedId)
      } yield result
  }

  def reactivateRecording(recordedId: RecordedId): ServiceResult[Boolean] = recordedId match {
    case RecordedIdInt(id) => post("ReactivateRecording", Map("RecordedId" -> id))()
    case RecordedIdChanTime(chanId, startTime) =>
      for {
        rec <- getRecorded(chanId, startTime)
        recordedId <- validRecordedId(rec)
        result <- reactivateRecording(recordedId)
      } yield result
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
      "Type"           -> RecType.description(rule.recType),
      "SearchType"     -> RecSearchType.description(rule.searchType),
      "RecPriority"    -> rule.recPriority,
      "PreferredInput" -> rule.preferredInput.map(_.id).getOrElse(0),
      "StartOffset"    -> rule.startOffset,
      "EndOffset"      -> rule.endOffset,
      "DupMethod"      -> DupCheckMethod.description(rule.dupMethod),
      "DupIn"          -> DupCheckIn.description(rule.dupIn),
      "Filter"         -> rule.filters.getOrElse(0),
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
    post("AddRecordSchedule", params)()
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
      "Type"           -> RecType.description(rule.recType),
      "SearchType"     -> RecSearchType.description(rule.searchType),
      "RecPriority"    -> rule.recPriority,
      "PreferredInput" -> rule.preferredInput.map(_.id).getOrElse(0),
      "StartOffset"    -> rule.startOffset,
      "EndOffset"      -> rule.endOffset,
      "DupMethod"      -> DupCheckMethod.description(rule.dupMethod),
      "DupIn"          -> DupCheckIn.description(rule.dupIn),
      "Filter"         -> rule.filters.getOrElse(0),
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
    post("UpdateRecordSchedule", params)()
  }

  def removeRecordSchedule(recordId: RecordRuleId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("RecordId" -> recordId.id)
    post("RemoveRecordSchedule", params)()
  }

  def disableRecordSchedule(recordId: RecordRuleId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("RecordId" -> recordId.id)
    post("DisableRecordSchedule", params)()
  }

  def enableRecordSchedule(recordId: RecordRuleId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("RecordId" -> recordId.id)
    post("EnableRecordSchedule", params)()
  }

  def addDontRecordSchedule(chanId: ChanId, startTime: MythDateTime, neverRecord: Boolean): ServiceResult[Boolean] = {
    var params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    if (neverRecord) params += "NeverRecord" -> neverRecord
    post("AddDontRecordSchedule", params)()
  }

  def rescheduleRecordings(): ServiceResult[Boolean] = {
    post("RescheduleRecordings")()
  }

  private def internalUpdateRecordedWatchedStatus(partialParams: Map[String, Any], watched: Boolean): ServiceResult[Boolean] = {
    val params: Map[String, Any] = partialParams + ("Watched" -> watched)
    post("UpdateRecordedWatchedStatus", params)()
  }

  def updateRecordedWatchedStatus(chanId: ChanId, startTime: MythDateTime, watched: Boolean): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    internalUpdateRecordedWatchedStatus(params, watched)
  }

  def updateRecordedWatchedStatus(recordedId: RecordedId, watched: Boolean): ServiceResult[Boolean] = recordedId match {
    case RecordedIdInt(id) => internalUpdateRecordedWatchedStatus(Map("RecordedId" -> id), watched)
    case RecordedIdChanTime(chanId, startTime) => updateRecordedWatchedStatus(chanId, startTime, watched)
  }

  private def internalSetSavedBookmark(partialParams: Map[String, Any], pos: VideoPosition): ServiceResult[Boolean] = {
    val (offset, offsetType) = pos match {
      case VideoPositionFrame(f) => (f, "")
      case VideoPositionBytes(b) => (b, "position")
      case VideoPositionSeconds(s) => (s * 1000, "duration")
      case VideoPositionMilliseconds(ms) => (ms, "duration")
    }
    val params = partialParams ++ Map(
      "OffsetType" -> offsetType,
      "Offset" -> offset
    )
    post("SetSavedBookmark", params)()
  }

  def setSavedBookmark(chanId: ChanId, startTime: MythDateTime, pos: VideoPosition): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    internalSetSavedBookmark(params, pos)
  }

  def setSavedBookmark(recordedId: RecordedId, pos: VideoPosition): ServiceResult[Boolean] = recordedId match {
    case RecordedIdInt(id) => internalSetSavedBookmark(Map("RecordedId" -> id), pos)
    case RecordedIdChanTime(chanId, startTime) => setSavedBookmark(chanId, startTime, pos)
  }

  def getInputList: ServiceResult[List[Input]] = {
    request("GetInputList")("InputList", "Inputs")
  }

  def getRecStorageGroupList: ServiceResult[List[String]] = {
    request("GetRecStorageGroupList")()
  }

  def getPlayGroupList: ServiceResult[List[String]] = {
    request("GetPlayGroupList")()
  }

  def getRecRuleFilterList: ServiceResult[List[RecRuleFilterItem]] = {
    val pl = request[PagedList[RecRuleFilterItem]]("GetRecRuleFilterList")("RecRuleFilterList")
    pl map (_.items)
  }

  def recStatusToString(recStatus: RecStatus): ServiceResult[String] = {
    val params: Map[String, Any] = Map("RecStatus" -> recStatus.id)
    request("RecStatusToString", params)()
  }

  def recStatusToDescription(recStatus: RecStatus, recType: RecType, recStartTs: MythDateTime): ServiceResult[String] = {
    val params: Map[String, Any] = Map(
      "RecStatus" -> recStatus.id,
      "RecType"   -> recType.id,
      "StartTime" -> recStartTs.toIsoFormat
    )
    request("RecStatusToDescription", params)()
  }

  def recTypeToString(recType: RecType): ServiceResult[String] = {
    val params: Map[String, Any] = Map("RecType" -> recType.id)
    request("RecTypeToString", params)()
  }

  def recTypeToDescription(recType: RecType): ServiceResult[String] = {
    val params: Map[String, Any] = Map("RecType" -> recType.id)
    request("RecTypeToDescription", params)()
  }

  // NB the DupMethodToXXXX service expects a *String* parameter, not an enum int id
  def dupMethodToString(dupMethod: DupCheckMethod): ServiceResult[String] = {
    val params: Map[String, Any] = Map("DupMethod" -> DupCheckMethod.description(dupMethod))
    request("DupMethodToString", params)()
  }

  // NB the DupMethodToXXXX service expects a *String* parameter, not an enum int id
  def dupMethodToDescription(dupMethod: DupCheckMethod): ServiceResult[String] = {
    val params: Map[String, Any] = Map("DupMethod" -> DupCheckMethod.description(dupMethod))
    request("DupMethodToDescription", params)()
  }

  // NB the DupInToXXXX service expects a *String* parameter, not an enum int id
  def dupInToString(dupIn: DupCheckIn): ServiceResult[String] = {
    val params: Map[String, Any] = Map("DupIn" -> DupCheckIn.description(dupIn))
    request("DupInToString", params)()
  }

  // NB the DupInToXXXX service expects a *String* parameter, not an enum int id
  def dupInToDescription(dupIn: DupCheckIn): ServiceResult[String] = {
    val params: Map[String, Any] = Map("DupIn" -> DupCheckIn.description(dupIn))
    request("DupInToDescription", params)()
  }

  private def videoSegmentsFromRecordedMarkup[VP <: VideoPosition](
    marks: List[RecordedMarkup[VP]], markStart: Markup, markEnd: Markup): ServiceResult[List[VideoSegment[VP]]] = Try {
    // TODO improved diagnostic if there are not an even number of marks, currently we capture for example:
    //    scala.MatchError: List(<RecordedMarkup CommStart VideoPositionFrame(xxx)>)

    val segments = marks grouped 2 map {
      case Seq(startMark: RecordedMarkup[VP], endMark: RecordedMarkup[VP]) =>
        assert(startMark.tag == markStart, s"start mark is ${startMark.tag}, expected $markStart")
        assert(endMark.tag == markEnd, s"end mark is ${endMark.tag}, expected $markEnd")
        new VideoSegment[VP] {
          def start = startMark.position
          def end = endMark.position
        }
    }
    segments.toList
  }

  private def internalGetRecordedCutList(recordedId: RecordedId)(endpoint: String): ServiceResult[List[RecordedMarkupFrame]] = recordedId match {
    case RecordedIdChanTime(chanId, startTime) => internalGetRecordedCutList(chanId, startTime)(endpoint)
    case RecordedIdInt(id) =>
      val params: Map[String, Any] = Map("RecordedId" -> id)
      request(endpoint, params)("CutList", "Cuttings")
  }

  private def internalGetRecordedCutList(chanId: ChanId, startTime: MythDateTime)(endpoint: String): ServiceResult[List[RecordedMarkupFrame]] = {
    val params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    request(endpoint, params)("CutList", "Cuttings")
  }

  private def internalGetRecordedCutListMs(recordedId: RecordedId)(endpoint: String): ServiceResult[List[RecordedMarkupMilliseconds]] = recordedId match {
    case RecordedIdChanTime(chanId, startTime) => internalGetRecordedCutListMs(chanId, startTime)(endpoint)
    case RecordedIdInt(id) =>
      val params: Map[String, Any] = Map("RecordedId" -> id, "OffsetType" -> "Duration")
      request(endpoint, params)("CutList", "Cuttings")
  }

  private def internalGetRecordedCutListMs(chanId: ChanId, startTime: MythDateTime)(endpoint: String): ServiceResult[List[RecordedMarkupMilliseconds]] = {
    val params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat, "OffsetType" -> "Duration")
    request(endpoint, params)("CutList", "Cuttings")
  }

  private def internalGetRecordedCutListBytes(recordedId: RecordedId)(endpoint: String): ServiceResult[List[RecordedMarkupBytes]] = recordedId match {
    case RecordedIdChanTime(chanId, startTime) => internalGetRecordedCutListBytes(chanId, startTime)(endpoint)
    case RecordedIdInt(id) =>
      val params: Map[String, Any] = Map("RecordedId" -> id, "OffsetType" -> "Position")
      request(endpoint, params)("CutList", "Cuttings")
  }

  private def internalGetRecordedCutListBytes(chanId: ChanId, startTime: MythDateTime)(endpoint: String): ServiceResult[List[RecordedMarkupBytes]] = {
    val params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat, "OffsetType" -> "Position")
    request(endpoint, params)("CutList", "Cuttings")
  }

  // ---

  def getRecordedCommBreak(recordedId: RecordedId): ServiceResult[List[VideoSegmentFrames]] = {
    for {
      marks <- internalGetRecordedCutList(recordedId)("GetRecordedCommBreak")
      segments <- videoSegmentsFromRecordedMarkup(marks, Markup.CommStart, Markup.CommEnd)
    } yield segments
  }

  def getRecordedCommBreak(chanId: ChanId, startTime: MythDateTime): ServiceResult[List[VideoSegmentFrames]] = {
    for {
      marks <- internalGetRecordedCutList(chanId, startTime)("GetRecordedCommBreak")
      segments <- videoSegmentsFromRecordedMarkup(marks, Markup.CommStart, Markup.CommEnd)
    } yield segments
  }

  def getRecordedCommBreakMs(recordedId: RecordedId): ServiceResult[List[VideoSegmentMilliseconds]] = {
    for {
      marks <- internalGetRecordedCutListMs(recordedId)("GetRecordedCommBreak")
      segments <- videoSegmentsFromRecordedMarkup(marks, Markup.CommStart, Markup.CommEnd)
    } yield segments
  }

  def getRecordedCommBreakMs(chanId: ChanId, startTime: MythDateTime): ServiceResult[List[VideoSegmentMilliseconds]] = {
    for {
      marks <- internalGetRecordedCutListMs(chanId, startTime)("GetRecordedCommBreak")
      segments <- videoSegmentsFromRecordedMarkup(marks, Markup.CommStart, Markup.CommEnd)
    } yield segments
  }

  def getRecordedCommBreakBytes(recordedId: RecordedId): ServiceResult[List[VideoSegmentBytes]] = {
    for {
      marks <- internalGetRecordedCutListBytes(recordedId)("GetRecordedCommBreak")
      segments <- videoSegmentsFromRecordedMarkup(marks, Markup.CommStart, Markup.CommEnd)
    } yield segments
  }

  def getRecordedCommBreakBytes(chanId: ChanId, startTime: MythDateTime): ServiceResult[List[VideoSegmentBytes]] = {
    for {
      marks <- internalGetRecordedCutListBytes(chanId, startTime)("GetRecordedCommBreak")
      segments <- videoSegmentsFromRecordedMarkup(marks, Markup.CommStart, Markup.CommEnd)
    } yield segments
  }

  // ---

  def getRecordedCutList(recordedId: RecordedId): ServiceResult[List[VideoSegmentFrames]] = {
    for {
      marks <- internalGetRecordedCutList(recordedId)("GetRecordedCutList")
      segments <- videoSegmentsFromRecordedMarkup(marks, Markup.CutStart, Markup.CutEnd)
    } yield segments
  }

  def getRecordedCutList(chanId: ChanId, startTime: MythDateTime): ServiceResult[List[VideoSegmentFrames]] = {
    for {
      marks <- internalGetRecordedCutList(chanId, startTime)("GetRecordedCutList")
      segments <- videoSegmentsFromRecordedMarkup(marks, Markup.CutStart, Markup.CutEnd)
    } yield segments
  }

  def getRecordedCutListMs(recordedId: RecordedId): ServiceResult[List[VideoSegmentMilliseconds]] = {
    for {
      marks <- internalGetRecordedCutListMs(recordedId)("GetRecordedCutList")
      segments <- videoSegmentsFromRecordedMarkup(marks, Markup.CutStart, Markup.CutEnd)
    } yield segments
  }

  def getRecordedCutListMs(chanId: ChanId, startTime: MythDateTime): ServiceResult[List[VideoSegmentMilliseconds]] = {
    for {
      marks <- internalGetRecordedCutListMs(chanId, startTime)("GetRecordedCutList")
      segments <- videoSegmentsFromRecordedMarkup(marks, Markup.CutStart, Markup.CutEnd)
    } yield segments
  }

  def getRecordedCutListBytes(recordedId: RecordedId): ServiceResult[List[VideoSegmentBytes]] = {
    for {
      marks <- internalGetRecordedCutListBytes(recordedId)("GetRecordedCutList")
      segments <- videoSegmentsFromRecordedMarkup(marks, Markup.CutStart, Markup.CutEnd)
    } yield segments
  }

  def getRecordedCutListBytes(chanId: ChanId, startTime: MythDateTime): ServiceResult[List[VideoSegmentBytes]] = {
    for {
      marks <- internalGetRecordedCutListBytes(chanId, startTime)("GetRecordedCutList")
      segments <- videoSegmentsFromRecordedMarkup(marks, Markup.CutStart, Markup.CutEnd)
    } yield segments
  }

  def getRecordedSeekBytes(recordedId: RecordedId): ServiceResult[List[RecordedSeekBytes]] = recordedId match {
    case RecordedIdInt(id) =>
      val params: Map[String, Any] = Map("RecordedId" -> id, "OffsetType" -> "BYTES")
      request("GetRecordedSeek", params)("CutList", "Cuttings")
    case RecordedIdChanTime(chanId, startTime) =>
      for {
        rec <- getRecorded(chanId, startTime)
        recordedId <- validRecordedId(rec)
        result <- getRecordedSeekBytes(recordedId)
      } yield result
  }

  def getRecordedSeekMs(recordedId: RecordedId): ServiceResult[List[RecordedSeekMilliseconds]] = recordedId match {
    case RecordedIdInt(id) =>
      val params: Map[String, Any] = Map("RecordedId" -> id, "OffsetType" -> "DURATION")
      request("GetRecordedSeek", params)("CutList", "Cuttings")
    case RecordedIdChanTime(chanId, startTime) =>
      for {
        rec <- getRecorded(chanId, startTime)
        recordedId <- validRecordedId(rec)
        result <- getRecordedSeekMs(recordedId)
      } yield result
  }

  def getOldRecordedList(
    title: String,
    seriesId: String,
    recordRuleId: RecordRuleId,
    startTime: MythDateTime,
    endTime: MythDateTime,
    startIndex: Int,
    count: OptionalCount[Int],
    sortBy: String,    // starttime is the default (title is other option)
    descending: Boolean
  ): ServiceResult[PagedList[Recording]] = {
    var params = buildStartCountParams(startIndex, count)
    if (title.nonEmpty)                  params += "Title"      -> title
    if (seriesId.nonEmpty)               params += "SeriesId"   -> seriesId
    if (recordRuleId.id != 0)            params += "RecordId"   -> recordRuleId.id
    if (startTime != MythDateTime.empty) params += "StartTime"  -> startTime.toIsoFormat
    if (endTime != MythDateTime.empty)   params += "EndTime"    -> endTime.toIsoFormat
    if (sortBy.nonEmpty)                 params += "Sort"       -> sortBy
    if (descending)                      params += "Descending" -> descending
    request("GetOldRecordedList", params)("ProgramList")
  }
}
