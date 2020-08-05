// SPDX-License-Identifier: LGPL-2.1-only
/*
 * MythBackend.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv

import java.time.Duration

import scala.annotation.nowarn

import model._
import connection.myth.{ Event, EventConnection, EventLock, MythProtocol, MythProtocolAPIConnection }
import util.{ ByteCount, ExpectedCountIterator, MythDateTime, MythFileHash }
import MythProtocol.MythProtocolFailure

class NoResultExceptionn extends RuntimeException("no results")
class APIFailureException(msg: String) extends RuntimeException(msg)

class MythBackend(val host: String) extends Backend with BackendOperations {
  def this(bi: BackendInfo) = this(bi.host)

  private[this] val conn = MythProtocolAPIConnection(host)
  @volatile private[this] var eventConnMayBeNull: EventConnection = _

  conn.announce("Monitor")

  import MythProtocolFailure._
  def fail(e: MythProtocolFailure) = e match {
    case MythProtocolNoResult => throw new NoResultExceptionn
    case MythProtocolFailureMessage(message) => throw new APIFailureException(message)
    case MythProtocolFailureUnknown => throw new APIFailureException("unknown failure from API call")
    case MythProtocolFailureThrowable(throwable) => throw throwable
  }

  def close(): Unit = {
    if (eventConnMayBeNull ne null) eventConnMayBeNull.close()
    conn.close()
  }

  private def eventConnection: EventConnection = synchronized {
    if (eventConnMayBeNull eq null) eventConnMayBeNull = EventConnection(host)
    eventConnMayBeNull
  }

  def recording(chanId: ChanId, startTime: MythDateTime): Recording = {
    conn.queryRecording(chanId, startTime) match {
      case Right(rec) => rec
      case Left(e) => fail(e)
    }
  }

  def deleteRecording(rec: Recording, force: Boolean): Boolean = {
    val status =
      if (force) {
        conn.forceDeleteRecording(rec) match {
          case Right(r) => r
          case Left(e) => fail(e)
        }
      }
      else {
        conn.deleteRecording(rec) match {
          case Right(r) => r
          case Left(e) => fail(e)
        }
      }
    status == 0
  }

  def forgetRecording(rec: Recording): Boolean = {
    conn.forgetRecording(rec) match {
      case Right(r) => r
      case Left(e) => fail(e)
    }
  }

  def stopRecording(rec: Recording): Option[CaptureCardId] = {
    conn.stopRecording(rec).toOption
  }

  def reschedule(): Unit = internalReschedule(None, wait = false)
  def reschedule(wait: Boolean): Unit = internalReschedule(None, wait)
  def reschedule(recordId: RecordRuleId, wait: Boolean = false): Unit = internalReschedule(None, wait)

  private def internalReschedule(recordId: Option[RecordRuleId], wait: Boolean): Unit = {
    val lock =
      if (wait) EventLock(eventConnection, _ == Event.ScheduleChangeEvent)
      else EventLock.empty
    if (recordId.isEmpty) conn.rescheduleRecordingsCheck(programId = "**any**")
    else conn.rescheduleRecordingsMatch(recordId = recordId.get)
    lock.await()
  }

  def isRecording(cardId: CaptureCardId): Boolean = {
    conn.queryRecorderIsRecording(cardId) match {
      case Right(r) => r
      case Left(e) => fail(e)
    }
  }

  def recordingsIterator: ExpectedCountIterator[Recording] = {
    conn.queryRecordings("Ascending") match {
      case Right(rec) => rec
      case Left(e) => fail(e)
    }
  }

  def recordings: List[Recording] = recordingsIterator.toList

  def expiringRecordingsIterator: ExpectedCountIterator[Recording] = {
    conn.queryGetExpiring match {
      case Right(rec) => rec
      case Left(e) => fail(e)
    }
  }

  def expiringRecordings: List[Recording] = expiringRecordingsIterator.toList

  def pendingRecordingsIterator: ExpectedCountIterator[Recordable] = {
    conn.queryGetAllPending match {
      case Right(rec) => rec
      case Left(e) => fail(e)
    }
  }

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

  @nowarn   // getFreeRecorderList is deprecated by us, don't warn
  def availableRecorders: List[CaptureCardId] = {
    conn.getFreeRecorderList match {
      case Right(cardids) => cardids
      case Left(e) => fail(e)
    }
  }

  //////

  def freeSpaceSummary: (ByteCount, ByteCount) = {
    conn.queryFreeSpaceSummary match {
      case Right(r) => r
      case Left(e) => fail(e)
    }
  }

  def freeSpace: List[FreeSpace] = {
    conn.queryFreeSpace match {
      case Right(r) => r
      case Left(e) => fail(e)
    }
  }

  def freeSpaceCombined: List[FreeSpace] = {
    conn.queryFreeSpaceList match {
      case Right(r) => r
      case Left(e) => fail(e)
    }
  }

  def fileHash(fileName: String, storageGroup: String, hostName: String): MythFileHash = {
    conn.queryFileHash(fileName, storageGroup, hostName) match {
      case Right(hash) => hash
      case Left(e) => fail(e)
    }
  }

  def fileExists(fileName: String, storageGroup: String): Boolean =
    conn.queryFileExists(fileName, storageGroup).isRight

  def uptime: Duration = {
    conn.queryUptime match {
      case Right(r) => r
      case Left(e) => fail(e)
    }
  }

  def loadAverages: (Double, Double, Double) = {
    conn.queryLoad match {
      case Right(r) => r
      case Left(e) => fail(e)
    }
  }

  def isActiveBackend(hostname: String): Boolean = {
    conn.queryIsActiveBackend(hostname) match {
      case Right(r) => r
      case Left(e) => fail(e)
    }
  }

  def isActive: Boolean = isActiveBackend(host)

  def guideDataThrough: MythDateTime = {
    conn.queryGuideDataThrough match {
      case Right(r) => r
      case Left(e) => fail(e)
    }
  }

  def scanVideos(): Map[String, Set[VideoId]] = {
    import connection.myth.Event.{ VideoListChangeEvent, VideoListNoChangeEvent }

    val scanSuccess = conn.scanVideos() match {
      case Right(r) => r
      case Left(e) => fail(e)
    }

    if (scanSuccess) {
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
