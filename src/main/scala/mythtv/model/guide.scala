package mythtv
package model

import util.MythDateTime
import EnumTypes.ListingSourceType

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

  // These fields from the 'program' DB table are not directly present in Program object
  /*
  def stereo: Boolean
  def subtitled: Boolean
  def hdtv: Boolean
  def closeCaptioned: Boolean
  */

  def titlePronounce: String    // only populated if lang="ja_JP@kana" on a program listing
  def previouslyShown: Boolean  // captured in ProgramFlags.Repeat in Program
  def showType: String
  def colorCode: String
  def manualId: RecordRuleId    // used by manual search recording rules?
  def listingSource: ListingSourceType

  // { generic, first, last } are computed by mythfilldatabase

  def generic: Boolean  // generic episode info for this program, rather than a particular episode
  def first: Boolean    // first showing of this episode (in current listings window)
  def last: Boolean     // last showing of this episode (in current listings window)
}
