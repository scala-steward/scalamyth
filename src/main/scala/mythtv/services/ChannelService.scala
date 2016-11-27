package mythtv
package services

import model.{ CaptureCardId, ChanId, ChannelDetails, Lineup,
  ListingSource, ListingSourceId, MultiplexId, VideoMultiplex }
import util.OptionalCount

trait ChannelService extends BackendService {
  def serviceName: String = "Channel"

  def getChannelInfo(chanId: ChanId): ServiceResult[ChannelDetails]
  def getChannelInfoList(sourceId: ListingSourceId): ServiceResult[PagedList[ChannelDetails]]

  def getVideoSource(sourceId: ListingSourceId): ServiceResult[ListingSource]
  def getVideoSourceList: ServiceResult[List[ListingSource]]

  def getVideoMultiplex(mplexId: MultiplexId): ServiceResult[VideoMultiplex]
  def getVideoMultiplexList(
    sourceId: ListingSourceId,
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all
  ): ServiceResult[PagedList[VideoMultiplex]]

  def getXmltvIdList(sourceId: ListingSourceId): ServiceResult[List[String]]

  /* mutating POST methods */

  def addDbChannel(channel: ChannelDetails): ServiceResult[Boolean]

  def removeDbChannel(chanId: ChanId): ServiceResult[Boolean]

  def updateDbChannel(channel: ChannelDetails): ServiceResult[Boolean]


  def addVideoSource(source: ListingSource): ServiceResult[ListingSourceId]

  def removeVideoSource(sourceId: ListingSourceId): ServiceResult[Boolean]

  def updateVideoSource(source: ListingSource): ServiceResult[Boolean]


  def getDDLineupList(userName: String, password: String, provider: String = ""): ServiceResult[List[Lineup]]

  def fetchChannelsFromSource(sourceId: ListingSourceId, cardId: CaptureCardId, waitForFinish: Boolean): ServiceResult[Int]
}
