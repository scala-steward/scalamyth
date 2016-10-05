package mythtv

import java.time.{ Duration, LocalDateTime }

import model._
import connection.myth.BackendAPIConnection
import connection.myth.data.BackendProgram  // TODO eliminate importing from connection.myth.data...
import util.{ ByteCount, ExpectedCountIterator, MythDateTime }

class MythBackend(val host: String) extends Backend with BackendOperations {
  import MythBackend._

  private[this] val conn = new BackendAPIConnection(host, DEFAULT_PORT)

  def close() = {
    conn.disconnect()
  }

  def recording(chanId: ChanId, startTime: MythDateTime): Recording = {
    conn.queryRecording(chanId, startTime)
  }

  def recordingsIterator: ExpectedCountIterator[Recording] = {
    val recs = conn.sendCommand("QUERY_RECORDINGS Ascending").get.split
    val fieldCount = BackendProgram.FIELD_ORDER.length

    val expectedCount = recs(0).toInt
    val it = recs.iterator drop 1 grouped fieldCount withPartial false
    new ExpectedCountIterator(expectedCount, it map (new BackendProgram(_)))
  }

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
}

object MythBackend {
  final val DEFAULT_PORT: Int = 6543
}
