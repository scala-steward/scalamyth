package mythtv

import java.time.Duration

import model._
import connection.myth.{ Event, EventConnection, EventLock, MythProtocolAPIConnection }
import util.{ ByteCount, ExpectedCountIterator, MythDateTime, MythFileHash }

// TODO fix up all the .right.get non-error-handling (both in here and other files)

class MythBackend(val host: String) extends Backend with BackendOperations {
  def this(bi: BackendInfo) = this(bi.host)

  private[this] val conn = MythProtocolAPIConnection(host)
  @volatile private[this] var eventConnMayBeNull: EventConnection = _

  conn.announce("Monitor")

  def close(): Unit = {
    if (eventConnMayBeNull ne null) eventConnMayBeNull.close()
    conn.close()
  }

  private def eventConnection: EventConnection = synchronized {
    if (eventConnMayBeNull eq null) eventConnMayBeNull = EventConnection(host)
    eventConnMayBeNull
  }

  def recording(chanId: ChanId, startTime: MythDateTime): Recording = {
    conn.queryRecording(chanId, startTime).right.get
  }

  def deleteRecording(rec: Recording, force: Boolean): Boolean = {
    val status =
      if (force) conn.forceDeleteRecording(rec).right.get
      else conn.deleteRecording(rec).right.get
    status == 0
  }

  def forgetRecording(rec: Recording): Boolean = {
    conn.forgetRecording(rec).right.get
  }

  def stopRecording(rec: Recording): Option[CaptureCardId] = {
    conn.stopRecording(rec).toOption
  }

  def reschedule(): Unit = reschedule(None, wait = false)
  def reschedule(wait: Boolean): Unit = reschedule(None, wait)
  def reschedule(recordId: RecordRuleId, wait: Boolean = false): Unit = reschedule(None, wait)

  private def reschedule(recordId: Option[RecordRuleId], wait: Boolean): Unit = {
    val lock =
      if (wait) EventLock(eventConnection, _ == Event.ScheduleChangeEvent)
      else EventLock.empty
    if (recordId.isEmpty) conn.rescheduleRecordingsCheck(programId = "**any**")
    else conn.rescheduleRecordingsMatch(recordId = recordId.get)
    lock.await()
  }

  def isRecording(cardId: CaptureCardId): Boolean = conn.queryRecorderIsRecording(cardId).right.get

  def recordingsIterator: ExpectedCountIterator[Recording] = conn.queryRecordings("Ascending").right.get
  def recordings: List[Recording] = recordingsIterator.toList

  def expiringRecordingsIterator: ExpectedCountIterator[Recording] = conn.queryGetExpiring.right.get
  def expiringRecordings: List[Recording] = expiringRecordingsIterator.toList

  def pendingRecordingsIterator: ExpectedCountIterator[Recordable] = conn.queryGetAllPending.right.get
  def pendingRecordings: List[Recordable] = pendingRecordingsIterator.toList

  def upcomingRecordingsIterator: Iterator[Recordable] = {
    pendingRecordingsIterator filter (_.recStatus == RecStatus.WillRecord)
  }
  def upcomingRecordings: List[Recordable] = upcomingRecordingsIterator.toList

  def conflictingRecordingsIterator: Iterator[Recordable] = {
    pendingRecordingsIterator filter (_.recStatus == RecStatus.Conflict)
  }
  def conflictingRecordings: List[Recordable] = conflictingRecordingsIterator.toList

  // capture cards

  def availableRecorders: List[CaptureCardId] = conn.getFreeRecorderList.right.get

  //////

  def freeSpaceSummary: (ByteCount, ByteCount) = conn.queryFreeSpaceSummary.right.get

  def freeSpace: List[FreeSpace] = conn.queryFreeSpace.right.get

  def freeSpaceCombined: List[FreeSpace] = conn.queryFreeSpaceList.right.get

  def fileHash(fileName: String, storageGroup: String, hostName: String): MythFileHash =
    conn.queryFileHash(fileName, storageGroup, hostName).right.get

  def fileExists(fileName: String, storageGroup: String): Boolean =
    conn.queryFileExists(fileName, storageGroup).isRight

  def uptime: Duration = conn.queryUptime.right.get
  def loadAverages: (Double, Double, Double) = conn.queryLoad.right.get

  def isActiveBackend(hostname: String): Boolean = conn.queryIsActiveBackend(hostname).right.get

  def isActive: Boolean = isActiveBackend(host)

  def guideDataThrough: MythDateTime = conn.queryGuideDataThrough.right.get

  def scanVideos(): Map[String, Set[VideoId]] = {
    import connection.myth.Event.{ VideoListChangeEvent, VideoListNoChangeEvent }

    if (conn.scanVideos().right.get) {
      val lock = EventLock(eventConnection, {
        case _: VideoListChangeEvent => true
        case VideoListNoChangeEvent => true
        case _ => false
      })
      lock.await()

      lock.event.get match {
        case VideoListChangeEvent(changeMap) => changeMap
        case VideoListNoChangeEvent => Map.empty
        case _ => Map.empty  // unexpected event
      }
    } else {
      Map.empty
    }
  }
}
