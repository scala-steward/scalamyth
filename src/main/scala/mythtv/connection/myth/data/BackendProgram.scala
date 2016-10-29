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
  lazy val season: Int = fields("season").toInt
  lazy val episode: Int = fields("episode").toInt
  def syndicatedEpisodeNumber: String = fields("syndicatedEpisodeNumber")
  def category: String = fields("category")
  lazy val chanId: ChanId = ChanId(fields("chanId").toInt)
  def chanNum: ChannelNumber = ChannelNumber(fields("chanNum"))
  def callsign: String = fields("callsign")
  def chanName: String = fields("chanName")
  def filename: String = fields("filename")
  lazy val filesize: ByteCount = DecimalByteCount(fields("filesize").toLong)
  lazy val startTime: MythDateTime = timestampField("startTime")
  lazy val endTime: MythDateTime = timestampField("endTime")
  lazy val findId: Int = fields("findId").toInt
  def hostname: String = fields("hostname")
  lazy val sourceId: ListingSourceId = ListingSourceId(fields("sourceId").toInt)
  lazy val cardId: CaptureCardId = CaptureCardId(fields("cardId").toInt)
  lazy val inputId: InputId = InputId(fields("inputId").toInt)
  lazy val recPriority: Int = fields("recPriority").toInt
  lazy val recStatus: RecStatus = RecStatus.applyOrUnknown(fields("recStatus").toInt)
  lazy val recordId: RecordRuleId = RecordRuleId(fields("recordId").toInt)
  lazy val recType: RecType = RecType.applyOrUnknown(fields("recType").toInt)
  lazy val dupIn: DupCheckIn = DupCheckIn(fields("dupIn").toInt)
  lazy val dupMethod: DupCheckMethod = DupCheckMethod(fields("dupMethod").toInt)
  lazy val recStartTS: MythDateTime = timestampField("recStartTS")
  lazy val recEndTS: MythDateTime = timestampField("recEndTS")
  lazy val programFlags: Int = fields("programFlags").toInt  // TODO is this an enum? python bindings use string??
  def recGroup: String = fields("recGroup")
  def outputFilters: String = fields("outputFilters")  // TODO what type is this really?
  def seriesId: String = fields("seriesId")
  def programId: String = fields("programId")
  def inetRef: String = fields("inetRef")
  lazy val lastModified: MythDateTime = timestampField("lastModified")
  lazy val stars: Option[Double] = if (fields("stars") == "0") None else Try(fields("stars").toDouble).toOption
  lazy val originalAirDate: Option[LocalDate] = Try(LocalDate.parse(fields("originalAirDate"))).toOption
  def playGroup: String = fields("playGroup")
  lazy val recPriority2: Int = fields("recPriority2").toInt
  lazy val parentId: Int = fields("parentId").toInt   // TODO is this an Int or String?
  def storageGroup: String = fields("storageGroup")
  lazy val audioProps: AudioProperties = AudioProperties(fields("audioProps").toInt)
  lazy val videoProps: VideoProperties = VideoProperties(fields("videoProps").toInt)
  lazy val subtitleType: SubtitleType = SubtitleType(fields("subtitleType").toInt)
  lazy val year: Option[Year] = optionalNonZeroIntField("year") map Year.of
  lazy val partNumber: Option[Int] = optionalNonZeroIntField("partNumber")
  lazy val partTotal: Option[Int] = optionalNonZeroIntField("partTotal")
}

