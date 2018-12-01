// SPDX-License-Identifier: LGPL-2.1-only
/*
 * AbstractGuideService.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package http

import scala.util.Try

import model._
import services.{ GuideService, PagedList, ServiceResult }
import util.{ MythDateTime, OptionalCount, OptionalCountSome }
import EnumTypes.RecSearchType

trait AbstractGuideService extends ServiceProtocol with GuideService {

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
    request("GetProgramGuide", params)("ProgramGuide")
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
    request("GetProgramGuide", params)("ProgramGuide")
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
    request("GetProgramDetails", params)("Program")
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
    request("GetProgramList", params)("ProgramList")
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
    request("GetProgramList", params)("ProgramList")
  }

  def getChannelIcon[U](chanId: ChanId, width: Int, height: Int)(f: HttpStreamResponse => U): ServiceResult[Unit] = {
    var params: Map[String, Any] = Map("ChanId" -> chanId.id)
    if (width != 0)  params += "Width" -> width
    if (height != 0) params += "Height" -> height
    Try {
      val response = requestStream("GetChannelIcon", params)
      streamResponse(response, f)
    }
  }

  def getCategoryList: ServiceResult[List[String]] = {
    request("GetCategoryList")()
  }

  def getChannelGroupList(includeEmpty: Boolean): ServiceResult[List[ChannelGroup]] = {
    var params: Map[String, Any] = Map.empty
    if (includeEmpty) params += "IncludeEmpty" -> includeEmpty
    request("GetChannelGroupList", params)("ChannelGroupList", "ChannelGroups")
  }

  def addToChannelGroup(channelGroupId: ChannelGroupId, chanId: ChanId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "ChannelGroupId" -> channelGroupId.id,
      "ChanId" -> chanId.id
    )
    post("AddToChannelGroup", params)()
  }

  def removeFromChannelGroup(channelGroupId: ChannelGroupId, chanId: ChanId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "ChannelGroupId" -> channelGroupId.id,
      "ChanId" -> chanId.id
    )
    post("RemoveFromChannelGroup", params)()
  }

  def getStoredSearches(searchType: RecSearchType): ServiceResult[List[String]] = {
    val params: Map[String, Any] = Map("Type" -> RecSearchType.description(searchType))
    request("GetStoredSearches", params)()
  }
}
