package mythtv
package services

import model.{ ChanId, ChannelDetails, ListingSource, ListingSourceId, MultiplexId, VideoMultiplex }
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

  /* TODO unimplemented methods
  def updateDbChannel(...): Boolean
  def addDbChannel(...): Boolean
  def removeDbChannel(chanId: ChanId): Boolean
  def updateVideoSource(...): Boolean
  def addVideoSource(...): Int
  def removeVideoSource(...): Boolean
  def getDdLineupList(...): ???
  def fetchChannelsFromSource(sourceId: ListingSourceId, cardId: CaptureCardId, waitForFinish: Boolean) = ???
   */
}
