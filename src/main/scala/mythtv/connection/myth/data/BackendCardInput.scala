package mythtv
package connection
package myth
package data

import model.{ CaptureCardId, CardInput, ListingSourceId }

private[myth] class BackendCardInput(data: Seq[String], fieldOrder: IndexedSeq[String])
    extends GenericBackendObject(data, fieldOrder) with CardInput {

  override def toString: String = s"<BackendCardInput $cardInputId, $cardId $name>"

  /* Convenience accessors with proper type */

  lazy val cardInputId: Int = fields("cardInputId").toInt
  lazy val cardId: CaptureCardId = CaptureCardId(fields("cardId").toInt)
  lazy val sourceId: ListingSourceId = ListingSourceId(fields("sourceId").toInt)
  def name: String = fields("name")
  lazy val mplexId: Int = fields("mplexId").toInt
  lazy val liveTVorder: Int = fields("liveTVorder").toInt
}

private[myth] trait BackendCardInputFactory extends GenericBackendObjectFactory[BackendCardInput]

private[myth] object BackendCardInput extends BackendCardInputFactory {
  final val FIELD_ORDER = IndexedSeq (
    "name", "sourceId", "cardInputId", "cardId", "mplexId", "liveTVorder"
  )

  def apply(data: Seq[String]): BackendCardInput = new BackendCardInput(data, FIELD_ORDER)
}
