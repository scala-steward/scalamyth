package mythtv
package model

import java.time.{ Duration, Instant, LocalDateTime, LocalDate, Year }

import EnumTypes._
import util.{ ByteCount, MythDateTime }

/* We define the Ordering objects in companion objects to the case classes
   because nested objects are currently prohibited inside value classes. */

final case class ChanId(id: Int) extends AnyVal

object ChanId {
  object ChanIdOrdering extends Ordering[ChanId] {
    def compare(x: ChanId, y: ChanId): Int = x.id compare y.id
  }
  implicit def ordering: Ordering[ChanId] = ChanIdOrdering
}

final case class ChannelNumber(num: String) extends AnyVal

object ChannelNumber {
  object ChannelNumberOrdering extends Ordering[ChannelNumber] {
    def split(n: ChannelNumber): Array[String] = n.num split "[^0-9]"
    def compare(x: ChannelNumber, y: ChannelNumber): Int = {
      val xs = split(x)
      val ys = split(y)
      val len = math.min(xs.length, ys.length)
      var i, cmp = 0
      while (cmp == 0 && i < len) {
        cmp = xs(i).toInt compare ys(i).toInt
        i += 1
      }
      cmp
    }
  }
  implicit def ordering: Ordering[ChannelNumber] = ChannelNumberOrdering
}

final case class CaptureCardId(id: Int) extends AnyVal
final case class StorageGroupId(id: Int) extends AnyVal
final case class RecordRuleId(id: Int) extends AnyVal
final case class VideoId(id: Int) extends AnyVal
final case class JobId(id: Int) extends AnyVal
final case class ListingSourceId(id: Int) extends AnyVal

/**
 * Represents the position of a video stream as a frame number.
 */
final case class VideoPosition(pos: Long) extends AnyVal

object VideoPosition {
  object VideoPositionOrdering extends Ordering[VideoPosition] {
    def compare(x: VideoPosition, y: VideoPosition): Int = x.pos compare y.pos
  }
  implicit def ordering: Ordering[VideoPosition] = VideoPositionOrdering
}

trait VideoSegment {
  def start: VideoPosition
  def end: VideoPosition
  override def toString: String = start.pos + ":" + end.pos
}

trait RecordedMarkup {
  def tag: Markup
  def position: VideoPosition
}

trait Backend extends BackendOperations
trait Frontend extends FrontendOperations

trait Encoder  // TODO how is Encoder different than CaptureCard?
trait CaptureCard {
  def cardId: CaptureCardId
  // TODO more
}

trait RemoteEncoder {
  def cardId: CaptureCardId  // is this correct type?
  def host: String
  def port: Int
}

trait CardInput {
  def cardInputId: Int
  def cardId: CaptureCardId
  def sourceId: ListingSourceId   // is this the right source Id?
  def name: String
  def mplexId: Int
  def liveTVorder: Int
}

trait FreeSpace {
  def host: String
  def path: String
  def isLocal: Boolean
  def diskNumber: Int
  def sGroupId: StorageGroupId
  def blockSize: ByteCount
  def totalSpace: ByteCount
  def usedSpace: ByteCount
  def freeSpace: ByteCount
}

trait ProgramAndVideoBase {   /// TODO need a better name for this trait.
  def title: String
  def subtitle: String
  def description: String
  def year: Option[Year]      // NB not Option in Video
  // TODO category ?
  // TODO stars/rating?
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
  def audioProps: AudioProperties  // TODO do we only know this after recording?, in program table
  def videoProps: VideoProperties  // TODO do we only know this after recording?, in program table
  def subtitleType: SubtitleType   // TODO do we only know this after recording?, in program table
  def year: Option[Year]           // NB called 'airdate' in program table
  def partNumber: Option[Int]
  def partTotal: Option[Int]
}

// Used, e.g. by Myth protocol QUERY_RECORDER/GET_NEXT_PROGRAM_INFO
//  (that message also returns some additional channel info: callsign, channame, iconpath)
trait UpcomingProgram {
  def title: String
  def subtitle: String
  def description: String
  def category: String
  def chanId: ChanId
  def startTime: MythDateTime
  def endTime: MythDateTime
  def seriesId: String
  def programId: String
}

trait Recordable extends Program {
  def findId: Int
  def hostname: String
  def sourceId: ListingSourceId
  def cardId: CaptureCardId
  def inputId: Int
  def recPriority: Int
  def recStatus: RecStatus
  def recordId: RecordRuleId
  def recType: RecType
  def dupIn: DupCheckIn
  def dupMethod: DupCheckMethod
  def recStartTS: MythDateTime
  def recEndTS: MythDateTime
  def recGroup: String
  def storageGroup: String
  def playGroup: String
  def recPriority2: Int

  def parentId: Int                // TODO what is? move to recordable?
  def lastModified: MythDateTime   // TODO what is? move to recordable?
  def chanNum: String              // TODO only in backend program
  def callsign: String             // TODO only in backend program
  def chanName: String             // TODO only in backend program
  def programFlags: Int            // TODO what is? move to recordable?, is it HDTV, etc. bitmask?
  def outputFilters: String        // TODO what is? move to recordable?
}

trait Recording extends Recordable {
  def filename: String
  def filesize: ByteCount

  // metadata downloaded from the internet? not in program guide
  def season: Int                  // TODO only for Recording?, not in program table
  def episode: Int                 // TODO only for Recording?, not in program table
  def inetRef: String              // TODO not in program table
}

trait PreviousRecording {
  // TODO  what's available here? a subset of Recording?
}

