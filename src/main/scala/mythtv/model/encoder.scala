package mythtv
package model

import EnumTypes.{ TvState, SleepStatus }

final case class CaptureCardId(id: Int) extends AnyVal

trait CaptureCard {
  def cardId: CaptureCardId
  def videoDevice: Option[String]
  def audioDevice: Option[String]
  def vbiDevice: Option[String]
  def cardType: Option[String]
  def audioRateLimit: Option[Int]
  def hostName: String    // TODO can this really be nullable as the DB schema says?
  def dvbSwFilter: Option[Int]  // TODO what is this?
  def dvbSatType: Option[Int]   // TODO what is this?
  def dvbWaitForSeqStart: Boolean
  def skipBtAudio: Boolean
  def dvbOnDemand: Boolean
  def dvbDiseqcType: Option[Int] // TODO what is this?
  def firewireSpeed: Option[Int] // TODO what is this?
  def firewireModel: Option[String]
  def firewireConnection: Option[Int]
  def signalTimeout: Int   // TODO what are units?
  def channelTimeout: Int  // TODO what are units?
  def dvbTuningDelay: Int  // TODO what are units?
  def contrast: Int        // TODO these all default to zero, is that a valid value?, i.e. can we map to None?
  def brightness: Int
  def colour: Int
  def hue: Int
  def diseqcId: Option[Int]
  def dvbEitScan: Boolean
  // field 'defaultinput' from the DB capturecard table is excluded here

  override def toString: String
    = s"<CaptureCard $cardId $hostName ${cardType.getOrElse("")} ${videoDevice.getOrElse("")}>"
}

trait RemoteEncoder {
  def cardId: CaptureCardId  // is this correct type?
  def host: String
  def port: Int
}

// this does not seem to include "port" data, though, hmm...
trait RemoteEncoderState extends RemoteEncoder {
  def local: Boolean
  def connected: Boolean
  def lowFreeSpace: Boolean
  def state: TvState
  def sleepStatus: SleepStatus
  def currentRecording: Option[Recording]
}

trait CardInput {
  def cardInputId: Int
  def cardId: CaptureCardId
  def sourceId: ListingSourceId   // is this the right source Id?
  def name: String
  def mplexId: Int
  def liveTVorder: Int
}
