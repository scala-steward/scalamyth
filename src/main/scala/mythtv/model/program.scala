package mythtv
package model

import java.time.{ LocalDate, Year }

import EnumTypes._
import util.{ ByteCount, MythDateTime }

trait ProgramVideoBase {
  def title: String
  def subtitle: String
  def description: String
  def year: Option[Year]      // NB called 'airdate' in program table

  def combinedTitle: String = combinedTitle(": ")
  def combinedTitle(sep: String): String =
    if (subtitle.nonEmpty) title + sep + subtitle
    else title
}

/* metadata acquired from the internet (e.g. TheTVDB.com or TheMovieDB.org (TMDb) or IMDb(?)) */
trait InternetMetadata {
  def inetRef: Option[String]
  def season: Option[Int]
  def episode: Option[Int]
}

trait Program extends ProgramVideoBase {
  def syndicatedEpisode: String
  def category: String
  def categoryType: Option[CategoryType]   // only really seems to be populated in program guide stuff
  def chanId: ChanId
  def startTime: MythDateTime
  def endTime: MythDateTime
  def seriesId: String
  def programId: String
  def stars: Option[Double]
  def originalAirDate: Option[LocalDate]
  def audioProps: AudioProperties
  def videoProps: VideoProperties
  def subtitleType: SubtitleType   // TODO do we only know this after recording?, in program table
  def partNumber: Option[Int]
  def partTotal: Option[Int]
  def programFlags: ProgramFlags

  def stereo: Boolean    = audioProps contains AudioProperties.Stereo
  def subtitled: Boolean = subtitleType != SubtitleType.Unknown

  def hdtv: Boolean =
    videoProps.containsAny(VideoProperties.Hdtv | VideoProperties.Hd720 | VideoProperties.Hd1080)

  def closeCaptioned: Boolean =
    (subtitleType contains SubtitleType.HardHear) ||
      (audioProps contains AudioProperties.HardHear)

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

trait Recordable extends Program {
  def findId: Int
  def hostname: String
  def sourceId: ListingSourceId
  def cardId: CaptureCardId
  def inputId: InputId
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
  def lastModified: MythDateTime
  def chanNum: ChannelNumber
  def callsign: String
  def chanName: String
  def outputFilters: String
  def parentId: Option[RecordRuleId]

  def recPriority2: Int            // TODO only surfaced by MythProtocol? Is it useful to be here?
}

trait Recording extends Recordable with InternetMetadata {
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
  case class RecordedIdInt(id: Int) extends RecordedId with IntegerIdentifier { def idString = id.toString }
  case class RecordedIdChanTime(chanId: ChanId, startTime: MythDateTime) extends RecordedId {
    def idString = chanId.id.toString + "_" + startTime.toMythFormat
  }
}
