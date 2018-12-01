// SPDX-License-Identifier: LGPL-2.1-only
/*
 * AbstractChannelService.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package http

import model._
import services.{ ChannelService, PagedList, ServiceResult, ServicesObject }
import services.Service.ServiceFailure.{ ServiceFailureUnknown, ServiceNoResult }
import util.OptionalCount

trait AbstractChannelService extends ServiceProtocol with ChannelService {

  def getChannelInfo(chanId: ChanId): ServiceResult[ChannelDetails] = {
    val params: Map[String, Any] = Map("ChanID" -> chanId.id)
    val chanTry = request[ChannelDetails]("GetChannelInfo", params)("ChannelInfo")
    if (chanTry.isSuccess && chanTry.get.chanId.id == 0) Left(ServiceNoResult)
    else chanTry
  }

  // The onlyVisible parameter is simulated for MythTV version < 0.28.
  // Also, older versions have arbitrarily sorted results...
  def getChannelInfoList(
    sourceId: ListingSourceId,
    startIndex: Int,
    count: OptionalCount[Int],
    onlyVisible: Boolean
  ): ServiceResult[PagedList[Channel]] = {
    var params = buildStartCountParams(startIndex, count) + ("SourceID" -> sourceId.id)
    var needsVisibleFilter = false

    if (onlyVisible) {
      if (endpoints("GetChannelInfoList").parameters contains "OnlyVisible")
        params += "OnlyVisible" -> onlyVisible
      else needsVisibleFilter = true
    }

    if (needsVisibleFilter) {
      val results = request[PagedList[ChannelDetails]]("GetChannelInfoList", params)("ChannelInfoList")
      results map (_ filter (_.visible))
    }
    else request("GetChannelInfoList", params)("ChannelInfoList")
  }

  // The onlyVisible parameter is simulated for MythTV version < 0.28.
  // Also, older versions have arbitrarily sorted results...
  def getChannelInfoDetailsList(
    sourceId: ListingSourceId,
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all,
    onlyVisible: Boolean = false
  ): ServiceResult[PagedList[ChannelDetails]] = {
    var params = buildStartCountParams(startIndex, count) ++ Map(
      "SourceID" -> sourceId.id,
      "Details"  -> true
    )
    var needsVisibleFilter = false

    if (onlyVisible) {
      if (endpoints("GetChannelInfoList").parameters contains "OnlyVisible")
        params += "OnlyVisible" -> onlyVisible
      else needsVisibleFilter = true
    }

    val results = request[PagedList[ChannelDetails]]("GetChannelInfoList", params)("ChannelInfoList")
    if (needsVisibleFilter) results map (_ filter (_.visible)) else results
  }

  def getVideoSource(sourceId: ListingSourceId): ServiceResult[ListingSource] = {
    val params: Map[String, Any] = Map("SourceID" -> sourceId.id)
    val srcTry = request[ListingSource]("GetVideoSource", params)("VideoSource")
    if (srcTry.isSuccess && srcTry.get.sourceId.id == 0) Left(ServiceNoResult)
    else srcTry
  }

  def getVideoSourceList: ServiceResult[List[ListingSource]] = {
    val r = request[ServicesObject[List[ListingSource]]]("GetVideoSourceList")("VideoSourceList")
    r map (_.data)
  }

  def getVideoMultiplex(mplexId: MultiplexId): ServiceResult[VideoMultiplex] = {
    val params: Map[String, Any] = Map("MplexID" -> mplexId.id)
    val mplexTry = request[VideoMultiplex]("GetVideoMultiplex", params)()
    if (mplexTry.isSuccess && mplexTry.get.mplexId.id == 0) Left(ServiceNoResult)
    else mplexTry
  }

  def getVideoMultiplexList(sourceId: ListingSourceId, startIndex: Int, count: OptionalCount[Int]
  ): ServiceResult[PagedList[VideoMultiplex]] = {
    val params = buildStartCountParams(startIndex, count) + ("SourceID" -> sourceId.id)
    request("GetVideoMultiplexList", params)("VideoMultiplexList")
  }

  def getXmltvIdList(sourceId: ListingSourceId): ServiceResult[List[String]] = {
    val params: Map[String, Any] = Map("SourceID" -> sourceId.id)
    request("GetXMLTVIdList", params)()
  }

  def getDDLineupList(userName: String, password: String, provider: String = ""): ServiceResult[List[Lineup]] = {
    var params: Map[String, Any] = Map("UserId" -> userName, "Password" -> password)
    if (provider.nonEmpty) params += "Source" -> provider
    post("GetDDLineupList", params)("LineupList", "Lineups")
  }

  /* mutating POST methods */

  def addDbChannel(channel: ChannelDetails): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "MplexID"          -> channel.mplexId.map(_.id).getOrElse(0),
      "SourceID"         -> channel.sourceId.id,
      "ChannelID"        -> channel.chanId.id,
      "CallSign"         -> channel.callsign,
      "ChannelName"      -> channel.name,
      "ChannelNumber"    -> channel.number.num,
      "ServiceID"        -> channel.serviceId.getOrElse(0),
      "ATSCMajorChannel" -> channel.atscMajorChan.getOrElse(0),
      "ATSCMinorChannel" -> channel.atscMinorChan.getOrElse(0),
      "UseEIT"           -> channel.useOnAirGuide,
      "visible"          -> channel.visible,
      "FrequencyID"      -> channel.freqId.getOrElse(""),
      "Icon"             -> channel.iconPath,
      "Format"           -> channel.format,
      "XMLTVID"          -> channel.xmltvId,
      "DefaultAuthority" -> channel.defaultAuthority.getOrElse("")
    )
    post("AddDBChannel", params)()
  }

  def removeDbChannel(chanId: ChanId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("ChannelId" -> chanId.id)
    post("RemoveDBChannel", params)()
  }


  def updateDbChannel(channel: ChannelDetails): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "MplexID"          -> channel.mplexId.map(_.id).getOrElse(0),
      "SourceID"         -> channel.sourceId.id,
      "ChannelID"        -> channel.chanId.id,
      "CallSign"         -> channel.callsign,
      "ChannelName"      -> channel.name,
      "ChannelNumber"    -> channel.number.num,
      "ServiceID"        -> channel.serviceId.getOrElse(0),
      "ATSCMajorChannel" -> channel.atscMajorChan.getOrElse(0),
      "ATSCMinorChannel" -> channel.atscMinorChan.getOrElse(0),
      "UseEIT"           -> channel.useOnAirGuide,
      "visible"          -> channel.visible,
      "FrequencyID"      -> channel.freqId.getOrElse(""),
      "Icon"             -> channel.iconPath,
      "Format"           -> channel.format,
      "XMLTVID"          -> channel.xmltvId,
      "DefaultAuthority" -> channel.defaultAuthority.getOrElse("")
    )
    post("UpdateDBChannel", params)()
  }

  def addVideoSource(source: ListingSource): ServiceResult[ListingSourceId] = {
    val params: Map[String, Any] = Map(
      "SourceName" -> source.name,
      "Grabber"    -> source.grabber.getOrElse(""),
      "UserId"     -> source.userId.getOrElse(""),
      "FreqTable"  -> source.freqTable,
      "LineupId"   -> source.lineupId.getOrElse(""),
      "Password"   -> source.password.getOrElse(""),
      "UseEIT"     -> source.useEit,
      "ConfigPath" -> source.configPath.getOrElse(""),
      "NITId"      -> source.dvbNitId.getOrElse(-1)
    )
    // returns -1 on error/key already exists?
    val idTry = post[ListingSourceId]("AddVideoSource", params)()
    if (idTry.isSuccess && idTry.get.id < 0) Left(ServiceFailureUnknown)
    else idTry
  }

  def removeVideoSource(sourceId: ListingSourceId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("SourceID" -> sourceId.id)
    post("RemoveVideoSource", params)()
  }


  def updateVideoSource(source: ListingSource): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "SourceID"   -> source.sourceId.id,
      "SourceName" -> source.name,
      "Grabber"    -> source.grabber.getOrElse(""),
      "UserId"     -> source.userId.getOrElse(""),
      "FreqTable"  -> source.freqTable,
      "LineupId"   -> source.lineupId.getOrElse(""),
      "Password"   -> source.password.getOrElse(""),
      "UseEIT"     -> source.useEit,
      "ConfigPath" -> source.configPath.getOrElse(""),
      "NITId"      -> source.dvbNitId.getOrElse(-1)
    )
    post("UpdateVideoSource", params)()
  }

  def fetchChannelsFromSource(
    sourceId: ListingSourceId,
    cardId: CaptureCardId,
    waitForFinish: Boolean
  ): ServiceResult[Int] = {
    var params: Map[String, Any] = Map("SourceId" -> sourceId.id, "CardId" -> cardId.id)
    if (waitForFinish) params += "WaitForFinish" -> waitForFinish
    post("FetchChannelsFromSource", params)()
  }
}
