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

  def availableRecorders: Iterable[CaptureCardId]

  def freeSpace: List[FreeSpace]
  def freeSpaceCombined: List[FreeSpace]
  def freeSpaceSummary: (ByteCount, ByteCount)

  def uptime: Duration
  def loadAverages: (Double, Double, Double)

  def isActiveBackend(hostname: String): Boolean

  def guideDataThrough: MythDateTime

  def isRecording(cardId: CaptureCardId): Boolean

  // These are FileOps methods in the Python bindings ...
  def recording(chanId: ChanId, startTime: MythDateTime): Recording
  def deleteRecording(rec: Recording, force: Boolean = false): Boolean
  def forgetRecording(rec: Recording): Boolean
  def stopRecording(rec: Recording): Option[CaptureCardId]

  /*
   * Storage group file operations
   def fileHash(fileName: String: storageGroup: String): String // TODO host optional param?
   def fileExists(fileName: String, storageGroup: String): Boolean
   */

  def reschedule(recordId: Option[RecordRuleId] = None, wait: Boolean = false): Unit

  // These are MythXML methods in the Python bindings ...
/*
  def programGuide(startTime: LocalDateTime, endTime: LocalDateTime, startChannelId: Int, numChannels: Option[Int]): Guide
  def programDetails(chanId: Int, startTime: LocalDateTime)
  def previewImage(chanId: Int, startTime: LocalDateTime, width: Option[Int], height: Option[Int], secsIn: Option[Int]): Array[Byte]
 */

  // These are MythDB methods in the Python bindings ...
  def scanVideos(): Map[String, Set[VideoId]]
  //def recorders: Iterable[CaptureCard]
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
