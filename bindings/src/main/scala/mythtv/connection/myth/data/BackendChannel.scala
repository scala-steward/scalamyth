package mythtv
package connection
package myth
package data

import model.{ Channel, ChannelNumber, ChanId, ListingSourceId, XmlTvChannel }

private[myth] class BackendChannel(data: Seq[String], fieldOrder: IndexedSeq[String])
    extends GenericBackendObject(data, fieldOrder) with Channel with XmlTvChannel {

  override def toString: String = s"<BackendChannel $chanId, $number $callsign>"

  /* Convenience accessors with proper type */

  def chanId: ChanId = ChanId(fields("chanId").toInt)
  def name: String = fields("name")
  def number: ChannelNumber = ChannelNumber(fields("number"))
  def callsign: String = fields("callsign")
  def sourceId: ListingSourceId = ListingSourceId(fields("sourceId").toInt)
  def xmltvId: String = fields("xmltvId")
}

private[myth] trait BackendChannelFactory extends GenericBackendObjectFactory[BackendChannel]
private[myth] trait ChannelOtherSerializer extends BackendTypeSerializer[Channel]

private[myth] object BackendChannel extends BackendChannelFactory {
  final val FIELD_ORDER = IndexedSeq (
    "chanId", "sourceId", "callsign", "number", "name", "xmltvId"
  )

  def apply(data: Seq[String]): BackendChannel = new BackendChannel(data, FIELD_ORDER)
}
