package mythtv
package connection
package http

import model.{ CaptureCard, CaptureCardId, CardInput, InputId }
import services.{ CaptureService, ServiceResult }

trait AbstractCaptureService extends ServiceProtocol with CaptureService {

  // TODO needs to detect dummy response and return appropriate value
  def getCaptureCard(cardId: CaptureCardId): ServiceResult[CaptureCard] = {
    val params: Map[String, Any] = Map("CardId" -> cardId.id)
    request("GetCaptureCard", params)("CaptureCard")
  }

  def getCaptureCardList(hostName: String, cardType: String): ServiceResult[List[CaptureCard]] = {
    var params: Map[String, Any] = Map.empty
    if (hostName.nonEmpty) params += "HostName" -> hostName
    if (cardType.nonEmpty) params += "CardType" -> cardType
    request("GetCaptureCardList", params)("CaptureCardList", "CaptureCards")
  }

  def addCaptureCard(card: CaptureCard): ServiceResult[CaptureCardId] = {
    val params: Map[String, Any] = Map(
      "VideoDevice"        -> card.videoDevice.getOrElse(""),   // NB this is required/checked by the backend
      "AudioDevice"        -> card.audioDevice.getOrElse(""),
      "VBIDevice"          -> card.vbiDevice.getOrElse(""),
      "CardType"           -> card.cardType.getOrElse(""),      // NB this is required/checked by the backend
      "AudioRateLimit"     -> card.audioRateLimit.getOrElse(0),
      "HostName"           -> card.hostName,                    // this is required
      "DVBSWFilter"        -> card.dvbSwFilter.getOrElse(0),
      "DVBSatType"         -> card.dvbSatType.getOrElse(0),
      "DVBWaitForSeqStart" -> card.dvbWaitForSeqStart,
      "SkipBTAudio"        -> card.skipBtAudio,
      "DVBOnDemand"        -> card.dvbOnDemand,
      "DVBDiSEqCType"      -> card.dvbDiseqcType.getOrElse(0),
      "FirewireSpeed"      -> card.firewireSpeed.getOrElse(0),
      "FirewireModel"      -> card.firewireModel.getOrElse(""),
      "FirewireConnection" -> card.firewireConnection.getOrElse(0),
      "SignalTimeout"      -> card.signalTimeout,
      "ChannelTimeout"     -> card.channelTimeout,
      "DVBTuningDelay"     -> card.dvbTuningDelay,
      "Contrast"           -> card.contrast,
      "Brightness"         -> card.brightness,
      "Colour"             -> card.colour,
      "Hue"                -> card.hue,
      "DiSEqCId"           -> card.diseqcId.getOrElse(0),  // TODO s/b NULL not 0 ??
      "DVBEITScan"         -> card.dvbEitScan
    )
    post("AddCaptureCard", params)()
  }

  def removeCaptureCard(cardId: CaptureCardId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("CardId" -> cardId.id)
    post("RemoveCaptureCard", params)()
  }

  def updateCaptureCard(cardId: CaptureCardId, setting: String, value: String): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "CardId"  -> cardId.id,
      "Setting" -> setting,
      "Value"   -> value
    )
    post("UpdateCaptureCard", params)()
  }

  // TODO we don't have enough details in CardInput trait currently to support all of addCardInput
  def addCardInput(input: CardInput): ServiceResult[InputId] = {
    val params: Map[String, Any] = Map(
      "CardId"          -> input.cardId.id,        // required
      "SourceId"        -> input.sourceId.id,      // required
      "InputName"       -> input.name,             // required
      /*
      "ExternalCommand" -> ???,
      "ChangerDevice"   -> ???,
      "ChangerModel"    -> ???,
      "HostName"        -> ???,
      "TuneChan"        -> ???,
      "StartChan"       -> ???,
      "DisplayName"     -> ???,
      "DishnetEIT"      -> ???,
      "RecPriority"     -> ???,
      "Quicktune"       -> ???,
      "SchedOrder"      -> ???,
      */
      "LiveTVOrder"     -> input.liveTvOrder
    )
    post("AddCardInput", params)()
  }

  def removeCardInput(inputId: InputId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("CardInputId" -> inputId.id)
    post("RemoveCardInput", params)()
  }

  def updateCardInput(inputId: InputId, setting: String, value: String): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "CardInputId" -> inputId.id,
      "Setting"     -> setting,
      "Value"       -> value
    )
    post("UpdateCardInput", params)()
  }
}
