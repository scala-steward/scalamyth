package mythtv
package connection
package http
package json

import scala.util.Try

import model._
import util.OptionalCount
import services.{ ChannelService, PagedList, ServiceResult }
import services.Service.ServiceFailure._
import RichJsonObject._

class JsonChannelService(conn: BackendJsonConnection)
  extends JsonBackendService(conn)
     with ChannelService {

  def getChannelInfo(chanId: ChanId): ServiceResult[ChannelDetails] = {
    val params: Map[String, Any] = Map("ChanID" -> chanId.id)
    for {
      response <- request("GetChannelInfo", params)
      root     <- responseRoot(response, "ChannelInfo")
      result   <- Try(root.convertTo[ChannelDetails])
    } yield result
  }

  // TODO simulate onlyVisible for older versions? also startIndex and count? Also, older versions have unsorted results...
  def getChannelInfoList(
    sourceId: ListingSourceId,
    startIndex: Int,
    count: OptionalCount[Int],
    onlyVisible: Boolean
  ): ServiceResult[PagedList[Channel]] = {
    var params = buildStartCountParams(startIndex, count) + ("SourceID" -> sourceId.id)
    if (onlyVisible) params += "OnlyVisible" -> onlyVisible
    for {
      response <- request("GetChannelInfoList", params)
      root     <- responseRoot(response, "ChannelInfoList")
      result   <- Try(root.convertTo[MythServicesPagedList[Channel]])
    } yield result
  }

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
    if (onlyVisible) params += "OnlyVisible" -> onlyVisible
    for {
      response <- request("GetChannelInfoList", params)
      root     <- responseRoot(response, "ChannelInfoList")
      result   <- Try(root.convertTo[MythServicesPagedList[ChannelDetails]])
    } yield result
  }

  def getVideoSource(sourceId: ListingSourceId): ServiceResult[ListingSource] = {
    val params: Map[String, Any] = Map("SourceID" -> sourceId.id)
    val srcTry = for {
      response <- request("GetVideoSource", params)
      root     <- responseRoot(response, "VideoSource")
      result   <- Try(root.convertTo[ListingSource])
    } yield result
    if (srcTry.isSuccess && srcTry.get.sourceId.id == 0) Left(ServiceNoResult)
    else srcTry
  }

  def getVideoSourceList: ServiceResult[List[ListingSource]] = {
    for {
      response <- request("GetVideoSourceList")
      root     <- responseRoot(response, "VideoSourceList")
      result   <- Try(root.convertTo[MythServicesObjectList[ListingSource]])
    } yield result.data
  }

  def getVideoMultiplex(mplexId: MultiplexId): ServiceResult[VideoMultiplex] = {
    val params: Map[String, Any] = Map("MplexID" -> mplexId.id)
    val mplexTry = for {
      response <- request("GetVideoMultiplex", params)
      root     <- responseRoot(response, "VideoMultiplex")
      result   <- Try(root.convertTo[VideoMultiplex])
    } yield result
    if (mplexTry.isSuccess && mplexTry.get.mplexId.id == 0) Left(ServiceNoResult)
    else mplexTry
  }

  def getVideoMultiplexList(sourceId: ListingSourceId, startIndex: Int, count: OptionalCount[Int]
  ): ServiceResult[PagedList[VideoMultiplex]] = {
    val params = buildStartCountParams(startIndex, count) + ("SourceID" -> sourceId.id)
    for {
      response <- request("GetVideoMultiplexList", params)
      root     <- responseRoot(response, "VideoMultiplexList")
      result   <- Try(root.convertTo[MythServicesPagedList[VideoMultiplex]])
    } yield result
  }

  def getXmltvIdList(sourceId: ListingSourceId): ServiceResult[List[String]] = {
    val params: Map[String, Any] = Map("SourceID" -> sourceId.id)
    for {
      response <- request("GetXMLTVIdList", params)
      root     <- responseRoot(response)
      result   <- Try(root.convertTo[List[String]])
    } yield result
  }

  def getDDLineupList(userName: String, password: String, provider: String = ""): ServiceResult[List[Lineup]] = {
    var params: Map[String, Any] = Map("UserId" -> userName, "Password" -> password)
    if (provider.nonEmpty) params += "Source" -> provider
    for {
      response <- post("GetDDLineupList", params)
      root     <- responseRoot(response, "LineupList")
      result   <- Try(root.convertTo[List[Lineup]])
    } yield result
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
    for {
      response <- post("AddDBChannel", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def removeDbChannel(chanId: ChanId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("ChannelId" -> chanId.id)
    for {
      response <- post("RemoveDBChannel", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
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
    for {
      response <- post("UpdateDBChannel", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
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
    val idTry = for {
      response <- post("AddVideoSource", params)
      root     <- responseRoot(response)
      result   <- Try(ListingSourceId(root.intField("int")))
    } yield result
    if (idTry.isSuccess && idTry.get.id < 0) Left(ServiceFailureUnknown)
    else idTry
  }

  def removeVideoSource(sourceId: ListingSourceId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("SourceID" -> sourceId.id)
    for {
      response <- post("RemoveVideoSource", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
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
    for {
      response <- post("UpdateVideoSource", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def fetchChannelsFromSource(
    sourceId: ListingSourceId,
    cardId: CaptureCardId,
    waitForFinish: Boolean
  ): ServiceResult[Int] = {
    var params: Map[String, Any] = Map("SourceId" -> sourceId.id, "CardId" -> cardId.id)
    if (waitForFinish) params += "WaitForFinish" -> waitForFinish
    for {
      response <- post("FetchChannelsFromSource", params)
      root     <- responseRoot(response)
      result   <- Try(root.intField("int"))
    } yield result
  }
}
