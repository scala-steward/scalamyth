package mythtv
package services

import model.{ CaptureCardId, ChanId, ChannelDetails, ListingSource, ListingSourceId, MultiplexId, VideoMultiplex }
import util.OptionalCount

trait ChannelService extends BackendService {
  def serviceName: String = "Channel"

  def getChannelInfo(chanId: ChanId): ChannelDetails
  def getChannelInfoList(sourceId: ListingSourceId): PagedList[ChannelDetails]

  def getVideoSource(sourceId: ListingSourceId): ListingSource
  def getVideoSourceList: List[ListingSource]

  def getVideoMultiplex(mplexId: MultiplexId): VideoMultiplex
  def getVideoMultiplexList(
    sourceId: ListingSourceId,
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all
  ): PagedList[VideoMultiplex]

  def getXmltvIdList(sourceId: ListingSourceId): List[String]

  /* mutating POST methods */

  def addDbChannel(channel: ChannelDetails): Boolean

  def removeDbChannel(chanId: ChanId): Boolean

  def updateDbChannel(channel: ChannelDetails): Boolean


  def addVideoSource(source: ListingSource): ListingSourceId

  def removeVideoSource(sourceId: ListingSourceId): Boolean

  def updateVideoSource(source: ListingSource): Boolean


  //def getDDLineupList(...): ???

  def fetchChannelsFromSource(sourceId: ListingSourceId, cardId: CaptureCardId, waitForFinish: Boolean): Int
}
