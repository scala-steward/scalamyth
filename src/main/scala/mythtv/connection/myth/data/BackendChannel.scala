package mythtv
package connection
package myth
package data

import model.{ Channel, ChanId, ListingSourceId }

private[myth] class BackendChannel(data: Seq[String], fieldOrder: IndexedSeq[String])
    extends GenericBackendObject with Channel {

  // assumes data.length >= fieldOrder.length, or else some fields will be missing
  val fields: Map[String, String] = (fieldOrder zip data).toMap

  def apply(fieldName: String): String = fields(fieldName)

  def get(fieldName: String): Option[String] = fields.get(fieldName)

  override def toString: String = s"<BackendChannel $chanId, $number $callsign>"

  /* Convenience accessors with proper type */

  lazy val chanId: ChanId = ChanId(fields("chanId").toInt)
  def name: String = fields("name")
  def number: String = fields("number")
  def callsign: String = fields("callsign")
  lazy val sourceId: ListingSourceId = ListingSourceId(fields("sourceId").toInt)
  def xmltvId: String = fields("xmltvId")
}

private[myth] trait BackendChannelFactory extends GenericBackendObjectFactory[BackendChannel]

private[myth] object BackendChannel extends BackendChannelFactory {
  final val FIELD_ORDER = IndexedSeq (
    "chanId", "sourceId", "callsign", "number", "name", "xmltvId"
  )

  def apply(data: Seq[String]): BackendChannel = new BackendChannel(data, FIELD_ORDER)
}
