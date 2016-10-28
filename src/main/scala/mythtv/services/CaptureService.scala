package mythtv
package services

import model.{ CaptureCardId, CaptureCard }

trait CaptureService extends BackendService {
  def serviceName: String = "Capture"

  def getCaptureCard(cardId: CaptureCardId): CaptureCard
  def getCaptureCardList(hostName: String = "", cardType: String = ""): List[CaptureCard]
}
