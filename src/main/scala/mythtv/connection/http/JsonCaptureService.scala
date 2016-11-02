package mythtv
package connection
package http

import services.CaptureService
import model.{ CaptureCard, CaptureCardId, CardInput, InputId }

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

  /* mutating POST methods */

  def addCaptureCard(card: CaptureCard): CaptureCardId = ???

  def removeCaptureCard(cardId: CaptureCardId): Boolean = ???

  def updateCaptureCard(cardId: CaptureCardId, setting: String, value: String): Boolean = ???


  // TODO we don't have enough details in CardInput trait currently to support addCardInput
  def addCardInput(cardInput: CardInput): InputId = ???

  def removeCardInput(inputId: InputId): Boolean = ???

  def updateCardInput(inputId: InputId, setting: String, value: String): Boolean = ???

}
