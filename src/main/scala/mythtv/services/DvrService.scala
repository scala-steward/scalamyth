package mythtv
package services

import model.{ ChanId, Recordable, Recording, RecordRule, RecordRuleId, RemoteEncoderState, TitleInfo }
import util.{ MythDateTime, OptionalCount }

trait DvrService extends BackendService {
  def serviceName: String = "Dvr"

  def getRecorded(chanId: ChanId, startTime: MythDateTime): ServiceResult[Recording]

  def getRecordedList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all,
    descending: Boolean = false
  ): ServiceResult[PagedList[Recording]]

  def getExpiringList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all
  ): ServiceResult[PagedList[Recording]]

  def getUpcomingList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all,
    showAll: Boolean = false
  ): ServiceResult[PagedList[Recordable]]

  def getConflictList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all
  ): ServiceResult[PagedList[Recordable]]

  def getEncoderList: ServiceResult[List[RemoteEncoderState]]

  def getRecordScheduleList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all
  ): ServiceResult[PagedList[RecordRule]]

  def getRecordSchedule(recordId: RecordRuleId): ServiceResult[RecordRule]

  def getRecGroupList: ServiceResult[List[String]]

  def getTitleList: ServiceResult[List[String]]

  def getTitleInfoList: ServiceResult[List[TitleInfo]]

  /* mutating POST methods */

  def removeRecorded(chanId: ChanId, startTime: MythDateTime): ServiceResult[Boolean]

  def addRecordSchedule(rule: RecordRule): ServiceResult[RecordRuleId]

  def updateRecordSchedule(rule: RecordRule): ServiceResult[Boolean]

  def removeRecordSchedule(recordId: RecordRuleId): ServiceResult[Boolean]

  def disableRecordSchedule(recordId: RecordRuleId): ServiceResult[Boolean]

  def enableRecordSchedule(recordId: RecordRuleId): ServiceResult[Boolean]

  /* Added to API on 6 Apr 2016 */
  def updateRecordedWatchedStatus(chanId: ChanId, startTime: MythDateTime, watched: Boolean): ServiceResult[Boolean]
}
