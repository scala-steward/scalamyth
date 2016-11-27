package mythtv
package connection
package http
package json

import scala.util.Try

import services.{ CaptureService, ServiceResult }
import model.{ CaptureCard, CaptureCardId, CardInput, InputId }
import RichJsonObject._

class JsonCaptureService(conn: BackendJsonConnection)
  extends JsonBackendService(conn)
     with CaptureService {

  def getCaptureCard(cardId: CaptureCardId): ServiceResult[CaptureCard] = {
    val params: Map[String, Any] = Map("CardId" -> cardId.id)
    for {
      response <- request("GetCaptureCard", params)
      root     <- responseRoot(response, "CaptureCard")
      result   <- Try( root.convertTo[CaptureCard] )
    } yield result
  }

  def getCaptureCardList(hostName: String, cardType: String): ServiceResult[List[CaptureCard]] = {
    var params: Map[String, Any] = Map.empty
    if (hostName.nonEmpty) params += "HostName" -> hostName
    if (cardType.nonEmpty) params += "CardType" -> cardType
    for {
      response <- request("GetCaptureCardList", params)
      root     <- responseRoot(response, "CaptureCardList")
      result   <- Try( root.convertTo[List[CaptureCard]] )
    } yield result
  }

  /* mutating POST methods */

  def addCaptureCard(card: CaptureCard): ServiceResult[CaptureCardId] = {
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
    for {
      response <- post("AddCaptureCard", params)
      result   <- Try( CaptureCardId(0) ) // TODO implement
    } yield result
  }

  def removeCaptureCard(cardId: CaptureCardId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("CardId" -> cardId.id)
    for {
      response <- post("RemoveCaptureCard", params)
      root     <- responseRoot(response)
      result   <- Try( root.booleanField("bool") ) // TODO test
    } yield result
  }

  def updateCaptureCard(cardId: CaptureCardId, setting: String, value: String): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "CardId"  -> cardId.id,
      "Setting" -> setting,
      "Value"   -> value
    )
    for {
      response <- post("UpdateCaptureCard", params)
      root     <- responseRoot(response)
      result   <- Try( root.booleanField("bool") ) // TODO test
    } yield result
  }

  // TODO we don't have enough details in CardInput trait currently to support addCardInput
  def addCardInput(input: CardInput): ServiceResult[InputId] = {
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
    for {
      response <- post("AddCardInput", params)
      result   <- Try( InputId(0) )  // TODO
    } yield result
  }

  def removeCardInput(inputId: InputId): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("CardInputId" -> inputId.id)
    for {
      response <- post("RemoveCardInput", params)
      root     <- responseRoot(response)
      result   <- Try( root.booleanField("bool") ) // TODO test
    } yield result
  }

  def updateCardInput(inputId: InputId, setting: String, value: String): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "CardInputId" -> inputId.id,
      "Setting"     -> setting,
      "Value"       -> value
    )
    for {
      response <- post("UpdateCardInput", params)
      root     <- responseRoot(response)
      result   <- Try( root.booleanField("bool") ) // TODO test
    } yield result
  }

}
