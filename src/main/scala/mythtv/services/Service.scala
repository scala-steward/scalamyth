package mythtv
package services

import model._
import util.{ OptionalCount, MythDateTime }

trait Service {
  def serviceName: String
}

trait BackendService extends Service
trait FrontendService extends Service

trait DataBytes  // TODO placeholder

// TODO use default arguments rather than overloaded methods where applicable

trait MythService extends BackendService {
  def serviceName: String = "Myth"

  def getHostName: String
  def getHosts: List[String]
  def getKeys: List[String]
  def getSetting(hostName: String, key: String = ""): Settings

  def getTimeZone: TimeZoneInfo
  def getStorageGroupDirs(hostName: String = "", groupName: String = ""): List[StorageGroupDir]
}

trait ChannelService extends BackendService {
  def serviceName: String = "Channel"

  def getChannelInfo(chanId: ChanId): Channel
  def getChannelInfoList: List[Channel]

  def getVideoSource(sourceId: ListingSourceId): ListingSource
  def getVideoSourceList: List[ListingSource]

  def getVideoMultiplex(mplexId: Int): VideoMultiplex
  def getVideoMultiplexList(sourceId: ListingSourceId): List[VideoMultiplex]

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

trait GuideService extends BackendService {
  def serviceName: String = "Guide"

  def getProgramGuide(
    startTime: MythDateTime,
    endTime: MythDateTime,
    startChanId: ChanId = ChanId(0),
    numChannels: OptionalCount[Int] = OptionalCount.all,
    details: Boolean = false
  ): Guide[Channel, Program]

  def getProgramDetails(chanId: ChanId, startTime: MythDateTime): Program

  def getChannelIcon(chanId: ChanId): DataBytes  // TODO optional width and height
}

trait DvrService extends BackendService {
  def serviceName: String = "Dvr"

  def getRecorded(chanId: ChanId, startTime: MythDateTime): Program

  def getRecordedList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all,
    descending: Boolean = false
  ): List[Program]

  def getExpiringList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all
  ): List[Program]

  def getUpcomingList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all,
    showAll: Boolean = false
  ): List[Program]

  def getConflictList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all
  ): List[Program]

  def getEncoderList: List[RemoteEncoderState]

  def getRecordScheduleList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all
  ): List[RecordRule]

  def getRecordSchedule(recordId: RecordRuleId): RecordRule

  def getRecGroupList: List[String]

  def getTitleList: List[String]

  def getTitleInfoList: List[TitleInfo]
}

trait VideoService extends BackendService {
  def serviceName: String = "Video"

  def getVideo(id: Int): Video
  def getVideoByFileName(fileName: String): Video
  def getVideoList: List[Video]
}

trait CaptureService extends BackendService {
  def serviceName: String = "Capture"

  def getCaptureCard(cardId: CaptureCardId): CaptureCard
  def getCaptureCardList(hostName: String = "", cardType: String = ""): List[CaptureCard]
}

trait ContentService extends BackendService {
  def serviceName: String = "Content"

  def getAlbumArt(id: Int): DataBytes  // TODO optional width, height
  def getFile(storageGroup: String, fileName: String): DataBytes
  def getFileList(storageGroup: String): List[String]
  def getHash(storageGroup: String, fileName: String): String
  def getImageFile(storageGroup: String, fileName: String): DataBytes  // optional width, height
  def getLiveStream(id: String): LiveStreamInfo  // TODO is id really a string or an Int?
  def getLiveStreamList: List[LiveStreamInfo]
  def getMusic(id: String): DataBytes  // TODO is id really a string or an int?
  def getPreviewImage(chanId: ChanId, startTime: MythDateTime): DataBytes // TODO optional params
  def getRecording(chanId: ChanId, startTime: MythDateTime): DataBytes
  def getVideo(id: Int): DataBytes
  // TODO more methods
}

/************************************/

trait MythFrontendService extends FrontendService {
  def serviceName: String = "Frontend"

  // query methods

  def getActionList: List[FrontendAction]   // the data here is really more like a map (action is a k/v tuple)
  def getContextList: List[String]
  def getStatus: FrontendStatus

  // action methods

  def playRecording(chanId: ChanId, startTime: MythDateTime): Boolean
  def playVideo(id: Int, useBookmark: Boolean = false): Boolean  // TODO use 0/1 instead of true/false ?
  def sendAction(action: String): Boolean // TODO optional params for SCREENSHOT; this method is controversial?
  def sendMessage(message: String): Boolean
  def sendNotification(message: String): Boolean  // TODO lots and lots of optional parameters (12) make a Notification class?
}
