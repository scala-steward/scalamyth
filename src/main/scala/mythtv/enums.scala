package mythtv

/* TODO: can I create a "LooseEnum" class that will allow creation
    of instances with unknown mapping (and preserve original int value? */

/* TODO create some sort of equivalent of Java's EnumSet for storing bit flag enums */

object Markup extends Enumeration {
  type Markup = Value
  val MARK_UNSET         = Value(-10)
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
  val MARK_VIDEO_WIDTH   = Value(15)
  val MARK_VIDEO_HEIGHT  = Value(31)
  val MARK_VIDEO_RATE    = Value(32)
  val MARK_DURATION_MS   = Value(33)
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

  def isDone(status: Value): Boolean = (status.id & 0x100) != 0
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
