package mythtv
package connection
package http
package json

import services.CaptureService
import model.{ CaptureCard, CaptureCardId, CardInput, InputId }
import RichJsonObject._

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

  def addCaptureCard(card: CaptureCard): CaptureCardId = {
    val params: Map[String, Any] = Map(
      "VideoDevice"        -> card.videoDevice.getOrElse(""),
      "AudioDevice"        -> card.audioDevice.getOrElse(""),
      "VBIDevice"          -> card.vbiDevice.getOrElse(""),
      "CardType"           -> card.cardType.getOrElse(""),
      "AudioRateLimit"     -> card.audioRateLimit.getOrElse(0),
      "HostName"           -> card.hostName,
      "DVBSWFilter"        -> card.dvbSwFilter.getOrElse(0),
      "DVBSatType"         -> card.dvbSatType.getOrElse(0),
      "DVBWaitForSeqStart" -> card.dvbWaitForSeqStart,
      "SkipBTAudio"        -> card.skipBtAudio,
      "DVBOnDemand"        -> card.dvbOnDemand,
      "DVBDiSEqCType"      -> card.dvbDiseqcType.getOrElse(0),
      "FirewireSpeed"      -> card.firewireSpeed.getOrElse(0),
      "FirewireModel"      -> card.firewireModel.getOrElse(""),  // TODO s/b NULL not ""
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
    val response = post("AddCaptureCard", params)
    CaptureCardId(0)  // TODO implement
  }

  def removeCaptureCard(cardId: CaptureCardId): Boolean = {
    val params: Map[String, Any] = Map("CardId" -> cardId.id)
    val response = post("RemoveCaptureCard", params)
    val root = responseRoot(response)
    root.booleanField("bool")  // TODO test
  }

  def updateCaptureCard(cardId: CaptureCardId, setting: String, value: String): Boolean = {
    val params: Map[String, Any] = Map(
      "CardId"  -> cardId.id,
      "Setting" -> setting,
      "Value"   -> value
    )
    val response = post("UpdateCaptureCard", params)
    val root = responseRoot(response)
    root.booleanField("bool")  // TODO test
  }

  // TODO we don't have enough details in CardInput trait currently to support addCardInput
  def addCardInput(input: CardInput): InputId = {
    val params: Map[String, Any] = Map(
      "CardId"          -> input.cardInputId.id,
      "SourceId"        -> input.sourceId.id,
      "InputName"       -> input.name,
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
      "LiveTVOrder"     -> input.liveTvOrder
    )
    val response = post("AddCardInput", params)
    InputId(0)   // TODO
  }

  def removeCardInput(inputId: InputId): Boolean = {
    val params: Map[String, Any] = Map("CardInputId" -> inputId.id)
    val response = post("RemoveCardInput", params)
    val root = responseRoot(response)
    root.booleanField("bool")  // TODO test
  }

  def updateCardInput(inputId: InputId, setting: String, value: String): Boolean = {
    val params: Map[String, Any] = Map(
      "CardInputId" -> inputId.id,
      "Setting"     -> setting,
      "Value"       -> value
    )
    val response = post("UpdateCardInput", params)
    val root = responseRoot(response)
    root.booleanField("bool")  // TODO test
  }

}
