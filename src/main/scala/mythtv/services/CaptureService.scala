package mythtv
package services

import model.{ CaptureCardId, CaptureCard, CardInput, InputId }

trait CaptureService extends BackendService {
  def serviceName: String = "Capture"

  def getCaptureCard(cardId: CaptureCardId): CaptureCard

  def getCaptureCardList(hostName: String = "", cardType: String = ""): List[CaptureCard]

  /* mutating POST methods */

  def addCaptureCard(card: CaptureCard): CaptureCardId

  def removeCaptureCard(cardId: CaptureCardId): Boolean

  def updateCaptureCard(cardId: CaptureCardId, setting: String, value: String): Boolean

  def addCardInput(cardInput: CardInput): InputId

  def removeCardInput(inputId: InputId): Boolean

  def updateCardInput(inputId: InputId, setting: String, value: String): Boolean
}
