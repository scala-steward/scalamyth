package mythtv
package connection
package myth
package data

import java.time.{ LocalDate, Year }

import scala.util.Try

import model._
import EnumTypes._
import RecordedId._
import util.{ ByteCount, DecimalByteCount, MythDateTime }

private[myth] class BackendProgram(data: Seq[String], fieldOrder: IndexedSeq[String])
    extends GenericBackendObject(data, fieldOrder) with Program with Recordable with Recording {

  override def toString: String = s"<BackendProgram $chanId, $startTime: $combinedTitle>"

  private def findRecordedId: RecordedId = {
    if (fields contains "recordedId") RecordedIdInt(fields("recordedId").toInt)
    else if (filename.isEmpty)        RecordedId.empty
    else                              RecordedIdChanTime(chanId, recStartTS)
  }

  private def optionalNonZeroIntField(f: String): Option[Int] =
    { Try(fields(f).toInt) filter (_ != 0) }.toOption

  private def optionalNonEmptyStringField(f: String): Option[String] = {
    val s = fields(f)
    if (s.isEmpty) None else Some(s)
  }

  private def timestampField(f: String): MythDateTime =
    MythDateTime.fromTimestamp(fields(f).toLong)

  /* Convenience accessors with proper type */

  def title: String = fields("title")
  def subtitle: String = fields("subtitle")
  def description: String = fields("description")
  def season: Option[Int] = optionalNonZeroIntField("season")
  def episode: Option[Int] = optionalNonZeroIntField("episode")
  def totalEpisodes: Option[Int] = optionalNonZeroIntField("totalEpisodes")
  def syndicatedEpisode: String = fields.getOrElse("syndicatedEpisode", "")
  def category: String = fields("category")
  def categoryType: Option[CategoryType] = None        // not included in myth protocol serialization (until ver 79?)
  def chanId: ChanId = ChanId(fields("chanId").toInt)
  def chanNum: ChannelNumber = ChannelNumber(fields("chanNum"))
  def callsign: String = fields("callsign")
  def chanName: String = fields("chanName")
  def filename: String = fields("filename")
  def filesize: ByteCount = DecimalByteCount(fields("filesize").toLong)
  def startTime: MythDateTime = timestampField("startTime")
  def endTime: MythDateTime = timestampField("endTime")
  def findId: Int = fields("findId").toInt
  def hostname: String = fields("hostname")
  def sourceId: ListingSourceId = ListingSourceId(fields("sourceId").toInt)
  def cardId: CaptureCardId = CaptureCardId(fields("cardId").toInt)
  def inputId: InputId = InputId(fields("inputId").toInt)
  def recPriority: Int = fields("recPriority").toInt
  def recStatus: RecStatus = RecStatus.applyOrUnknown(fields("recStatus").toInt)
  def recordId: RecordRuleId = RecordRuleId(fields("recordId").toInt)
  def recType: RecType = RecType.applyOrUnknown(fields("recType").toInt)
  def dupIn: DupCheckIn = DupCheckIn(fields("dupIn").toInt)
  def dupMethod: DupCheckMethod = DupCheckMethod(fields("dupMethod").toInt)
  def recStartTS: MythDateTime = timestampField("recStartTS")
  def recEndTS: MythDateTime = timestampField("recEndTS")
  def programFlags: ProgramFlags = ProgramFlags(fields("programFlags").toInt)
  def recGroup: String = fields("recGroup")
  def outputFilters: String = fields("outputFilters")
  def seriesId: String = fields("seriesId")
  def programId: String = fields("programId")
  def inetRef: Option[String] = optionalNonEmptyStringField("inetRef")
  def lastModified: MythDateTime = timestampField("lastModified")
  def stars: Option[Double] = if (fields("stars") == "0") None else Try(fields("stars").toDouble).toOption
  def originalAirDate: Option[LocalDate] = Try(LocalDate.parse(fields("originalAirDate"))).toOption
  def playGroup: String = fields("playGroup")
  def recPriority2: Int = fields("recPriority2").toInt
  def parentId: Option[RecordRuleId] = optionalNonZeroIntField("parentId") map RecordRuleId
  def storageGroup: String = fields("storageGroup")
  def audioProps: AudioProperties = AudioProperties(fields("audioProps").toInt)
  def videoProps: VideoProperties = VideoProperties(fields("videoProps").toInt)
  def subtitleType: SubtitleType = SubtitleType(fields("subtitleType").toInt)
  def year: Option[Year] = optionalNonZeroIntField("year") map Year.of
  def partNumber: Option[Int] = optionalNonZeroIntField("partNumber")
  def partTotal: Option[Int] = optionalNonZeroIntField("partTotal")
  def recordedId: RecordedId = findRecordedId
}