private[myth] trait BackendProgramFactory extends GenericBackendObjectFactory[BackendProgram]
private[myth] trait ProgramOtherSerializer extends MythProtocolSerializer with BackendTypeSerializer[Recording]

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

  def serializeOther(in: Recording): String = serializeOther(in, new StringBuilder).toString
  def serializeOther(in: Recording, builder: StringBuilder): StringBuilder = {
    serialize(in.title, builder)                   ++= MythProtocol.BACKEND_SEP
    serialize(in.subtitle, builder)                ++= MythProtocol.BACKEND_SEP
    serialize(in.description, builder)             ++= MythProtocol.BACKEND_SEP
    serialize(in.season, builder)                  ++= MythProtocol.BACKEND_SEP
    serialize(in.episode, builder)                 ++= MythProtocol.BACKEND_SEP
    serialize(in.syndicatedEpisodeNumber, builder) ++= MythProtocol.BACKEND_SEP
    serialize(in.category, builder)                ++= MythProtocol.BACKEND_SEP
    serialize(in.chanId, builder)                  ++= MythProtocol.BACKEND_SEP
    serialize(in.chanNum, builder)                 ++= MythProtocol.BACKEND_SEP
    serialize(in.callsign, builder)                ++= MythProtocol.BACKEND_SEP
    serialize(in.chanName, builder)                ++= MythProtocol.BACKEND_SEP
    serialize(in.filename, builder)                ++= MythProtocol.BACKEND_SEP
    serialize(in.filesize.bytes, builder)          ++= MythProtocol.BACKEND_SEP
    serialize(in.startTime, builder)               ++= MythProtocol.BACKEND_SEP
    serialize(in.endTime, builder)                 ++= MythProtocol.BACKEND_SEP
    serialize(in.findId, builder)                  ++= MythProtocol.BACKEND_SEP
    serialize(in.hostname, builder)                ++= MythProtocol.BACKEND_SEP
    serialize(in.sourceId, builder)                ++= MythProtocol.BACKEND_SEP
    serialize(in.cardId, builder)                  ++= MythProtocol.BACKEND_SEP
    serialize(in.inputId, builder)                 ++= MythProtocol.BACKEND_SEP
    serialize(in.recPriority, builder)             ++= MythProtocol.BACKEND_SEP
    serialize(in.recStatus, builder)               ++= MythProtocol.BACKEND_SEP
    serialize(in.recordId, builder)                ++= MythProtocol.BACKEND_SEP
    serialize(in.recType, builder)                 ++= MythProtocol.BACKEND_SEP
    serialize(in.dupIn, builder)                   ++= MythProtocol.BACKEND_SEP
    serialize(in.dupMethod, builder)               ++= MythProtocol.BACKEND_SEP
    serialize(in.recStartTS, builder)              ++= MythProtocol.BACKEND_SEP
    serialize(in.recEndTS, builder)                ++= MythProtocol.BACKEND_SEP
    serialize(in.programFlags, builder)            ++= MythProtocol.BACKEND_SEP
    serialize(in.recGroup, builder)                ++= MythProtocol.BACKEND_SEP
    serialize(in.outputFilters, builder)           ++= MythProtocol.BACKEND_SEP
    serialize(in.seriesId, builder)                ++= MythProtocol.BACKEND_SEP
    serialize(in.programId, builder)               ++= MythProtocol.BACKEND_SEP
    serialize(in.inetRef, builder)                 ++= MythProtocol.BACKEND_SEP
    serialize(in.lastModified, builder)            ++= MythProtocol.BACKEND_SEP
    serialize(in.stars, builder)                   ++= MythProtocol.BACKEND_SEP
    serialize(in.originalAirDate, builder)         ++= MythProtocol.BACKEND_SEP
    serialize(in.playGroup, builder)               ++= MythProtocol.BACKEND_SEP
    serialize(in.recPriority2, builder)            ++= MythProtocol.BACKEND_SEP
    serialize(in.parentId, builder)                ++= MythProtocol.BACKEND_SEP
    serialize(in.storageGroup, builder)            ++= MythProtocol.BACKEND_SEP
    serialize(in.audioProps, builder)              ++= MythProtocol.BACKEND_SEP
    serialize(in.videoProps, builder)              ++= MythProtocol.BACKEND_SEP
    serialize(in.subtitleType, builder)            ++= MythProtocol.BACKEND_SEP
    serialize(in.year, builder)                    ++= MythProtocol.BACKEND_SEP
    serialize(in.partNumber, builder)              ++= MythProtocol.BACKEND_SEP
    serialize(in.partTotal, builder)
  }
}
