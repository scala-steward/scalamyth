package mythtv
package connection
package http

import services.CaptureService
import model.{ CaptureCard, CaptureCardId }

class JsonCaptureService(conn: BackendJsonConnection)
  extends JsonBackendService(conn)
     with CaptureService {
  def getCaptureCard(cardId: CaptureCardId): CaptureCard = {
    val params: Map[String, Any] = Map("CardId" -> cardId.id)
    val response = request("GetCaptureCard", params)
    val root = responseRoot(response, "CaptureCard")
    root.convertTo[CaptureCard]
  }

  def getCaptureCardList(hostName: String, cardType: String): List[CaptureCard] = {
    var params: Map[String, Any] = Map.empty
    if (hostName.nonEmpty) params += "HostName" -> hostName
    if (cardType.nonEmpty) params += "CardType" -> cardType
    val response = request("GetCaptureCardList", params)
    val root = responseRoot(response, "CaptureCardList")
    root.convertTo[List[CaptureCard]]
  }
}
