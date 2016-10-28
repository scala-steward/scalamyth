package mythtv
package services

import model.{ ChanId, Program, RecordRule, RecordRuleId, RemoteEncoderState, TitleInfo }
import util.{ MythDateTime, OptionalCount }

trait DvrService extends BackendService {
  def serviceName: String = "Dvr"

  def getRecorded(chanId: ChanId, startTime: MythDateTime): Program

  def getRecordedList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all,
    descending: Boolean = false
  ): PagedList[Program]

  def getExpiringList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all
  ): PagedList[Program]

  def getUpcomingList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all,
    showAll: Boolean = false
  ): PagedList[Program]

  def getConflictList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all
  ): PagedList[Program]

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