private[myth] trait BackendProgramFactory extends GenericBackendObjectFactory[BackendProgram]
private[myth] trait ProgramOtherSerializer extends BackendTypeSerializer[Recording]

private[myth] object BackendProgram75 extends BackendProgramFactory with ProgramOtherSerializer {
  final val FIELD_ORDER = IndexedSeq(
    "title",      "subtitle",        "description",  "season",       "episode",
    "category",   "chanId",          "chanNum",      "callsign",     "chanName",   "filename",
    "filesize",   "startTime",       "endTime",      "findId",       "hostname",   "sourceId",
    "cardId",     "inputId",         "recPriority",  "recStatus",    "recordId",   "recType",
    "dupIn",      "dupMethod",       "recStartTS",   "recEndTS",     "programFlags",
    "recGroup",   "outputFilters",   "seriesId",     "programId",    "inetRef",    "lastModified",
    "stars",      "originalAirDate", "playGroup",    "recPriority2", "parentId",   "storageGroup",
    "audioProps", "videoProps",      "subtitleType", "year"
  )

  def apply(data: Seq[String]): BackendProgram = new BackendProgram(data, FIELD_ORDER)

  def serialize(in: Recording): String = serialize(in, new StringBuilder).toString
  def serialize(in: Recording, sb: StringBuilder): StringBuilder = {
    (new BackendSerializationBuilder(sb)
      += in.title
      += in.subtitle
      += in.description
      += in.season
      += in.episode
      += in.category
      += in.chanId
      += in.chanNum
      += in.callsign
      += in.chanName
      += in.filename
      += in.filesize.bytes
      += in.startTime
      += in.endTime
      += in.findId
      += in.hostname
      += in.sourceId
      += in.cardId
      += in.inputId
      += in.recPriority
      += in.recStatus
      += in.recordId
      += in.recType
      += in.dupIn
      += in.dupMethod
      += in.recStartTS
      += in.recEndTS
      += in.programFlags
      += in.recGroup
      += in.outputFilters
      += in.seriesId
      += in.programId
      += in.inetRef
      += in.lastModified
      += in.stars
      += in.originalAirDate
      += in.playGroup
      += in.recPriority2
      += in.parentId
      += in.storageGroup
      += in.audioProps
      += in.videoProps
      += in.subtitleType
      += in.year
      ).result
  }
}

