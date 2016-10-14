package mythtv

import java.time.Duration

import model._
import connection.myth.{ BackendAPIConnection, EventConnection }
import util.{ ByteCount, ExpectedCountIterator, MythDateTime }

class MythBackend(val host: String) extends Backend with BackendOperations {
  private[this] val conn = BackendAPIConnection(host)
  @volatile private[this] var eventConnMayBeNull: EventConnection = _

  conn.announce("Monitor")

  def close() = {
    if (eventConnMayBeNull ne null) eventConnMayBeNull.disconnect()
    conn.disconnect()
  }

  private def eventConnection: EventConnection = synchronized {
    if (eventConnMayBeNull eq null) eventConnMayBeNull = EventConnection(host)
    eventConnMayBeNull
  }

  def recording(chanId: ChanId, startTime: MythDateTime): Recording = {
    conn.queryRecording(chanId, startTime)
  }

  def deleteRecording(rec: Recording, force: Boolean): Boolean = {
    val status =
      if (force) conn.forceDeleteRecording(rec)
      else conn.deleteRecording(rec)
    status == 0
  }

  def forgetRecording(rec: Recording): Boolean = {
    val status = conn.forgetRecording(rec)
    status == 0
  }

  def stopRecording(rec: Recording): Option[CaptureCardId] = {
    val status = conn.stopRecording(rec)
    if (status < 0) None
    else Some(CaptureCardId(status))
  }

  def recordingsIterator: ExpectedCountIterator[Recording] = conn.queryRecordings("Ascending")
  def recordings: List[Recording] = recordingsIterator.toList

  def expiringRecordingsIterator: ExpectedCountIterator[Recording] = conn.queryGetExpiring
  def expiringRecordings: List[Recording] = expiringRecordingsIterator.toList

  def pendingRecordingsIterator: ExpectedCountIterator[Recordable] = conn.queryGetAllPending
  def pendingRecordings: List[Recordable] = pendingRecordingsIterator.toList

  def scheduledRecordingsIterator: ExpectedCountIterator[Recordable] = conn.queryGetAllScheduled
  def scheduledRecordings: List[Recordable] = scheduledRecordingsIterator.toList

  def upcomingRecordingsIterator: Iterator[Recordable] = {
    pendingRecordingsIterator filter (_.recStatus == RecStatus.WillRecord)
  }
  def upcomingRecordings: List[Recordable] = upcomingRecordingsIterator.toList

  def conflictingRecordingsIterator: Iterator[Recordable] = {
    pendingRecordingsIterator filter (_.recStatus == RecStatus.Conflict)
  }
  def conflictingRecordings: List[Recordable] = conflictingRecordingsIterator.toList

  // capture cards

  def availableRecorders: List[CaptureCardId] = conn.getFreeRecorderList

  //////

  def freeSpaceSummary: (ByteCount, ByteCount) = conn.queryFreeSpaceSummary
  def freeSpace: List[FreeSpace] = conn.queryFreeSpace
  def freeSpaceCombined: List[FreeSpace] = conn.queryFreeSpaceList

  def uptime: Duration = conn.queryUptime
  def loadAverages: (Double, Double, Double) = conn.queryLoad

  def isActiveBackend(hostname: String): Boolean = conn.queryIsActiveBackend(hostname)
  def isActive: Boolean = isActiveBackend(host)   // TODO does this only work in master backends?

  def guideDataThrough: MythDateTime = conn.queryGuideDataThrough

}
