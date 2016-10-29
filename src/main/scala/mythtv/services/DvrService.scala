package mythtv
package services

import model.{ ChanId, Recordable, Recording, RecordRule, RecordRuleId, RemoteEncoderState, TitleInfo }
import util.{ MythDateTime, OptionalCount }

trait DvrService extends BackendService {
  def serviceName: String = "Dvr"

  def getRecorded(chanId: ChanId, startTime: MythDateTime): Recording

  def getRecordedList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all,
    descending: Boolean = false
  ): PagedList[Recording]

  def getExpiringList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all
  ): PagedList[Recording]

  def getUpcomingList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all,
    showAll: Boolean = false
  ): PagedList[Recordable]

  def getConflictList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all
  ): PagedList[Recordable]

  def getEncoderList: List[RemoteEncoderState]

  def getRecordScheduleList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all
  ): PagedList[RecordRule]

  def getRecordSchedule(recordId: RecordRuleId): RecordRule

  def getRecGroupList: List[String]

  def getTitleList: List[String]

  def getTitleInfoList: List[TitleInfo]
}
