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
  final val None           =  Mask(0x00000000)
  final val CommFlag       = Value(0x00000001)
  final val CutList        = Value(0x00000002)
  final val AutoExpire     = Value(0x00000004)
  final val Editing        = Value(0x00000008)
  final val Bookmark       = Value(0x00000010)
  final val ReallyEditing  = Value(0x00000020)
  final val CommProcessing = Value(0x00000040)
  final val DeletePending  = Value(0x00000080)
  final val Transcoded     = Value(0x00000100)
  final val Watched        = Value(0x00000200)
  final val Preserved      = Value(0x00000400)
  final val ChanCommFree   = Value(0x00000800)
  final val Repeat         = Value(0x00001000)
  final val Duplicate      = Value(0x00002000)
  final val Reactivate     = Value(0x00004000)
  final val IgnoreBookmark = Value(0x00008000)
  final val ProgramType    =  Mask(0x000f0000)
  final val InUseRecording = Value(0x00100000)
  final val InUsePlaying   = Value(0x00200000)
  final val InUseOther     = Value(0x00400000)
}

object ProgramType extends LooseEnum {
  type ProgramType = Value
  final val Recording      = Value(0)
  final val VideoFile      = Value(1)
  final val Dvd            = Value(2)
  final val StreamingHtml  = Value(3)
  final val StreamingRtsp  = Value(4)
  final val Bluray         = Value(5)
}

object CategoryType extends LooseEnum {
  type CategoryType = Value
  final val None     = Value(0, "")
  final val Movie    = Value(1, "movie")
  final val Series   = Value(2, "series")
  final val Sports   = Value(3, "sports")
  final val TvShow   = Value(4, "tvshow")
}

object RecStatus extends LooseEnum {
  type RecStatus = Value
  final val OtherRecording    = Value(-13)
  final val OtherTuning       = Value(-12)
  final val MissedFuture      = Value(-11)
  final val Tuning            = Value(-10)
  final val Failed            = Value(-9)
  final val TunerBusy         = Value(-8)
  final val LowDiskSpace      = Value(-7)
  final val Cancelled         = Value(-6)
  final val Missed            = Value(-5)
  final val Aborted           = Value(-4)
  final val Recorded          = Value(-3)
  final val Recording         = Value(-2)
  final val WillRecord        = Value(-1)
  final val Unknown           = Value(0)
  final val DontRecord        = Value(1)
  final val PreviousRecording = Value(2)
  final val CurrentRecording  = Value(3)
  final val EarlierShowing    = Value(4)
  final val TooManyRecordings = Value(5)
  final val NotListed         = Value(6)
  final val Conflict          = Value(7)
  final val LaterShowing      = Value(8)
  final val Repeat            = Value(9)
  final val Inactive          = Value(10)
  final val NeverRecord       = Value(11)
  final val Offline           = Value(12)
  final val OtherShowing      = Value(13)
}

object AudioProperties extends IntBitmaskEnum {
  type AudioProperties = Base
  final val Unknown      =  Mask(0x00)
  final val Stereo       = Value(0x01)
  final val Mono         = Value(0x02)
  final val Surround     = Value(0x04)
  final val Dolby        = Value(0x08)
  final val HardHear     = Value(0x10)
  final val VisualImpair = Value(0x20)
}

object VideoProperties extends IntBitmaskEnum {
  type VideoProperties = Base
  final val Unknown    =  Mask(0x00)
  final val Hdtv       = Value(0x01)
  final val Widescreen = Value(0x02)
  final val AVC        = Value(0x04)
  final val Hd720      = Value(0x08)
  final val Hd1080     = Value(0x10)
  final val Damaged    = Value(0x20)
  final val ThreeD     = Value(0x40)
}

object SubtitleType extends IntBitmaskEnum {
  type SubtitleType = Base
  final val Unknown  =  Mask(0x00)
  final val HardHear = Value(0x01)
  final val Normal   = Value(0x02)
  final val OnScreen = Value(0x04)
  final val Signed   = Value(0x08)
}
