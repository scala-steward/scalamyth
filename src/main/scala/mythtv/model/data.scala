package mythtv
package model

import java.time.{ Duration, Instant, LocalDateTime, LocalDate, LocalTime, Year, ZoneOffset }

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

trait CaptureCard {
  def cardId: CaptureCardId
  def videoDevice: Option[String]
  def audioDevice: Option[String]
  def vbiDevice: Option[String]
  def cardType: Option[String]
  def audioRateLimit: Option[Int]
  def hostName: String    // TODO can this really be nullable as the DB schema says?
  def dvbSwFilter: Option[Int]  // TODO what is this?
  def dvbSatType: Option[Int]   // TODO what is this?
  def dvbWaitForSeqStart: Boolean
  def skipBtAudio: Boolean
  def dvbOnDemand: Boolean
  def dvbDiseqcType: Option[Int] // TODO what is this?
  def firewireSpeed: Option[Int] // TODO what is this?
  def firewireModel: Option[String]
  def firewireConnection: Option[Int]
  def signalTimeout: Int   // TODO what are units?
  def channelTimeout: Int  // TODO what are units?
  def dvbTuningDelay: Int  // TODO what are units?
  def contrast: Int        // TODO these all default to zero, is that a valid value?, i.e. can we map to None?
  def brightness: Int
  def colour: Int
  def hue: Int
  def diseqcId: Option[Int]
  def dvbEitScan: Boolean
  // field 'defaultinput' from the DB capturecard table is excluded here
}

trait RemoteEncoder {
  def cardId: CaptureCardId  // is this correct type?
  def host: String
  def port: Int
}

// this does not seem to include "port" data, though, hmm...
trait RemoteEncoderState extends RemoteEncoder {
  def local: Boolean
  def connected: Boolean
  def lowFreeSpace: Boolean
  def state: TvState
  def sleepStatus: SleepStatus
  def currentRecording: Option[Recording]
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

  def stereo: Boolean    = audioProps contains AudioProperties.Stereo
  def subtitled: Boolean = subtitleType != SubtitleType.Unknown
  def hdtv: Boolean      = videoProps contains VideoProperties.Hdtv  // TODO might need to check Hd1080 and Hd720 also
  def closeCaptioned: Boolean = ???  // Do we use audioProps or subtitleType (or both?)
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
  def hostname: String           // TODO why is this in Recordable vs Recording? Services API only has data here for recordings
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
  def chanNum: ChannelNumber       // TODO only in backend program, services recording Channel
  def callsign: String             // TODO only in backend program, services recording Channel
  def chanName: String             // TODO only in backend program, services recording Channel
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
  /*
  def stereo: Boolean            // These are bound into Audio/Video properties bitmask in Program?
  def subtitled: Boolean
  def hdtv: Boolean
  def closeCaptioned: Boolean
  */

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

// This structure optimized for channel-based access rather than time-based access.
// This mirrors what is returned by the Services API GetProgramGuide
trait Guide[Chan <: Channel, +Prog <: Program] {
  def startTime: MythDateTime
  def endTime: MythDateTime
  def startChanId: ChanId
  def endChanId: ChanId
  def programCount: Int
  def programs: Map[Chan, Seq[Prog]]
}


// TODO storage group stuff ...

// TODO make a tuple type for (chanid, starttime) to shorten parameter lists?
//        and to ease switchover to 0.28+ recordedId in places?

/* TODO these five items are included in the channel info we get from the MythProtocol
   command QUERY_RECORDER/GET_CHANNEL_INFO (+xmltvId) ; but there are many more fields
   in the database and returned from services API */
trait Channel {
  def chanId: ChanId
  def name: String
  def number: ChannelNumber
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

  override def toString: String = s"<Channel $chanId ${number.num} $callsign>"
}

trait ChannelDetails extends Channel {
  def freqId: Option[String]
  def iconPath: String  // TODO is this a URL or file path (or could be either!)
  def fineTune: Option[Int]   // TODO what is this?
  /* TODO does videofilters field map anywhere? */
  def xmltvId: String
  /* TODO: does recpriority field map anywhere? */
  // also contrast, brightness, colour, hue
  def format: String   // sometimes set to "" even when database entry disagrees (e.g. in return from GetRecorded...)
  def visible: Boolean
  /* TODO does outputfilters field map anywhere? */
  def useOnAirGuide: Boolean  // TODO is this really nullable as database schema indicates?
  def mplexId: Option[Int]    // TODO what is this?
  def serviceId: Option[Int]  // TODO what is this?
  /* TODO does tmoffset map anywhere */
  def atscMajorChan: Option[Int]      // sometimes set to "0" even when data is avail (e.g. in return from GetRecorded...)
  def atscMinorChan: Option[Int]      //     "       "  "   "
  /* TODO last_record? */
  def defaultAuthority: Option[String]  // TODO what is this?
  /* TODO does db commmethod map to commfree ? */
  /* iptvid? */
  /* Results not in DB: ??
       ChanFilters, CommFree, Frequency, FrequencyTable, Modulation, NetworkId, SIStandard, TransportId
       some of this data may come from dtv_multiple or channelscan_dtv_multiplex table?
   */
}

trait ListingSource {
  def sourceId: ListingSourceId
  def name: String
  def grabber: Option[String]
  def freqTable: String
  def lineupId: Option[String]
  def userId: Option[String]
  def password: Option[String]
  def useEit: Boolean
  def configPath: Option[String]
  def dvbNitId: Option[Int]
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
  def season: Option[Int]
  def episode: Option[Int]
  def category: String
  def recProfile: String
  def recPriority: Int
  def autoExpire: Boolean
  def maxEpisodes: Int
  def maxNewest: Boolean
  def startOffset: Int   // TODO what is units? minutes?
  def endOffset: Int     // TODO what is units? minutes?
  def recGroup: String
  def dupMethod: DupCheckMethod
  def dupIn: DupCheckIn
  def callsign: String   // NB called "station" in the record DB table
  def seriesId: Option[String]
  def programId: Option[String]
  def inetRef: Option[String]
  def searchType: RecSearchType
  def autoTranscode: Boolean
  def autoCommFlag: Boolean
  def autoUserJob1: Boolean
  def autoUserJob2: Boolean
  def autoUserJob3: Boolean
  def autoUserJob4: Boolean
  def autoMetadata: Boolean
  def findDay: Int      // TODO is this really a day-of-week integer?
  def findTime: Option[LocalTime]
  def inactive: Boolean
  def parentId: Option[RecordRuleId]
  def transcoder: Option[Int]   // TODO what type is this? (ugh, see what I do here in mythjango...)
  def playGroup: String
  def preferredInput: Option[Int]       // TODO what type is this? CardInputId from the cardinput table (NOT CaptureCardId)
  def nextRecord: Option[MythDateTime]
  def lastRecord: Option[MythDateTime]
  def lastDelete: Option[MythDateTime]
  def storageGroup: String
  def averageDelay: Int   // TODO what units?
  def filter: Option[Int] // TODO what type is this? bitmask enum of data from the recordfilter table?
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

trait Settings {
  def hostName: String
  def settings: Map[String, String]

  override def toString: String = s"<Settings for $hostName (${settings.size})>"
}

trait TimeZoneInfo {
  def tzName: String
  def offset: ZoneOffset
  def currentTime: Instant

  override def toString: String = s"<TimeZoneInfo $tzName $offset>"
}

// TODO from services
trait StorageGroupDir  // TODO move
trait LiveStreamInfo   // TODO move
trait FrontendStatus   // TODO move
trait FrontendAction   // TODO move

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
