package mythtv
package model

import RecordedId._
import util.{ DecimalByteCount, MythDateTime }

class DummyRecording extends Recording {
  def title             = ""
  def subtitle          = ""
  def description       = ""
  def year              = None
  def syndicatedEpisode = ""
  def category          = ""
  def categoryType      = None
  def chanId            = ChanId.empty
  def startTime         = MythDateTime.fromTimestamp(0)
  def endTime           = MythDateTime.fromTimestamp(0)
  def seriesId          = ""
  def programId         = ""
  def stars             = None
  def originalAirDate   = None
  def audioProps        = AudioProperties.Unknown
  def videoProps        = VideoProperties.Unknown
  def subtitleType      = SubtitleType.Unknown
  def partNumber        = None
  def partTotal         = None
  def programFlags      = ProgramFlags.None
  def findId            = 0
  def hostname          = ""
  def sourceId          = ListingSourceId(0)
  def cardId            = CaptureCardId(0)
  def inputId           = InputId(0)
  def recPriority       = 0
  def recStatus         = RecStatus.Unknown
  def recordId          = RecordRuleId(0)
  def recType           = RecType.NotRecording
  def dupIn             = DupCheckIn.All
  def dupMethod         = DupCheckMethod.None
  def recStartTS        = MythDateTime.fromTimestamp(0)
  def recEndTS          = MythDateTime.fromTimestamp(0)
  def recGroup          = "Default"
  def storageGroup      = "Default"
  def playGroup         = "Default"
  def lastModified      = MythDateTime.fromTimestamp(0)
  def chanNum           = ChannelNumber("")
  def callsign          = ""
  def chanName          = ""
  def outputFilters     = ""
  def recPriority2      = 0
  def parentId          = None
  def filename          = ""
  def filesize          = DecimalByteCount(0)
  def season            = None
  def episode           = None
  def totalEpisodes     = None
  def inetRef           = None
  def recordedId        = RecordedId.empty
}
