package mythtv
package model

import java.time.{ DayOfWeek, LocalTime }

import EnumTypes._
import util.{ IntBitmaskEnum, LooseEnum, MythDateTime }

final case class RecordRuleId(id: Int) extends AnyVal with IntegerIdentifier

// TODO seems like this contains most of the elements of ProgramGuideEntry or Recordable or some such...
trait RecordRule {
  def id: RecordRuleId
  def recType: RecType
  def chanId: Option[ChanId]
  def startTime: MythDateTime
  def endTime: MythDateTime
  def title: String
  def subtitle: String
  def description: String
  def season: Option[Int]
  def episode: Option[Int]
  def category: String
  def recProfile: String
  def recPriority: Int
  def autoExpire: Boolean
  def maxEpisodes: Int
  def maxNewest: Boolean
  def startOffset: Int   // units: minutes
  def endOffset: Int     // units: minutes
  def recGroup: String
  def dupMethod: DupCheckMethod
  def dupIn: DupCheckIn
  def callsign: String   // NB called "station" in the record DB table
  def seriesId: Option[String]
  def programId: Option[String]
  def inetRef: Option[String]
  def searchType: RecSearchType
  def autoTranscode: Boolean
  def autoCommFlag: Boolean
  def autoUserJob1: Boolean
  def autoUserJob2: Boolean
  def autoUserJob3: Boolean
  def autoUserJob4: Boolean
  def autoMetadata: Boolean
  def findDay: Option[DayOfWeek]
  def findTime: Option[LocalTime]
  def inactive: Boolean
  def parentId: Option[RecordRuleId]
  def transcoder: Option[Int]   // TODO what type is this? (ugh, see what I do here in mythjango...recordingprofiles+profilegroups tables)
  def playGroup: String
  def preferredInput: Option[InputId]
  def nextRecord: Option[MythDateTime]
  def lastRecord: Option[MythDateTime]
  def lastDelete: Option[MythDateTime]
  def storageGroup: String
  def averageDelay: Int   // average number of hours until recording is deleted? in range [1, 200]
  def filters: Option[Int] // TODO what type is this? bitmask enum of data from the recordfilter table?

  override def toString: String = s"<RecordRule $id $title>"
}

object RecordRule {
  def apply(host: String, ruleId: RecordRuleId): RecordRule = {
    import services.ServiceProvider

    // FIXME acquisition via service is imperfect; some fields are lost/misrepresented:
    //   e.g. rectype loses TemplateRecord (goes to NotRecording)

    val dvr = ServiceProvider.dvrService(host)
    dvr.getRecordSchedule(ruleId) match {
      case Right(rule) => rule
      case Left(_) => throw new NoSuchElementException
    }
  }

  // TODO build our own default rather than throw an exception if we fail to discover the default template
  def default(host: String): RecordRule = template(host, "Default").get

  def template(host: String, template: String): Option[RecordRule] = {
    templateRuleId(host, template) map (apply(host, _))
  }

  def templateRuleId(host: String, template: String): Option[RecordRuleId] = {
    // Services API (at least in 0.27) doesn't provide TemplateRecord as a RecType
    import connection.myth.MythProtocolAPIConnection

    val api = MythProtocolAPIConnection(host)
    api.announce("Monitor", "recruletemplatecheck")

    val templates = api.queryGetAllScheduled map (_ filter { rule =>
      rule.recType == RecType.TemplateRecord && (
        rule.title == template || rule.title == template + " (Template)")
    })

    api.close()

    templates.toOption flatMap { it =>
      if (it.hasNext) Some(it.next().recordId)
      else            None
    }
  }
}

trait RecRuleFilter {   // this is a "dynamic enum", defined in the database
  def id: Int
  def name: String

  override def toString = s"<RecRuleFilter $id $name>"
}

object DupCheckIn extends IntBitmaskEnum /* with EnumWithDescription[DupCheckIn#Base] */ {
  type DupCheckIn = Base
  val Recorded      = Value(0x01)
  val OldRecorded   = Value(0x02)
  val All           =  Mask(0x0f)
  val NewEpisodes   = Value(0x10)  // this should always be combined with DupsInAll ??

  private val id2Description: Map[Base, String] = Map(
    DupCheckIn.Recorded    -> "Current Recordings",
    DupCheckIn.OldRecorded -> "Previous Recordings",
    DupCheckIn.All         -> "All Recordings",
    DupCheckIn.NewEpisodes -> "New Episodes Only"
  )

