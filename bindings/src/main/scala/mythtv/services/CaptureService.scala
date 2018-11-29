package mythtv
package services

import model.{ CaptureCardId, CaptureCard, CardInput, InputId }

trait CaptureService extends BackendService {
  final def serviceName: String = "Capture"

  def getCaptureCard(cardId: CaptureCardId): ServiceResult[CaptureCard]

  def getCaptureCardList(hostName: String = "", cardType: String = ""): ServiceResult[List[CaptureCard]]

  /* mutating POST methods */

  def addCaptureCard(card: CaptureCard): ServiceResult[CaptureCardId]

  def removeCaptureCard(cardId: CaptureCardId): ServiceResult[Boolean]

  def updateCaptureCard(cardId: CaptureCardId, setting: String, value: String): ServiceResult[Boolean]

  def addCardInput(cardInput: CardInput): ServiceResult[InputId]

  def removeCardInput(inputId: InputId): ServiceResult[Boolean]

  def updateCardInput(inputId: InputId, setting: String, value: String): ServiceResult[Boolean]
}
