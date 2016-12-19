package mythtv
package model

import java.time.{ LocalDate, Year }

import EnumTypes._
import util.{ IntBitmaskEnum, ByteCount, LooseEnum, MythDateTime }


trait Titled {
  def title: String
  def subtitle: String

  def combinedTitle: String = combinedTitle(": ")
  def combinedTitle(sep: String): String =
    if (subtitle.nonEmpty) title + sep + subtitle
    else title
}

trait ProgramVideoBase extends Titled {
  def description: String
  def year: Option[Year]      // NB called 'airdate' in program table
  def season: Option[Int]
  def episode: Option[Int]
}

/* metadata acquired from the internet (e.g. TheTVDB.com or TheMovieDB.org (TMDb) or IMDb(?)) */
trait InternetMetadata {
  def inetRef: Option[String]
}

// fields returned from Services API when Details=false
trait ProgramBrief extends Titled {
  def chanId: ChanId
  def startTime: MythDateTime
  def endTime: MythDateTime
  def category: String
  def categoryType: Option[CategoryType]   // only really seems to be populated in program guide stuff
  def audioProps: AudioProperties
  def videoProps: VideoProperties
  def subtitleType: SubtitleType   // TODO do we only know this after recording?, in program table

  def isRepeat: Boolean

  def stereo: Boolean    = audioProps contains AudioProperties.Stereo
  def subtitled: Boolean = subtitleType != SubtitleType.Unknown

  def hdtv: Boolean =
    videoProps.containsAny(VideoProperties.Hdtv | VideoProperties.Hd720 | VideoProperties.Hd1080)

  def closeCaptioned: Boolean =
    (subtitleType contains SubtitleType.HardHear) ||
      (audioProps contains AudioProperties.HardHear)
}

trait Program extends ProgramBrief with ProgramVideoBase {
  def totalEpisodes: Option[Int]           // starting in MythTV 0.28
  def syndicatedEpisode: String
  def seriesId: String
  def programId: String
  def stars: Option[Double]
  def originalAirDate: Option[LocalDate]
  def partNumber: Option[Int]
  def partTotal: Option[Int]
  def programFlags: ProgramFlags

  def isRepeat: Boolean = programFlags contains ProgramFlags.Repeat
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

trait RecordableBrief extends ProgramBrief {
  def recStatus: RecStatus
  def recPriority: Int
  def recStartTS: MythDateTime
  def recEndTS: MythDateTime
  def chanNum: ChannelNumber
  def callsign: String
  def chanName: String
}

trait Recordable extends Program with RecordableBrief {
  def findId: Int
  def hostname: String
  def sourceId: ListingSourceId
  def cardId: CaptureCardId
  def inputId: InputId
  def recordId: RecordRuleId
  def recType: RecType
  def dupIn: DupCheckIn
  def dupMethod: DupCheckMethod
  def recGroup: String
  def storageGroup: String
  def playGroup: String
  def lastModified: MythDateTime
  def outputFilters: String
  def parentId: Option[RecordRuleId]

  def recPriority2: Int            // TODO only surfaced by MythProtocol? Is it useful to be here?
}

trait RecordingBrief extends RecordableBrief {
  def recordedId: RecordedId
}

trait Recording extends Recordable with RecordingBrief with InternetMetadata {
  def filename: String
  def filesize: ByteCount

  // TODO get this working w/out calling .id
  def programType: ProgramType = ProgramType.applyOrUnknown((ProgramFlags.ProgramType.id & programFlags.id) >> 16)

  def isInUsePlaying: Boolean   = programFlags contains ProgramFlags.InUsePlaying
  def isCommercialFree: Boolean = programFlags contains ProgramFlags.ChanCommFree
  def hasCutList: Boolean       = programFlags contains ProgramFlags.CutList
  def hasBookmark: Boolean      = programFlags contains ProgramFlags.Bookmark
  def isWatched: Boolean        = programFlags contains ProgramFlags.Watched
  def isAutoExpirable: Boolean  = programFlags contains ProgramFlags.AutoExpire
  def isPreserved: Boolean      = programFlags contains ProgramFlags.Preserved
  def isDuplicate: Boolean      = programFlags contains ProgramFlags.Duplicate
  def isReactivated: Boolean    = programFlags contains ProgramFlags.Reactivate
  def isDeletePending: Boolean  = programFlags contains ProgramFlags.DeletePending

