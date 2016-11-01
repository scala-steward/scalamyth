package mythtv
package connection
package http

import model._
import util.OptionalCount
import services.{ ChannelService, PagedList }

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
}
