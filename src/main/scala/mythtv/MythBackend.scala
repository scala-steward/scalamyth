package mythtv

import java.time.{ Duration, LocalDateTime }

import model._
import connection.myth.{ BackendConnection, MythProtocol, BackendResponse }
import connection.myth.data._
import util.{ ByteCount, ExpectedCountIterator, MythDateTime }

class MythBackend(val host: String) extends Backend with BackendOperations {
  import MythBackend._

  private[this] val conn = new BackendConnection(host, DEFAULT_PORT)

  def close() = {
    conn.disconnect()
  }

  def recording(chanId: Int, startTime: MythDateTime): Recording = {
    val cmd = s"QUERY_RECORDING TIMESLOT $chanId ${startTime.mythformat}"
    val res = conn.sendCommand(cmd).get.split
    new BackendProgram(res drop 1)  // first item is flag of some sort?
    // TODO returns "ERROR" if there was a problem...
  }

  def recordingsIterator: ExpectedCountIterator[Recording] = {
    val recs = conn.sendCommand("QUERY_RECORDINGS Ascending").get.split
    val fieldCount = BackendProgram.FIELD_ORDER.length

    val expectedCount = recs(0).toInt
    val it = recs.iterator drop 1 grouped fieldCount withPartial false
    new ExpectedCountIterator(expectedCount, it map (new BackendProgram(_)))
  }

  def recordings: List[Recording] = recordingsIterator.toList

  def expiringRecordingsIterator: ExpectedCountIterator[Recording] = {
    val recs = conn.sendCommand("QUERY_GETEXPIRING").get.split
    val fieldCount = BackendProgram.FIELD_ORDER.length

    val expectedCount = recs(0).toInt
    val it = recs.iterator drop 1 grouped fieldCount withPartial false
    new ExpectedCountIterator(expectedCount, it map (new BackendProgram(_)))
  }

  def expiringRecordings: List[Recording] = expiringRecordingsIterator.toList

  def pendingRecordingsIterator: ExpectedCountIterator[Recordable] = {
    val pending = conn.sendCommand("QUERY_GETALLPENDING").get.split
    val fieldCount = BackendProgram.FIELD_ORDER.length

    // first two items are header items (? and expected item count)
    val expectedCount = pending(1).toInt
    val it = pending.iterator drop 2 grouped fieldCount withPartial false
    new ExpectedCountIterator(expectedCount, it map (new BackendProgram(_)))
  }

  def pendingRecordings: List[Recordable] = pendingRecordingsIterator.toList

  def scheduledRecordingsIterator: ExpectedCountIterator[Recordable] = {
    val sched = conn.sendCommand("QUERY_GETALLSCHEDULED").get.split
    val fieldCount = BackendProgram.FIELD_ORDER.length

    val expectedCount = sched(0).toInt
    val it = sched.iterator drop 1 grouped fieldCount withPartial false
    new ExpectedCountIterator(expectedCount, it map (new BackendProgram(_)))
  }

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

  def availableRecorders: List[Int] = {
    val rec = conn.sendCommand("GET_FREE_RECORDER_LIST").get.split
    (rec map (_.toInt)).toList
  }

  //////

  def freeSpaceSummary: (ByteCount, ByteCount) = {
    val res = conn.sendCommand("QUERY_FREE_SPACE_SUMMARY").get.split
    val Array(total, used) = res map (_.toLong)
    (ByteCount(total * 1024), ByteCount(used * 1024))
  }

  def freeSpace: List[FreeSpace] = {
    val fs = conn.sendCommand("QUERY_FREE_SPACE").get.split
    val fieldCount = BackendFreeSpace.FIELD_ORDER.length
    val it = fs.iterator grouped fieldCount withPartial false map (new BackendFreeSpace(_))
    it.toList
  }

  def freeSpaceCombined: List[FreeSpace] = {
    val fs = conn.sendCommand("QUERY_FREE_SPACE_LIST").get.split
    val fieldCount = BackendFreeSpace.FIELD_ORDER.length
    val it = fs.iterator grouped fieldCount withPartial false map (new BackendFreeSpace(_))
    it.toList
  }

  def uptime: Duration = {
    val res = conn.sendCommand("QUERY_UPTIME").get.raw
    Duration.ofSeconds(res.toLong)
  }

  def loadAverages: List[Double] = {
    val res = conn.sendCommand("QUERY_LOAD").get.split
    (res map (_.toDouble)).toList
  }

  def isActiveBackend(hostname: String): Boolean = {
    val cmd = List("QUERY_IS_ACTIVE_BACKEND", hostname) mkString MythProtocol.BACKEND_SEP
    conn.sendCommand(cmd).getOrElse(BackendResponse("FALSE")).raw.toBoolean
  }

  def isActive: Boolean = isActiveBackend(host)   // TODO does this only work in master backends?
}

object MythBackend {
  final val DEFAULT_PORT: Int = 6543
}
