package mythtv
package model

import java.time.{ LocalDate, Year }

import model._
import model.EnumTypes._
import util.{ ByteCount, DecimalByteCount, MythDateTime }

class DummyRecording extends Recording {
  def title             = ""
  def subtitle          = ""
  def description       = ""
  def year              = None
  def syndicatedEpisode = ""
  def category          = ""
  def categoryType      = None
  def chanId            = ChanId(0)
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
  def parentId          = 0
  def filename          = ""
  def filesize          = DecimalByteCount(0)
  def season            = 0
  def episode           = 0
  def inetRef           = ""
}
