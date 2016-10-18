package mythtv
package connection
package myth
package data

import model.{ ChanId, UpcomingProgram }
import util.MythDateTime

private[myth] class BackendUpcomingProgram(data: Seq[String]) extends GenericBackendObject with UpcomingProgram {
  import BackendUpcomingProgram.FIELD_ORDER

  // assumes data.length >= FIELD_ORDER.length, or else some fields will be missing
  val fields: Map[String, String] = (FIELD_ORDER zip data).toMap

  def apply(fieldName: String): String = fields(fieldName)

  def get(fieldName: String): Option[String] = fields.get(fieldName)

  override def toString: String = s"<BackendUpcomingProgram $chanId, $startTime: $title>"

  private def isoDateTimeField(f: String): MythDateTime = MythDateTime.fromNaiveIso(fields(f))

  /* Convenience accessors with proper type */

  def title: String = fields("title")
  def subtitle: String = fields("subtitle")
  def description: String = fields("description")
  def category: String = fields("category")
  lazy val chanId: ChanId = ChanId(fields("chanId").toInt)
  lazy val startTime: MythDateTime = isoDateTimeField("startTime")
  lazy val endTime: MythDateTime = isoDateTimeField("endTime")
  def seriesId: String = fields("seriesId")
  def programId: String = fields("programId")
}

private[myth] trait BackendUpcomingProgramFactory extends GenericBackendObjectFactory[BackendUpcomingProgram]

private[myth] object BackendUpcomingProgram extends BackendUpcomingProgramFactory {
  final val FIELD_ORDER = IndexedSeq(
    "title", "subtitle", "description", "category", "startTime", "endTime",
    "callsign", "iconpath", "channame", "chanId", "seriesId", "programId"
  )

  def apply(data: Seq[String]): BackendUpcomingProgram = new BackendUpcomingProgram(data)
}
