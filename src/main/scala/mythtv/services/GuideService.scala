package mythtv
package services

import model.EnumTypes.RecSearchType
import model.{ ChanId, Channel, ChannelGroup, ChannelGroupId, Guide, Program }
import util.{ OptionalCount, MythDateTime }
import connection.http.HttpStreamResponse

trait GuideService extends BackendService {
  def serviceName: String = "Guide"

  // TODO separate getProgramGuide methods for details=True/False (with different return types)
  // for MythTV 0.28, this gains a ChannelGroupId parameter, and loses numChannels and startChanId
  def getProgramGuide(
    startTime: MythDateTime,
    endTime: MythDateTime,
    startChanId: ChanId = ChanId(0),
    numChannels: OptionalCount[Int] = OptionalCount.all,
    channelGroupId: ChannelGroupId = ChannelGroupId(0),
    details: Boolean = false
  ): ServiceResult[Guide[Channel, Program]]

  def getProgramDetails(chanId: ChanId, startTime: MythDateTime): ServiceResult[Program]

  // TODO separate getProgramList methods for details=True/False (with different return types)
  // getProgramList is new for MythTV 0.28
  def getProgramList(
    startTime: MythDateTime,
    endTime: MythDateTime,
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all,
    details: Boolean = false,
    /* filters */
    chanId: ChanId = ChanId(0),
    title: String = "",
    category: String = "",
    person: String = "",
    keyword: String = "",
    onlyNew: Boolean = false,
    /* ordering */
    sortBy: String = "",    // one of { starttime (default), title, channel, duration }
    descending: Boolean = false
  ): ServiceResult[PagedList[Program]]

  def getChannelIcon[U](chanId: ChanId, width: Int = 0, height: Int = 0)(f: (HttpStreamResponse) => U): ServiceResult[Unit]

  // getCategoryList is new for MythTV 0.28
  def getCategoryList: ServiceResult[List[String]]

  // getChannelGroupList is new for MythTV 0.28
  def getChannelGroupList: ServiceResult[List[ChannelGroup]] = getChannelGroupList(false)
  def getChannelGroupList(includeEmpty: Boolean): ServiceResult[List[ChannelGroup]]

  // getStoredSearches is new for MythTV 0.28
  def getStoredSearches(searchType: RecSearchType): ServiceResult[List[String]]
}
