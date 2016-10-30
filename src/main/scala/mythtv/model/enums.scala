package mythtv
package model

import util.{ BitmaskEnum, LooseEnum }

/* TODO create some sort of equivalent of Java's EnumSet for storing bit flag enums */

object Markup extends LooseEnum {
  type Markup = Value
  val All           = Value(-100)
  val Unset         = Value(-10)
  val TmpCutEnd     = Value(-5)
  val TmpCutStart   = Value(-4)
  val UpdatedCut    = Value(-3)
  val Placeholder   = Value(-2)
  val CutEnd        = Value(0)
  val CutStart      = Value(1)
  val Bookmark      = Value(2)
  val BlankFrame    = Value(3)
  val CommStart     = Value(4)
  val CommEnd       = Value(5)
  val GopStart      = Value(6)
  val KeyFrame      = Value(7)
  val SceneChange   = Value(8)
  val GopByFrame    = Value(9)
  @deprecated("", "")
  val Aspect1x1     = Value(10)
  val Aspect4x3     = Value(11)
  val Aspect16x9    = Value(12)
  val Aspect221x1   = Value(13)
  val AspectCustom  = Value(14)
  @deprecated("", "")
  val VideoWidthOld = Value(15)
  val VideoWidth    = Value(30)
  val VideoHeight   = Value(31)
  val VideoRate     = Value(32)
  val DurationMs    = Value(33)
  val TotalFrame    = Value(34)
}

// TODO add new RecType names, deprecate old names
object RecType extends LooseEnum {
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

object RecSearchType extends LooseEnum {
  type RecSearchType = Value
  val NoSearch      = Value(0)
  val PowerSearch   = Value(1)
  val TitleSearch   = Value(2)
  val KeywordSearch = Value(3)
  val PeopleSearch  = Value(4)
  val ManualSearch  = Value(5)
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

object AudioProperties extends BitmaskEnum[Int] {
  type AudioProperties = Base
  val Unknown      =  Mask(0x00)
  val Stereo       = Value(0x01)
  val Mono         = Value(0x02)
  val Surround     = Value(0x04)
  val Dolby        = Value(0x08)
  val HardHear     = Value(0x10)
  val VisualImpair = Value(0x20)
}

object VideoProperties extends BitmaskEnum[Int] {
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

object SubtitleType extends BitmaskEnum[Int] {
  type SubtitleType = Base
  val Unknown  =  Mask(0x00)
  val HardHear = Value(0x01)
  val Normal   = Value(0x02)
  val OnScreen = Value(0x04)
  val Signed   = Value(0x08)
}

object CategoryType extends LooseEnum {
  type CategoryType = Value
  val None     = Value(0)
  val Movie    = Value(1, "movie")
  val Series   = Value(2, "series")
  val Sports   = Value(3, "sports")
  val TvShow   = Value(4, "tvshow")
}

object ProgramFlags extends BitmaskEnum[Int] {
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

object DupCheckIn extends BitmaskEnum[Int] {
  type DupCheckIn = Base
  val Recorded      = Value(0x01)
  val OldRecorded   = Value(0x02)
  val All           =  Mask(0x0f)
  val NewEpisodes   = Value(0x10)  // this should always be combined with DupsInAll ??
}

object DupCheckMethod extends BitmaskEnum[Int] {
  type DupCheckMethod = Base
  val None             = Value(0x01)
  val Subtitle         = Value(0x02)
  val Description      = Value(0x04)
  val SubtitleDesc     =  Mask(0x06)
  val SubtitleThenDesc = Value(0x08) // subtitle, then description
}

/* as set in the commmethod field of the channel table in the database */
object ChannelCommDetectMethod extends LooseEnum {
  type ChannelCommDetectMethod = Value
  val CommFree      = Value(-2)
  val Uninitialized = Value(-1)
}

object JobStatus extends LooseEnum {
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

object JobType extends LooseEnum {
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

object JobCommand extends LooseEnum {
  type JobCommand = Value
  val Run     = Value(0x0000)
  val Pause   = Value(0x0001)
  val Resume  = Value(0x0002)
  val Stop    = Value(0x0004)
  val Restart = Value(0x0008)
}

object JobFlags extends BitmaskEnum[Int] {
  type JobFlags = Base
  val None       =  Mask(0x0000)
  val UseCutlist = Value(0x0001)
  val LiveRec    = Value(0x0002)
  val External   = Value(0x0004)
  val Rebuild    = Value(0x0008)
}

object ListingSourceType extends LooseEnum {
  type ListingSourceType = Value
  val EIT             = Value(0x01)
  val SchedulesDirect = Value(0x02)
  val XMLTV           = Value(0x04)
  val DBOX2EPG        = Value(0x08)
}

object PictureAdjustType extends LooseEnum {
  type PictureAdjustType = Value
  val None      = Value(0)
  val Playback  = Value(1)
  val Channel   = Value(2)
  val Recording = Value(3)
}

object ChannelBrowseDirection extends LooseEnum {
  type ChannelBrowseDirection = Value
  val Invalid   = Value(-1)
  val Same      = Value(0)  // Current channel and time
  val Up        = Value(1)  // Previous channel
  val Down      = Value(2)  // Next channel
  val Left      = Value(3)  // Current channel in the past
  val Right     = Value(4)  // Current channel ni the future
  val Favorite  = Value(5)  // Next favorite channel
}

object ChannelChangeDirection extends LooseEnum {
  type ChannelChangeDirection = Value
  val Up        = Value(0)
  val Down      = Value(1)
  val Favorite  = Value(2)
  val Same      = Value(3)
}

object SleepStatus extends LooseEnum {
  type SleepStatus = Value
  val Awake         = Value(0x0)
  val Asleep        = Value(0x1)
  val FallingAsleep = Value(0x3)
  val Waking        = Value(0x5)
  val Undefined     = Value(0x8)
}

object TvState extends LooseEnum {
  type TvState = Value
  val Error               = Value(-1)
  val None                = Value(0)
  val WatchingLiveTv      = Value(1)
  val WatchingPreRecorded = Value(2)
  val WatchingVideo       = Value(3)
  val WatchingDvd         = Value(4)
  val WatchingBd          = Value(5)
  val WatchingRecording   = Value(6)
  val RecordingOnly       = Value(7)
  val ChangingState       = Value(8)
}