  private lazy val description2Id: Map[String, Base] = id2Description map (_.swap)

  def description(value: Base): String = id2Description(value)
  def withDescription(description: String): Base = description2Id(description)
}

object DupCheckMethod extends IntBitmaskEnum /* with EnumWithDescription[DupCheckMethod#Base] */ {
  type DupCheckMethod = Base
  val None             = Value(0x01)
  val Subtitle         = Value(0x02)
  val Description      = Value(0x04)
  val SubtitleDesc     =  Mask(0x06)
  val SubtitleThenDesc = Value(0x08) // subtitle, then description

  private val id2Description: Map[DupCheckMethod, String] = Map(
    DupCheckMethod.None             -> "None",
    DupCheckMethod.Subtitle         -> "Subtitle",
    DupCheckMethod.Description      -> "Description",
    DupCheckMethod.SubtitleDesc     -> "Subtitle and Description",
    DupCheckMethod.SubtitleThenDesc -> "Subtitle then Description"
  )

  private lazy val description2Id: Map[String, Base] = id2Description map (_.swap)

  def description(value: Base): String = id2Description(value)
  def withDescription(description: String): Base = description2Id(description)
}

object RecSearchType extends LooseEnum /* with EnumWithDescription[RecSearchType#Value] */ {
  type RecSearchType = Value
  val NoSearch      = Value(0)
  val PowerSearch   = Value(1)
  val TitleSearch   = Value(2)
  val KeywordSearch = Value(3)
  val PeopleSearch  = Value(4)
  val ManualSearch  = Value(5)

  private val id2Description: Map[Value, String] = Map(
    RecSearchType.NoSearch      -> "None",
    RecSearchType.PowerSearch   -> "Power Search",
    RecSearchType.TitleSearch   -> "Title Search",
    RecSearchType.KeywordSearch -> "Keyword Search",
    RecSearchType.PeopleSearch  -> "People Search",
    RecSearchType.ManualSearch  -> "Manual Search"
  )
  private lazy val description2Id: Map[String, Value] = id2Description map (_.swap)

  def description(value: Value): String = id2Description(value)
  def withDescription(description: String): Value = description2Id(description)
}

object RecType extends LooseEnum /* with EnumWithDescription[RecType#Value] */ {
  type RecType = Value
  val NotRecording     = Value(0)
  val SingleRecord     = Value(1)
  val DailyRecord      = Value(2)
  @deprecated("use DailyRecord + 'this time' filter", "MythTV 0.27")
  val TimeslotRecord   = DailyRecord
  @deprecated("use 'this channel' filter", "MythTV 0.27")
  val ChannelRecord    = Value(3)
  val AllRecord        = Value(4)
  val WeeklyRecord     = Value(5)
  @deprecated("use WeeklyRecord + 'this day and time' filter", "MythTV 0.27")
  val WeekslotRecord   = WeeklyRecord
  val OneRecord        = Value(6)
  @deprecated("", "")
  val FindOneRecord    = OneRecord
  val OverrideRecord   = Value(7)
  val DontRecord       = Value(8)
  @deprecated("", "MythTV 0.27")
  val FindDailyRecord  = Value(9)
  @deprecated("", "MythTV 0.27")
  val FindWeeklyRecord = Value(10)
  val TemplateRecord   = Value(11)

  // TODO this description mapping is tricker because the map may have duplicate keys!
  //   (see libs/libmyth/recordingtypes.cpp: toRawString(RecordingType)
  // This also means there may be a loss of precision between a record rule
  // RecType and how it is described in the services API representation.
  // TODO this mapping changed between Myth versions (0.26 -> 0.27?)
  private val id2Description: Map[Value, String] = Map(
    RecType.NotRecording   -> "Not Recording",
    RecType.SingleRecord   -> "Single Record",
    RecType.AllRecord      -> "Record All",
    RecType.OneRecord      -> "Record One",
    RecType.DailyRecord    -> "Record Daily",
    RecType.WeeklyRecord   -> "Record Weekly",
    RecType.OverrideRecord -> "Override Recording",
    RecType.DontRecord     -> "Do not Record",
    RecType.TemplateRecord -> "Recording Template"
  )

  private lazy val description2Id: Map[String, Value] = id2Description map (_.swap)

  def description(value: Value): String = id2Description(value)
  def withDescription(description: String): Value = description2Id(description)
}
