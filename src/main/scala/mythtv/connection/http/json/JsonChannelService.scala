package mythtv
package connection
package http
package json

import model._
import util.OptionalCount
import services.{ ChannelService, PagedList }
import RichJsonObject._

class JsonChannelService(conn: BackendJsonConnection)
  extends JsonBackendService(conn)
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

  def getDDLineupList(userName: String, password: String, provider: String = ""): List[Lineup] = {
    var params: Map[String, Any] = Map("UserId" -> userName, "Password" -> password)
    if (provider.nonEmpty) params += "Source" -> provider
    val response = post("GetDDLineupList", params)
    val root = responseRoot(response, "LineupList")
    root.convertTo[List[Lineup]]
  }

  /* mutating POST methods */

  def addDbChannel(channel: ChannelDetails): Boolean = {
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
    val response = post("AddDBChannel", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  def removeDbChannel(chanId: ChanId): Boolean = {
    val params: Map[String, Any] = Map("ChannelId" -> chanId.id)
    val response = post("RemoveDBChannel", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  def updateDbChannel(channel: ChannelDetails): Boolean = {
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
    val response = post("UpdateDBChannel", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }


  def addVideoSource(source: ListingSource): ListingSourceId = {
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
    val response = post("AddVideoSource", params)
    ListingSourceId(0)  // TODO
  }

  def removeVideoSource(sourceId: ListingSourceId): Boolean = {
    val params: Map[String, Any] = Map("SourceID" -> sourceId.id)
    val response = post("RemoveVideoSource", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  def updateVideoSource(source: ListingSource): Boolean = {
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
    val response = post("UpdateVideoSource", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  def fetchChannelsFromSource(
    sourceId: ListingSourceId,
    cardId: CaptureCardId,
    waitForFinish: Boolean
  ): Int = {
    var params: Map[String, Any] = Map("SourceId" -> sourceId.id, "CardId" -> cardId.id)
    if (waitForFinish) params += "WaitForFinish" -> waitForFinish
    val response = post("FetchChannelsFromSource", params)
    0   // TODO
  }

}
