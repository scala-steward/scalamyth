package mythtv

import java.time.{ Duration, Instant, LocalDateTime }

import model._
import EnumTypes._
import util.{ ByteCount, MythDateTime }


trait BackendOperations {
  def recordings: Iterable[Recording]
  def expiringRecordings: Iterable[Recording]

  def pendingRecordings: Iterable[Recordable]
  def upcomingRecordings: Iterable[Recordable]
  def scheduledRecordings: Iterable[Recordable]
  def conflictingRecordings: Iterable[Recordable]
/*
  def recorders: Iterable[CaptureCard]   // this is a DB method in the python bindings
  def availableRecorders: Iterable[CaptureCard]
 */
  def freeSpace: List[FreeSpace]
  def freeSpaceCombined: List[FreeSpace]
  def freeSpaceSummary: (ByteCount, ByteCount)

  def uptime: Duration
  def loadAverages: (Double, Double, Double)

  def isActiveBackend(hostname: String): Boolean

/*
  def isRecording(card: CaptureCard): Boolean
 */

  // These are FileOps methods in the Python bindings ...
/*
  def recording(chanId: Int, startTime: LocalDateTime): Recording
  def forgetRecording(rec: Recording): Boolean
  def deleteRecording(rec: Recording, force: Option[Boolean]): Boolean

  def reschedule(recordId: Option[Int], wait: Option[Boolean]): Unit
 */

  // These are MythXML methods in the Python bindings ...
/*
  def programGuide(startTime: LocalDateTime, endTime: LocalDateTime, startChannelId: Int, numChannels: Option[Int]): Guide
  def programDetails(chanId: Int, startTime: LocalDateTime)
  def previewImage(chanId: Int, startTime: LocalDateTime, width: Option[Int], height: Option[Int], secsIn: Option[Int]): Array[Byte]
 */
}

trait FrontendOperations {
  def play(media: PlayableMedia): Boolean
  def screenshot(format: String, width: Int, height: Int): Array[Byte]

  def uptime: Duration
  def loadAverages: List[Double]
  def memoryStats: Map[String, Long]  // memory type -> bytes available
  def currentTime: Instant

  // remote control methods
  def key: PartialFunction[MythFrontend.KeyName, Boolean]
  def jump: PartialFunction[MythFrontend.JumpPoint, Boolean]
}

trait BackendServiceOperations {
  def hosts: List[String]
  def keys: List[String]
  def setting(key: String, hostname: Option[String] = None, default: Option[String] = None)
}
