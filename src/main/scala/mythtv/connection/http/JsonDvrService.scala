package mythtv
package connection
package http

import model._
import util.{ MythDateTime, OptionalCount }
import services.{ DvrService, PagedList }
import RichJsonObject._

class JsonDvrService(conn: BackendJsonConnection)
  extends JsonBackendService(conn)
     with DvrService {
  // TODO catch when we get bogus data back and don't return an object?
  /* NB the 'ProgramFlags' field that we get back from GetRecorded seems to be wacky, whereas the same
     field for the same program in GetRecordedList seems to be correct. For an example, see the recording
     Martha Bakes: Bake it Dark, where flags are 0x2ff0f004 vs 0x1004

     We see the same discrepancy in the MythProtocol API queryRecorings() vs queryRecording()

     The root cause for this discrepancy seems to be that in ProgramInfo::LoadProgramFromRecorded,
     the programflags field is not initialized, and the ProgramInfo variable is allocated on the stack
     at the call site. Compare this to the implementation of LoadFromRecorded */
  def getRecorded(chanId: ChanId, startTime: MythDateTime): Recording = {
    val params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    val response = request("GetRecorded", params)
    val root = responseRoot(response, "Program")
    root.convertTo[Recording]
  }

  def getRecordedList(startIndex: Int, count: OptionalCount[Int], descending: Boolean): PagedList[Recording] = {
    var params = buildStartCountParams(startIndex, count)
    if (descending) params += "Descending" -> descending
    val response = request("GetRecordedList", params)
    val root = responseRoot(response, "ProgramList")
    root.convertTo[MythJsonPagedObjectList[Recording]]
  }

  def getExpiringList(startIndex: Int, count: OptionalCount[Int]): PagedList[Recording] = {
    val params = buildStartCountParams(startIndex, count)
    val response = request("GetExpiringList", params)
    val root = responseRoot(response, "ProgramList")
    root.convertTo[MythJsonPagedObjectList[Recording]]
  }

  def getUpcomingList(startIndex: Int, count: OptionalCount[Int], showAll: Boolean): PagedList[Recordable] = {
    var params = buildStartCountParams(startIndex, count)
    if (showAll) params += "ShowAll" -> showAll
    val response = request("GetUpcomingList", params)
    val root = responseRoot(response, "ProgramList")
    root.convertTo[MythJsonPagedObjectList[Recordable]]
  }

  def getConflictList(startIndex: Int, count: OptionalCount[Int]): PagedList[Recordable] = {
    val params = buildStartCountParams(startIndex, count)
    val response = request("GetConflictList", params)
    val root = responseRoot(response, "ProgramList")
    root.convertTo[MythJsonPagedObjectList[Recordable]]
  }

  def getEncoderList: List[RemoteEncoderState] = {
    val response = request("GetEncoderList")
    val root = responseRoot(response, "EncoderList")
    root.convertTo[List[RemoteEncoderState]]
  }

  def getRecordScheduleList(startIndex: Int, count: OptionalCount[Int]): PagedList[RecordRule] = {
    val params = buildStartCountParams(startIndex, count)
    val response = request("GetRecordScheduleList", params)
    val root = responseRoot(response, "RecRuleList")
    root.convertTo[MythJsonPagedObjectList[RecordRule]]
  }

  def getRecordSchedule(recordId: RecordRuleId): RecordRule = {
    val params: Map[String, Any] = Map("RecordId" -> recordId.id)
    val response = request("GetRecordSchedule", params)
    val root = responseRoot(response, "RecRule")
    root.convertTo[RecordRule]
  }

  def getRecGroupList: List[String] = {
    val response = request("GetRecGroupList")
    val root = responseRoot(response)
    root.convertTo[List[String]]
  }

  def getTitleList: List[String] = {
    val response = request("GetTitleList")
    val root = responseRoot(response)
    root.convertTo[List[String]]
  }

  def getTitleInfoList: List[TitleInfo] = ???

}