  def isDummy: Boolean = title.isEmpty && chanId.id == 0
}

sealed trait RecordedId {
  def idString: String
}

object RecordedId {
  final val empty: RecordedId = RecordedIdInt(0)
  case class RecordedIdInt(id: Int) extends RecordedId with IntegerIdentifier { def idString = id.toString }
  case class RecordedIdChanTime(chanId: ChanId, startTime: MythDateTime) extends RecordedId {
    def idString = chanId.id.toString + "_" + startTime.toMythFormat
  }
}

object ProgramFlags extends IntBitmaskEnum {
  type ProgramFlags = Base
  val None           =  Mask(0x00000000)
  val CommFlag       = Value(0x00000001)
  val CutList        = Value(0x00000002)
  val AutoExpire     = Value(0x00000004)
  val Editing        = Value(0x00000008)
  val Bookmark       = Value(0x00000010)
  val ReallyEditing  = Value(0x00000020)
  val CommProcessing = Value(0x00000040)
  val DeletePending  = Value(0x00000080)
  val Transcoded     = Value(0x00000100)
  val Watched        = Value(0x00000200)
  val Preserved      = Value(0x00000400)
  val ChanCommFree   = Value(0x00000800)
  val Repeat         = Value(0x00001000)
  val Duplicate      = Value(0x00002000)
  val Reactivate     = Value(0x00004000)
  val IgnoreBookmark = Value(0x00008000)
  val ProgramType    =  Mask(0x000f0000)
  val InUseRecording = Value(0x00100000)
  val InUsePlaying   = Value(0x00200000)
  val InUseOther     = Value(0x00400000)
}

object ProgramType extends LooseEnum {
  type ProgramType = Value
  val Recording      = Value(0)
  val VideoFile      = Value(1)
  val Dvd            = Value(2)
  val StreamingHtml  = Value(3)
  val StreamingRtsp  = Value(4)
  val Bluray         = Value(5)
}

object CategoryType extends LooseEnum {
  type CategoryType = Value
  val None     = Value(0, "")
  val Movie    = Value(1, "movie")
  val Series   = Value(2, "series")
  val Sports   = Value(3, "sports")
  val TvShow   = Value(4, "tvshow")
}

object RecStatus extends LooseEnum {
  type RecStatus = Value
  val OtherRecording    = Value(-13)
  val OtherTuning       = Value(-12)
  val MissedFuture      = Value(-11)
  val Tuning            = Value(-10)
  val Failed            = Value(-9)
  val TunerBusy         = Value(-8)
  val LowDiskSpace      = Value(-7)
  val Cancelled         = Value(-6)
  val Missed            = Value(-5)
  val Aborted           = Value(-4)
  val Recorded          = Value(-3)
  val Recording         = Value(-2)
  val WillRecord        = Value(-1)
  val Unknown           = Value(0)
  val DontRecord        = Value(1)
  val PreviousRecording = Value(2)
  val CurrentRecording  = Value(3)
  val EarlierShowing    = Value(4)
  val TooManyRecordings = Value(5)
  val NotListed         = Value(6)
  val Conflict          = Value(7)
  val LaterShowing      = Value(8)
  val Repeat            = Value(9)
  val Inactive          = Value(10)
  val NeverRecord       = Value(11)
  val Offline           = Value(12)
  val OtherShowing      = Value(13)
}

object AudioProperties extends IntBitmaskEnum {
  type AudioProperties = Base
  val Unknown      =  Mask(0x00)
  val Stereo       = Value(0x01)
  val Mono         = Value(0x02)
  val Surround     = Value(0x04)
  val Dolby        = Value(0x08)
  val HardHear     = Value(0x10)
  val VisualImpair = Value(0x20)
}

object VideoProperties extends IntBitmaskEnum {
  type VideoProperties = Base
  val Unknown    =  Mask(0x00)
  val Hdtv       = Value(0x01)
  val Widescreen = Value(0x02)
  val AVC        = Value(0x04)
  val Hd720      = Value(0x08)
  val Hd1080     = Value(0x10)
  val Damaged    = Value(0x20)
  val ThreeD     = Value(0x40)
}

object SubtitleType extends IntBitmaskEnum {
  type SubtitleType = Base
  val Unknown  =  Mask(0x00)
  val HardHear = Value(0x01)
  val Normal   = Value(0x02)
  val OnScreen = Value(0x04)
  val Signed   = Value(0x08)
}