private[myth] object BackendProgram77 extends BackendProgramFactory with ProgramOtherSerializer {
  final val FIELD_ORDER = IndexedSeq(
    "title",      "subtitle",        "description",  "season",       "episode",    "syndicatedEpisode",
    "category",   "chanId",          "chanNum",      "callsign",     "chanName",   "filename",
    "filesize",   "startTime",       "endTime",      "findId",       "hostname",   "sourceId",
    "cardId",     "inputId",         "recPriority",  "recStatus",    "recordId",   "recType",
    "dupIn",      "dupMethod",       "recStartTS",   "recEndTS",     "programFlags",
    "recGroup",   "outputFilters",   "seriesId",     "programId",    "inetRef",    "lastModified",
    "stars",      "originalAirDate", "playGroup",    "recPriority2", "parentId",   "storageGroup",
    "audioProps", "videoProps",      "subtitleType", "year",         "partNumber", "partTotal"
  )

  def apply(data: Seq[String]): BackendProgram = new BackendProgram(data, FIELD_ORDER)

  def serialize(in: Recording): String = serialize(in, new StringBuilder).toString
  def serialize(in: Recording, sb: StringBuilder): StringBuilder = {
    (new BackendSerializationBuilder(sb)
      += in.title
      += in.subtitle
      += in.description
      += in.season
      += in.episode
      += in.syndicatedEpisode
      += in.category
      += in.chanId
      += in.chanNum
      += in.callsign
      += in.chanName
      += in.filename
      += in.filesize.bytes
      += in.startTime
      += in.endTime
      += in.findId
      += in.hostname
      += in.sourceId
      += in.cardId
      += in.inputId
      += in.recPriority
      += in.recStatus
      += in.recordId
      += in.recType
      += in.dupIn
      += in.dupMethod
      += in.recStartTS
      += in.recEndTS
      += in.programFlags
      += in.recGroup
      += in.outputFilters
      += in.seriesId
      += in.programId
      += in.inetRef
      += in.lastModified
      += in.stars
      += in.originalAirDate
      += in.playGroup
      += in.recPriority2
      += in.parentId
      += in.storageGroup
      += in.audioProps
      += in.videoProps
      += in.subtitleType
      += in.year
      += in.partNumber
      += in.partTotal
    ).result
  }
}

private[myth] object BackendProgram88 extends BackendProgramFactory with ProgramOtherSerializer {
  // NB the cardId and inputId fields now refer to the same underlying object on the backend
  final val FIELD_ORDER = IndexedSeq(
    "title",      "subtitle",        "description",  "season",       "episode",    "totalEpisodes", "syndicatedEpisode",
    "category",   "chanId",          "chanNum",      "callsign",     "chanName",   "filename",
    "filesize",   "startTime",       "endTime",      "findId",       "hostname",   "sourceId",
    "cardId",     "inputId",         "recPriority",  "recStatus",    "recordId",   "recType",
    "dupIn",      "dupMethod",       "recStartTS",   "recEndTS",     "programFlags",
    "recGroup",   "outputFilters",   "seriesId",     "programId",    "inetRef",    "lastModified",
    "stars",      "originalAirDate", "playGroup",    "recPriority2", "parentId",   "storageGroup",
    "audioProps", "videoProps",      "subtitleType", "year",         "partNumber", "partTotal",
    "categoryType", "recordedId",    "inputName",    "bookmarkUpdate"
  )

  def apply(data: Seq[String]): BackendProgram = new BackendProgram(data, FIELD_ORDER)

  def serialize(in: Recording): String = serialize(in, new StringBuilder).toString
  def serialize(in: Recording, sb: StringBuilder): StringBuilder = {
    (new BackendSerializationBuilder(sb)
      += in.title
      += in.subtitle
      += in.description
      += in.season
      += in.episode
      += in.totalEpisodes
      += in.syndicatedEpisode
      += in.category
      += in.chanId
      += in.chanNum
      += in.callsign
      += in.chanName
      += in.filename
      += in.filesize.bytes
      += in.startTime
      += in.endTime
      += in.findId
      += in.hostname
      += in.sourceId
      += in.cardId   // should be the same as inputId now
      += in.inputId
      += in.recPriority
      += in.recStatus
      += in.recordId
      += in.recType
      += in.dupIn
      += in.dupMethod
      += in.recStartTS
      += in.recEndTS
      += in.programFlags
      += in.recGroup
      += in.outputFilters
      += in.seriesId
      += in.programId
      += in.inetRef
      += in.lastModified
      += in.stars
      += in.originalAirDate
      += in.playGroup
      += in.recPriority2
      += in.parentId
      += in.storageGroup
      += in.audioProps
      += in.videoProps
      += in.subtitleType
      += in.year
      += in.partNumber
      += in.partTotal
      += in.categoryType.getOrElse(CategoryType.None)
      += in.recordedId
      += ""  // TODO inputName
      += ""  // TODO bookmarkUpdateTime
    ).result
  }
}
