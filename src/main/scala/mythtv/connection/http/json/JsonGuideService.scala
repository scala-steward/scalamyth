package mythtv
package connection
package http
package json

import scala.util.Try

import model.EnumTypes.RecSearchType
import services.{ GuideService, PagedList, ServiceResult, ServicesObject }
import model.{ ChanId, Channel, ChannelDetails, ChannelGroup, ChannelGroupId, Guide, Program, ProgramBrief }
import util.{ MythDateTime, OptionalCount, OptionalCountSome }

class JsonGuideService(conn: BackendJsonConnection)
  extends JsonBackendService(conn)
     with GuideService {

  def getProgramGuide(
    startTime: MythDateTime,
    endTime: MythDateTime,
    startChanId: ChanId,
    numChannels: OptionalCount[Int],
    channelGroupId: ChannelGroupId
  ): ServiceResult[Guide[Channel, ProgramBrief]] = {
    // There seems to be an off-by-one error on my 0.27 backend implementation of NumChannels
    // NumChannels=1 returns 1 channels, NumChannels={n|n>1} returns n-1 channels
    // FIXME UPSTREAM: this is because it ignores channel visibility when computing start/end chanid
    var params: Map[String, Any] = Map("StartTime" -> startTime.toIsoFormat, "EndTime" -> endTime.toIsoFormat)
    if (channelGroupId.id != 0) params += "ChannelGroupId" -> channelGroupId.id
    if (startChanId.id != 0)    params += "StartChanId" -> startChanId.id
    numChannels match {
      case OptionalCountSome(n) => params += "NumChannels" -> n
      case _ => ()
    }
    for {
      response <- request("GetProgramGuide", params)
      root     <- responseRoot(response, "ProgramGuide")
      result   <- Try(root.convertTo[ServicesObject[Guide[Channel, ProgramBrief]]])
    } yield result.data
  }

  def getProgramGuideDetails(
    startTime: MythDateTime,
    endTime: MythDateTime,
    startChanId: ChanId = ChanId.empty,
    numChannels: OptionalCount[Int] = OptionalCount.all,
    channelGroupId: ChannelGroupId = ChannelGroupId(0)
  ): ServiceResult[Guide[ChannelDetails, Program]] = {
    var params: Map[String, Any] = Map(
      "StartTime" -> startTime.toIsoFormat,
      "EndTime" -> endTime.toIsoFormat,
      "Details" -> true
    )
    if (channelGroupId.id != 0) params += "ChannelGroupId" -> channelGroupId.id
    if (startChanId.id != 0)    params += "StartChanId" -> startChanId.id
    numChannels match {
      case OptionalCountSome(n) => params += "NumChannels" -> n
      case _ => ()
    }
    for {
      response <- request("GetProgramGuide", params)
      root     <- responseRoot(response, "ProgramGuide")
      result   <- Try(root.convertTo[ServicesObject[Guide[ChannelDetails, Program]]])
    } yield result.data
  }

  /*
   * Note: for a program currently recording, the StartTS field inside the Recording
   * object will contain the actual time the recording started (which may not be the
   * same as the program start time). However, once the recording is completed, this
   * field reverts to containing the actual start time of the program, and thus is
   * unsuitable for looking up the actual recording in all cases.  TODO example
   */
  def getProgramDetails(chanId: ChanId, startTime: MythDateTime): ServiceResult[Program] = {
    val params: Map[String, Any] = Map("StartTime" -> startTime.toIsoFormat, "ChanId" -> chanId.id)
    for {
      response <- request("GetProgramDetails", params)
      root     <- responseRoot(response, "Program")
      result   <- Try(root.convertTo[Program])
    } yield result
  }

  def getProgramList(
    startTime: MythDateTime,
    endTime: MythDateTime,
    startIndex: Int,
    count: OptionalCount[Int],
    chanId: ChanId,
    title: String,
    category: String,
    person: String,
    keyword: String,
    onlyNew: Boolean,
    sortBy: String,
    descending: Boolean
  ): ServiceResult[PagedList[ProgramBrief]] = {
    var params = buildStartCountParams(startIndex, count) ++ Map(
      "StartTime" -> startTime.toIsoFormat,
      "EndTime"   -> endTime.toIsoFormat
    )
    if (chanId.id != 0)    params += "ChanId"         -> chanId.id
    if (title.nonEmpty)    params += "TitleFilter"    -> title
    if (category.nonEmpty) params += "CategoryFilter" -> category
    if (person.nonEmpty)   params += "PersonFilter"   -> person
    if (keyword.nonEmpty)  params += "KeywordFilter"  -> keyword
    if (onlyNew)           params += "OnlyNew"        -> onlyNew
    if (sortBy.nonEmpty)   params += "Sort"           -> sortBy
    if (descending)        params += "Descending"     -> descending
    for {
      response <- request("GetProgramList", params)
      root     <- responseRoot(response, "ProgramList")
      result   <- Try(root.convertTo[MythServicesPagedList[ProgramBrief]])
    } yield result
  }

  def getProgramListDetails(
    startTime: MythDateTime,
    endTime: MythDateTime,
    startIndex: Int,
    count: OptionalCount[Int],
    chanId: ChanId,
    title: String,
    category: String,
    person: String,
    keyword: String,
    onlyNew: Boolean,
    sortBy: String,
    descending: Boolean
  ): ServiceResult[PagedList[Program]] = {
    var params = buildStartCountParams(startIndex, count) ++ Map(
      "StartTime" -> startTime.toIsoFormat,
      "EndTime"   -> endTime.toIsoFormat,
      "Details"   -> true
    )
    if (chanId.id != 0)    params += "ChanId"         -> chanId.id
    if (title.nonEmpty)    params += "TitleFilter"    -> title
    if (category.nonEmpty) params += "CategoryFilter" -> category
    if (person.nonEmpty)   params += "PersonFilter"   -> person
    if (keyword.nonEmpty)  params += "KeywordFilter"  -> keyword
    if (onlyNew)           params += "OnlyNew"        -> onlyNew
    if (sortBy.nonEmpty)   params += "Sort"           -> sortBy
    if (descending)        params += "Descending"     -> descending
    for {
      response <- request("GetProgramList", params)
      root     <- responseRoot(response, "ProgramList")
      result   <- Try(root.convertTo[MythServicesPagedList[Program]])
    } yield result
  }

  def getChannelIcon[U](chanId: ChanId, width: Int, height: Int)(f: (HttpStreamResponse) => U): ServiceResult[Unit] = {
    var params: Map[String, Any] = Map("ChanId" -> chanId.id)
    if (width != 0)  params += "Width" -> width
    if (height != 0) params += "Height" -> height

    Try {
      val response = requestStream("GetChannelIcon", params)
      streamResponse(response, f)
    }
  }

  def getCategoryList: ServiceResult[List[String]] = {
    for {
      response <- request("GetCategoryList")
      root     <- responseRoot(response, "StringList")
      result   <- Try(root.convertTo[List[String]])
    } yield result
  }

  def getChannelGroupList(includeEmpty: Boolean): ServiceResult[List[ChannelGroup]] = {
    var params: Map[String, Any] = Map.empty
    if (includeEmpty) params += "IncludeEmpty" -> includeEmpty
    for {
      response <- request("GetChannelGroupList", params)
      root     <- responseRoot(response, "ChannelGroupList", "ChannelGroups")
      result   <- Try(root.convertTo[List[ChannelGroup]])
    } yield result
  }

  def getStoredSearches(searchType: RecSearchType): ServiceResult[List[String]] = {
    val params: Map[String, Any] = Map("Type" -> RecSearchTypeJsonFormat.id2Description(searchType))
    for {
      response <- request("GetStoredSearches", params)
      root     <- responseRoot(response, "StringList")
      result   <- Try(root.convertTo[List[String]])
    } yield result
  }
}
