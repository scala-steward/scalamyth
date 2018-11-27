package mythtv
package services

import model.EnumTypes.RecSearchType
import model.{ ChanId, Channel, ChannelDetails, ChannelGroup, ChannelGroupId, Guide, Program, ProgramBrief }
import util.{ OptionalCount, MythDateTime }
import connection.http.HttpStreamResponse

trait GuideService extends BackendService {
  final def serviceName: String = "Guide"

  // for MythTV 0.28, this gains a ChannelGroupId parameter, and loses numChannels and startChanId
  def getProgramGuide(
    startTime: MythDateTime,
    endTime: MythDateTime,
    startChanId: ChanId = ChanId.empty,
    numChannels: OptionalCount[Int] = OptionalCount.all,
    channelGroupId: ChannelGroupId = ChannelGroupId(0)
  ): ServiceResult[Guide[Channel, ProgramBrief]]

  // NB this fills out ChannelDetails with dummy data until MythTV 0.28
  def getProgramGuideDetails(
    startTime: MythDateTime,
    endTime: MythDateTime,
    startChanId: ChanId = ChanId.empty,
    numChannels: OptionalCount[Int] = OptionalCount.all,
    channelGroupId: ChannelGroupId = ChannelGroupId(0)
  ): ServiceResult[Guide[ChannelDetails, Program]]

  def getProgramDetails(chanId: ChanId, startTime: MythDateTime): ServiceResult[Program]

  // getProgramList is new for MythTV 0.28
  def getProgramList(
    startTime: MythDateTime,
    endTime: MythDateTime,
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all,
    /* filters */
    chanId: ChanId = ChanId.empty,
    title: String = "",
    category: String = "",
    person: String = "",
    keyword: String = "",
    onlyNew: Boolean = false,
    /* ordering */
    sortBy: String = "",    // one of { starttime (default), title, channel, duration }
    descending: Boolean = false
  ): ServiceResult[PagedList[ProgramBrief]]

  def getProgramListDetails(
    startTime: MythDateTime,
    endTime: MythDateTime,
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all,
    /* filters */
    chanId: ChanId = ChanId.empty,
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

  // addToChannelGroup and removeFromChannelGroup are new for MythTV 29
  def addToChannelGroup(channelGroupId: ChannelGroupId, chanId: ChanId): ServiceResult[Boolean]
  def removeFromChannelGroup(channelGroupId: ChannelGroupId, chanId: ChanId): ServiceResult[Boolean]
}
