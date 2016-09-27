package mythtv

import java.time.{ Duration, Instant, LocalDateTime, LocalDate, Year }

import mythtv.enum.Types._
import mythtv.util.{ ByteCount, MythDateTime }

final case class ChanId(n: Int) extends AnyVal

/* Database backed, at least in part */
case class D_Channel(chanId: ChanId, props: PropertyMap)
case class D_GuideEntry(chanId: ChanId, startTime: LocalDateTime, props: PropertyMap)   // DB based, table=program
case class B_Program(chanId: ChanId, startTime: LocalDateTime, props: PropertyMap)      // BE based
case class D_Recording(chanId: ChanId, startTime: LocalDateTime, props: PropertyMap)    // DB based, table=recorded

case class PreviousRecording(chanId: ChanId, startTime: LocalDateTime, props: PropertyMap)
case class RecordRule(id: Int, props: PropertyMap)
case class Video(id: Int, props: PropertyMap)
case class Song(props: PropertyMap)  // TODO what is primary key for MythMusic stuff?
case class Album(props: PropertyMap)
case class Artist(props: PropertyMap)
case class MusicPlaylist(props: PropertyMap)
case class Job(jobId: Int, props: PropertyMap)
case class Artwork(props: PropertyMap)   // TODO : what is primary key for artwork?

case class CaptureCard(cardId: Int, props: PropertyMap)
trait Encoder  // TODO how different than a CaptureCard?

trait Frontend
trait Backend

trait PlayableMedia {
  // TODO what are subclasses?  Program(?), Recording, Video
  // TODO methods on PlayableMedia
  def playOnFrontend(fe: Frontend): Boolean
}

trait FreeSpace {
  def host: String
  def path: String
  def isLocal: Boolean
  def diskNumber: Int
  def sGroupId: Int
  def blockSize: ByteCount
  def totalSpace: ByteCount
  def usedSpace: ByteCount
  def freeSpace: ByteCount
}

// TODO some of these fields are optional or have default (meaningless values)
trait Program {
  def title: String
  def subtitle: String
  def description: String
  def syndicatedEpisodeNumber: String
  def category: String
  def chanId: ChanId
  def startTime: MythDateTime
  def endTime: MythDateTime
  def seriesId: String
  def programId: String
  def stars: Option[Double]
  def originalAirDate: Option[LocalDate]
  def audioProps: Int              // TODO do we only know this after recording?, in program table
  def videoProps: Int              // TODO do we only know this after recording?, in program table
  def subtitleType: Int            // TODO do we only know this after recording?, in program table
  def year: Option[Int]            // TODO called 'airdate' in program table
  def partNumber: Option[Int]
  def partTotal: Option[Int]
}

trait Recordable extends Program {
  def findId: Int
  def hostname: String
  def sourceId: Int
  def cardId: Int
  def inputId: Int
  def recPriority: Int
  def recStatus: Int
  def recordId: Int
  def recType: Int
  def dupIn: Int
  def dupMethod: Int
  def recStartTS: MythDateTime
  def recEndTS: MythDateTime
  def recGroup: String
  def storageGroup: String
  def playGroup: String
  def recPriority2: Int

  def parentId: Int                // TOOD what is? move to recordable?
  def lastModified: MythDateTime   // TOOD what is? move to recordable?
  def chanNum: String              // TODO only in backend program
  def callsign: String             // TODO only in backend program
  def chanName: String             // TODO only in backend program
  def programFlags: Int            // TOOD what is? move to recordable?, is it HDTV, etc. bitmask?
  def outputFilters: String        // TOOD what is? move to recordable?
}

trait Recording extends Recordable {
  def filename: String
  def filesize: ByteCount

  // metadata downloaded from the internet? not in program guide
  def season: Int                  // TODO only for Recording?, not in program table
  def episode: Int                 // TODO only for Recording?, not in program table
  def inetRef: String              // TODO not in program table
}

trait GuideEntry extends Program {
  // Fields not originally written here but in the program table
  def audioprop: Set[Any]      // TODO enum set -- called audioProps in Program
  def videoprop: Set[Any]      // TODO enum set -- called videoProps in Program
  def subtitletypes: Set[Any]  // TODO enum set -- called subtitleType in Program

  // These fields are not present (at least not directly) in Program object
  def titlePronounce: String  // TODO what is this?
  def categoryType: String
  def previouslyShown: Boolean
  def stereo: Boolean
  def subtitled: Boolean
  def hdtv: Boolean
  def closeCaptioned: Boolean
  def showType: String
  def colorCode: String
  def manualId: Int  // TODO what is this?
  def generic: Int   // TODO what is this?
  def listingSource: Int
  def first: Int     // TODO what is this?
  def last: Int      // TODO what is this?

// Computed...
  def recStatus: RecStatus
  // TODO : what else?
}

// TODO storage group stuff ...

// TODO make a tuple type for (chanid, starttime) to shorten parameter lists?

trait Channel {
  def chanId: ChanId
  def name: String
  def callsign: String
  def visible: Boolean
  def recpriority: Int
  def lastRecord: LocalDateTime
}

trait IProgram extends PlayableMedia {
  // TODO
}

trait IRecording extends IProgram {
  // TODO
}

trait IPreviousRecording {
  // TODO
}

trait IVideo extends PlayableMedia {
  def id: Int
  def title: String
  def subtitle: String
  def director: String
  def year: Year
  // TODO more
}

trait IRecordRule {
  def id: Int
  def rectype: RecType
  def chanId: Option[Int]
  def startTime: LocalDateTime
  def endTime: LocalDateTime
  def title: String
  def subtitle: String
  def description: String
  def category: String
  def seriesId: Option[String]
  def programId: Option[String]
  def inactive: Boolean
  def nextRecord: LocalDateTime
  def lastRecord: LocalDateTime
  // TODO more
}

trait IJob {
  def id: Int
  def jobType: Any // TODO need job type enumeration? use integer? what about extensibility?
  def comment: String
  def hostname: String
  def flags: Any   // TODO what type is this?
  def status: JobStatus
  def statusTime: LocalDateTime
  def chanId: Int
  def startTime: LocalDateTime
  def insertTime: LocalDateTime
  def schedRunTime: LocalDateTime
}

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
  def loadAverages: List[Double]

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
  def key: PartialFunction[String, Boolean]   // TODO use KeyName type
  def jump: PartialFunction[String, Boolean]  // TODO use JumpPoint type
}

// TODO: rename to "ServiceOperations"? we can use JSON as interchange format vs XML
trait BackendXMLOperations {
  def hosts: List[String]
  def keys: List[String]
  def setting(key: String, hostname: Option[String] = None, default: Option[String] = None)
}
