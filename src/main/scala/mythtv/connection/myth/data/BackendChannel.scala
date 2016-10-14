package mythtv
package connection
package myth
package data

import model.{ Channel, ChanId, ListingSourceId }

private[myth] class BackendChannel(data: Seq[String]) extends Channel {
  import BackendChannel.FIELD_ORDER

  // assumes data.length >= FIELD_ORDER.length, or else some fields will be missing
  val fields: Map[String, String] = (FIELD_ORDER zip data).toMap

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

private[myth] object BackendChannel {
  final val FIELD_ORDER = IndexedSeq (
    "chanId", "sourceId", "callsign", "number", "name", "xmltvId"
  )

  def apply(data: Seq[String]): BackendChannel = new BackendChannel(data)
}
