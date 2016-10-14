package mythtv
package model

/* TODO: can I create a "LooseEnum" class that will allow creation
    of instances with unknown mapping (and preserve original int value? */

/* TODO create some sort of equivalent of Java's EnumSet for storing bit flag enums */

object Markup extends Enumeration {
  type Markup = Value
  val MARK_ALL           = Value(-100)
  val MARK_UNSET         = Value(-10)
  val MARK_TMP_CUT_END   = Value(-5)
  val MARK_TMP_CUT_START = Value(-4)
  val MARK_UPDATED_CUT   = Value(-3)
  val MARK_PLACEHOLDER   = Value(-2)
  val MARK_CUT_END       = Value(0)
  val MARK_CUT_START     = Value(1)
  val MARK_BOOKMARK      = Value(2)
  val MARK_BLANK_FRAME   = Value(3)
  val MARK_COMM_START    = Value(4)
  val MARK_COMM_END      = Value(5)
  val MARK_GOP_START     = Value(6)
  val MARK_KEYFRAME      = Value(7)
  val MARK_SCENE_CHANGE  = Value(8)
  val MARK_GOP_BYFRAME   = Value(9)
  val MARK_ASPECT_1_1    = Value(10)
  val MARK_ASPECT_4_3    = Value(11)
  val MARK_ASPECT_16_9   = Value(12)
  val MARK_ASPECT_2_21_1 = Value(13)
  val MARK_ASPECT_CUSTOM = Value(14)
  val MARK_VIDEO_WIDTHold= Value(15)
  val MARK_VIDEO_WIDTH   = Value(30)
  val MARK_VIDEO_HEIGHT  = Value(31)
  val MARK_VIDEO_RATE    = Value(32)
  val MARK_DURATION_MS   = Value(33)
  val MARK_TOTAL_FRAMES  = Value(34)
}

object RecType extends Enumeration {
  type RecType = Value
  val NotRecording     = Value(0)
  val SingleRecord     = Value(1)
  val TimeslotRecord   = Value(2)
  val ChannelRecord    = Value(3)
  val AllRecord        = Value(4)
  val WeekslotRecord   = Value(5)
  val FindOneRecord    = Value(6)
  val OverrideRecord   = Value(7)
  val DontRecord       = Value(8)
  val FindDailyRecord  = Value(9)
  val FindWeeklyRecord = Value(10)
  val TemplateRecord   = Value(11)
}

object RecSearchType extends Enumeration {
  type RecSearchType = Value
  val NoSearch      = Value(0)
  val PowerSearch   = Value(1)
  val TitleSearch   = Value(2)
  val KeywordSearch = Value(3)
  val PeopleSearch  = Value(4)
  val ManualSearch  = Value(5)
}

object RecStatus extends Enumeration {
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


object AudioProperties extends BitmaskEnum {
  type AudioProperties = Value
  val Unknown      = Value(0x00)
  val Stereo       = Value(0x01)
  val Mono         = Value(0x02)
  val Surround     = Value(0x03)
  val Dolby        = Value(0x04)
  val HardHear     = Value(0x10)
  val VisualImpair = Value(0x20)
}

object VideoProperties extends BitmaskEnum {
  type VideoProperties = Value
  val Unknown    = Value(0x00)
  val Hdtv       = Value(0x01)
  val Widescreen = Value(0x02)
  val AVC        = Value(0x04)
  val Hd720      = Value(0x08)
  val Hd1080     = Value(0x10)
}

object SubtitleType extends BitmaskEnum {
  type SubtitleType = Value
  val Unknown  = Value(0x00)
  val HardHear = Value(0x01)
  val Normal   = Value(0x02)
  val OnScreen = Value(0x04)
  val Signed   = Value(0x08)
}

object DupCheckIn extends BitmaskEnum {
  type DupCheckIn = Value
  val DupsInRecorded    = Value(0x01)
  val DupsInOldRecorded = Value(0x02)
  val DupsInAll         = Value(0x0f)  // this is a mask/combined value
  val DupsNewEpisodes   = Value(0x10)  // this should always be combined with DupsInAll ??
}

object DupCheckMethod extends BitmaskEnum {
  type DupCheckMethod = Value
  val DupCheckNone        = Value(0x01)
  val DupCheckSubtitle    = Value(0x02)
  val DupCheckDescription = Value(0x04)
  val DupCheckSubDesc     = Value(0x06) // this is a mask/combined value
  val DupCheckSubThenDesc = Value(0x08) // subtitle, then description
}

object JobStatus extends Enumeration {
  type JobStatus = Value
  val Unknown     = Value(0x0000)
  val Queued      = Value(0x0001)
  val Pending     = Value(0x0002)
  val Starting    = Value(0x0003)
  val Running     = Value(0x0004)
  val Stopping    = Value(0x0005)
  val Paused      = Value(0x0006)
  val Retry       = Value(0x0007)
  val Erroring    = Value(0x0008)
  val Aborting    = Value(0x0009)
  val Finished    = Value(0x0110)
  val Aborted     = Value(0x0120)
  val Errored     = Value(0x0130)
  val Cancelled   = Value(0x0140)

