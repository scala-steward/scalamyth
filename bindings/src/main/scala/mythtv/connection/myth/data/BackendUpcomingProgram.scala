package mythtv
package connection
package myth
package data

import model.{ ChanId, UpcomingProgram }
import util.MythDateTime

private[myth] class BackendUpcomingProgram(data: Seq[String], fieldOrder: IndexedSeq[String])
    extends GenericBackendObject(data, fieldOrder) with UpcomingProgram {

  override def toString: String = s"<BackendUpcomingProgram $chanId, $startTime: $title>"

  private def isoDateTimeField(f: String): MythDateTime = MythDateTime.fromNaiveIso(fields(f))

  /* Convenience accessors with proper type */

  def title: String = fields("title")
  def subtitle: String = fields("subtitle")
  def description: String = fields("description")
  def category: String = fields("category")
  def chanId: ChanId = ChanId(fields("chanId").toInt)
  def startTime: MythDateTime = isoDateTimeField("startTime")
  def endTime: MythDateTime = isoDateTimeField("endTime")
  def seriesId: String = fields("seriesId")
  def programId: String = fields("programId")
}

private[myth] trait BackendUpcomingProgramFactory extends GenericBackendObjectFactory[BackendUpcomingProgram]
private[myth] trait UpcomingProgramOtherSerializer extends BackendTypeSerializer[UpcomingProgram]

private[myth] object BackendUpcomingProgram extends BackendUpcomingProgramFactory {
  final val FIELD_ORDER = IndexedSeq(
    "title", "subtitle", "description", "category", "startTime", "endTime",
    "callsign", "iconpath", "channame", "chanId", "seriesId", "programId"
  )

  def apply(data: Seq[String]): BackendUpcomingProgram = new BackendUpcomingProgram(data, FIELD_ORDER)
}