trait ProgramGuideEntry extends Program {
  // Fields not originally written here but in the program table
  def audioprop: Set[Any]      // TODO enum set -- called audioProps in Program
  def videoprop: Set[Any]      // TODO enum set -- called videoProps in Program
  def subtitletypes: Set[Any]  // TODO enum set -- called subtitleType in Program

  // These fields are not present (at least not directly) in Program object
  def stereo: Boolean            // These are bound into Audio/Video properties bitmask in Program?
  def subtitled: Boolean
  def hdtv: Boolean
  def closeCaptioned: Boolean

  def titlePronounce: String  // TODO what is this?
  def categoryType: String
  def previouslyShown: Boolean
  def showType: String
  def colorCode: String
  def manualId: Int  // TODO what is this?
  def generic: Int   // TODO what is this?
  def listingSource: ListingSourceType
  def first: Int     // TODO what is this?
  def last: Int      // TODO what is this?

// Computed/queried...
  def recStatus: RecStatus

  // TODO : what else?
}

// TODO storage group stuff ...

// TODO make a tuple type for (chanid, starttime) to shorten parameter lists?
//        and to ease switchover to 0.28+ recordedId in places?

trait Channel {
  def chanId: ChanId
  def name: String
  def number: String
  def callsign: String
  def sourceId: ListingSourceId  // TODO is this the right type? or do we need VideoSourceId?

  /*
  def xmltvId: String //TODO, expose this here? it is returned by QUERY_RECORDER/GET_CHANNEL_INFO in MythProtocol
   */

  /*
  def visible: Boolean
  def recPriority: Int           // TODO do we want this here?  Not in serivce object?
  def lastRecord: MythDateTime   // TODO do we want this here?  Not in service object?
   */
}

trait Video {
  def id: VideoId
  def title: String
  def subtitle: String
  def director: String
  def year: Year
  // TODO more
  // TODO check these against database, they are from VideoMetadataInfo service
  def tagline: Option[String]
  def description: String
  def inetRef: String
  def homePage: String
  def studio: Option[String]
  def season: Int
  def episode: Int
  def length: Option[Duration]  // duration of video in minutes (may be zero == unknown)
  def playCount: Int
  def hash: String
  def visible: Boolean
  def fileName: String
  def contentType: String  // enum? MOVIE, TELEVISION, ADULT, MUSICVIDEO, HOMEVIDEO
  def hostName: String
  def addDate: Option[Instant]
  def watched: Boolean
  def userRating: Double   // TODO rename to stars and converge with Program? Scale is always implied!
  def rating: String       // This is MPPA or some such rating, correct?  Should this really be a Map? (RatingBody -> Rating)
  def collectionRef: Int
  def releaseDate: LocalDate
  // TODO various artworks   These are common to many elements, no?  What should a "HasArtwork" (or may have artwork, really...) trait be called?
}

trait RecordRule {    // TODO seems like this contains most of the elements of ProgramGuideEntry or Recordable or some such...
  def id: RecordRuleId
  def recType: RecType
  def chanId: Option[ChanId]
  def startTime: MythDateTime
  def endTime: MythDateTime
  def title: String
  def subtitle: String
  def description: String
  def category: String
  def seriesId: Option[String]
  def programId: Option[String]
  def inactive: Boolean
  def nextRecord: MythDateTime
  def lastRecord: MythDateTime
  // TODO more
}

trait Job {
  def id: JobId
  def jobType: JobType
  def chanId: ChanId
  def startTime: LocalDateTime
  def comment: String
  def hostname: String
  def flags: JobFlags
  def status: JobStatus
  def statusTime: LocalDateTime
  def insertTime: LocalDateTime
  def schedRunTime: LocalDateTime
}

trait PlayableMedia {
  // TODO what are subclasses?  Program(?), Recording, Video, music?
  // TODO methods on PlayableMedia
  def playOnFrontend(fe: Frontend): Boolean
}

trait ProgramLike extends PlayableMedia {
  // TODO
}

trait RecordingLike extends ProgramLike {
  // TODO
}

trait VideoLike extends PlayableMedia {
  // TODO
}

// TODO from services
trait Setting          // TODO move
trait TimeZoneInfo     // TODO move
trait StorageGroupDir  // TODO move
trait LiveStreamInfo   // TODO move
trait FrontendStatus   // TODO move
trait FrontendAction   // TODO move
trait VideoSource      // TODO move

/**************************************************************************/
/* Database backed, at least in part */
/*
case class D_Channel(chanId: ChanId, props: PropertyMap)
case class D_GuideEntry(chanId: ChanId, startTime: LocalDateTime, props: PropertyMap)   // DB based, table=program
case class B_Program(chanId: ChanId, startTime: LocalDateTime, props: PropertyMap)      // BE based
case class D_Recording(chanId: ChanId, startTime: LocalDateTime, props: PropertyMap)    // DB based, table=recorded
case class D_CaptureCard(cardId: Int, props: PropertyMap)

case class D_PreviousRecording(chanId: ChanId, startTime: LocalDateTime, props: PropertyMap)
case class D_RecordRule(id: Int, props: PropertyMap)
case class D_Video(id: Int, props: PropertyMap)
case class D_Song(props: PropertyMap)  // TODO what is primary key for MythMusic stuff?
case class D_Album(props: PropertyMap)
case class D_Artist(props: PropertyMap)
case class D_MusicPlaylist(props: PropertyMap)
case class D_Job(jobId: Int, props: PropertyMap)
case class D_Artwork(props: PropertyMap)   // TODO : what is primary key for artwork?
 */
/**************************************************************************/
