package mythtv
package model

import java.time.{ LocalDate, Year }

import EnumTypes._
import util.{ ByteCount, MythDateTime }

trait ProgramAndVideoBase {   /// TODO need a better name for this trait.
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

trait Program extends ProgramAndVideoBase {
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
  def audioProps: AudioProperties  // TODO do we only know this after recording?, in program table
  def videoProps: VideoProperties  // TODO do we only know this after recording?, in program table
  def subtitleType: SubtitleType   // TODO do we only know this after recording?, in program table
  def partNumber: Option[Int]
  def partTotal: Option[Int]
  def programFlags: ProgramFlags

  def stereo: Boolean    = audioProps contains AudioProperties.Stereo
  def subtitled: Boolean = subtitleType != SubtitleType.Unknown
  def hdtv: Boolean      = videoProps contains VideoProperties.Hdtv  // TODO might need to check Hd1080 and Hd720 also
  def closeCaptioned: Boolean = ???  // Do we use audioProps or subtitleType (or both?)

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

  def recPriority2: Int            // TODO only surfaced by MythProtocol? Is it useful to be here?
  def parentId: Int                // TODO what is this? the parent RecordRuleId?
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

trait PreviousRecording {
  // TODO what's available here? a subset of Recording?
}
