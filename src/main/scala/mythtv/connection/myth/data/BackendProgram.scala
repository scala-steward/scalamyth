package mythtv
package connection
package myth
package data

import java.time.{ LocalDate, Year }

import scala.util.Try

import model._
import EnumTypes._
import util.{ ByteCount, DecimalByteCount, MythDateTime }

private[myth] class BackendProgram(data: Seq[String], fieldOrder: IndexedSeq[String])
    extends GenericBackendObject(data, fieldOrder) with Program with Recordable with Recording {

  override def toString: String = s"<BackendProgram $chanId, $startTime: $title>"

  private def optionalNonZeroIntField(f: String): Option[Int] =
    { Try(fields(f).toInt) filter (_ != 0) }.toOption

  private def timestampField(f: String): MythDateTime =
    MythDateTime.fromTimestamp(fields(f).toLong)

  /* Convenience accessors with proper type */

  def title: String = fields("title")
  def subtitle: String = fields("subtitle")
  def description: String = fields("description")
  def season: Int = fields("season").toInt
  def episode: Int = fields("episode").toInt
  def syndicatedEpisodeNumber: String = fields("syndicatedEpisodeNumber")
  def category: String = fields("category")
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
  def outputFilters: String = fields("outputFilters")  // TODO what type is this really?
  def seriesId: String = fields("seriesId")
  def programId: String = fields("programId")
  def inetRef: String = fields("inetRef")
  def lastModified: MythDateTime = timestampField("lastModified")
  def stars: Option[Double] = if (fields("stars") == "0") None else Try(fields("stars").toDouble).toOption
  def originalAirDate: Option[LocalDate] = Try(LocalDate.parse(fields("originalAirDate"))).toOption
  def playGroup: String = fields("playGroup")
  def recPriority2: Int = fields("recPriority2").toInt
  def parentId: Int = fields("parentId").toInt   // TODO is this an Int or String?
  def storageGroup: String = fields("storageGroup")
  def audioProps: AudioProperties = AudioProperties(fields("audioProps").toInt)
  def videoProps: VideoProperties = VideoProperties(fields("videoProps").toInt)
  def subtitleType: SubtitleType = SubtitleType(fields("subtitleType").toInt)
  def year: Option[Year] = optionalNonZeroIntField("year") map Year.of
  def partNumber: Option[Int] = optionalNonZeroIntField("partNumber")
  def partTotal: Option[Int] = optionalNonZeroIntField("partTotal")
}

private[myth] trait BackendProgramFactory extends GenericBackendObjectFactory[BackendProgram]
private[myth] trait ProgramOtherSerializer extends BackendTypeSerializer[Recording]

private[myth] object BackendProgram extends BackendProgramFactory with ProgramOtherSerializer {
  final val FIELD_ORDER = IndexedSeq(
    // TODO should this be a tuple or array or other such constant?
    //      Looks like a Vector wrapped around an array in bytecode
    "title",      "subtitle",        "description",  "season",       "episode",    "syndicatedEpisodeNumber",
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
      += in.syndicatedEpisodeNumber
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
