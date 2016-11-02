package mythtv
package connection
package myth
package data

import model.{ CaptureCardId, CardInput, InputId, ListingSourceId, MultiplexId }

private[myth] class BackendCardInput(data: Seq[String], fieldOrder: IndexedSeq[String])
    extends GenericBackendObject(data, fieldOrder) with CardInput {

  override def toString: String = s"<BackendCardInput $cardInputId, $cardId $name>"

  /* Convenience accessors with proper type */

  def cardInputId: InputId = InputId(fields("cardInputId").toInt)
  def cardId: CaptureCardId = CaptureCardId(fields("cardId").toInt)
  def sourceId: ListingSourceId = ListingSourceId(fields("sourceId").toInt)
  def name: String = fields("name")
  def mplexId: MultiplexId = MultiplexId(fields("mplexId").toInt)
  def liveTvOrder: Int = fields("liveTVorder").toInt
}

private[myth] trait BackendCardInputFactory extends GenericBackendObjectFactory[BackendCardInput]
private[myth] trait CardInputOtherSerializer extends BackendTypeSerializer[CardInput]

private[myth] object BackendCardInput extends BackendCardInputFactory {
  final val FIELD_ORDER = IndexedSeq (
    "name", "sourceId", "cardInputId", "cardId", "mplexId", "liveTVorder"
  )

  def apply(data: Seq[String]): BackendCardInput = new BackendCardInput(data, FIELD_ORDER)
}