  def isDone(status: JobStatus): Boolean = new RichJobStatus(status).isDone

  implicit class RichJobStatus(val status: JobStatus) extends AnyVal {
    def isDone: Boolean = (status.id & 0x100) != 0
  }
}

object JobType extends Enumeration {
  type JobType = Value
  val None      = Value(0x0000)
  val Transcode = Value(0x0001)
  val CommFlag  = Value(0x0002)
  val Metadata  = Value(0x0004)
  val UserJob1  = Value(0x0100)
  val UserJob2  = Value(0x0200)
  val UserJob3  = Value(0x0400)
  val UserJob4  = Value(0x0800)

  def isSystem(jt: JobType): Boolean = new RichJobType(jt).isSystem
  def isUser(jt: JobType): Boolean   = new RichJobType(jt).isUser

  implicit class RichJobType(val jobtype: JobType) extends AnyVal {
    def isSystem: Boolean = (jobtype.id & 0x00ff) != 0
    def isUser: Boolean   = (jobtype.id & 0xff00) != 0
  }
}

object JobCommand extends Enumeration {
  type JobCommand = Value
  val Run     = Value(0x0000)
  val Pause   = Value(0x0001)
  val Resume  = Value(0x0002)
  val Stop    = Value(0x0004)
  val Restart = Value(0x0008)
}

object JobFlags extends BitmaskEnum {
  type JobFlags = Value
  val None       = Value(0x0000)
  val UseCutlist = Value(0x0001)
  val LiveRec    = Value(0x0002)
  val External   = Value(0x0004)
  val Rebuild    = Value(0x0008)
}

object ListingSourceType extends Enumeration {
  type ListingSourceType = Value
  val EIT             = Value(0x01)
  val SchedulesDirect = Value(0x02)
  val XMLTV           = Value(0x04)
  val DBOX2EPG        = Value(0x08)
}

object PictureAdjustType extends Enumeration {
  type PictureAdjustType = Value
  val None      = Value(0)
  val Playback  = Value(1)
  val Channel   = Value(2)
  val Recording = Value(3)
}

object ChannelBrowseDirection extends Enumeration {
  type ChannelBrowseDirection = Value
  val Invalid   = Value(-1)
  val Same      = Value(0)  // Current channel and time
  val Up        = Value(1)  // Previous channel
  val Down      = Value(2)  // Next channel
  val Left      = Value(3)  // Current channel in the past
  val Right     = Value(4)  // Current channel ni the future
  val Favorite  = Value(5)  // Next favorite channel
}

object ChannelChangeDirection extends Enumeration {
  type ChannelChangeDirection = Value
  val Up        = Value(0)
  val Down      = Value(1)
  val Favorite  = Value(2)
  val Same      = Value(3)
}

/*
package object enums {
  type Markup = mythtv.Markup.Value
  type RecType = mythtv.RecType.Value
  type RecSearchType = mythtv.RecSearchType.Value
  type RecStatus = mythtv.RecStatus.Value
  type JobStatus = mythtv.JobStatus.Value
}
 */
