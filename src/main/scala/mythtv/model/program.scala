package mythtv
package model

import java.time.{ LocalDate, Year }

import EnumTypes._
import util.{ ByteCount, MythDateTime }

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
