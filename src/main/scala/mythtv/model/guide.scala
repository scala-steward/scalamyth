package mythtv
package model

import EnumTypes.{ ListingSourceType, RecStatus }
import util.MythDateTime

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
