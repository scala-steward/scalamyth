package mythtv
package model

import EnumTypes.{ TvState, SleepStatus }

final case class CaptureCardId(id: Int) extends AnyVal with IntegerIdentifier

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
  def cardId: CaptureCardId
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

final case class InputId(id: Int) extends AnyVal with IntegerIdentifier

trait CardInput {
  def cardInputId: InputId
  def cardId: CaptureCardId
  def sourceId: ListingSourceId
  def name: String
  def mplexId: MultiplexId
  def liveTvOrder: Int
}

trait SignalMonitorValue {
  def name: String
  def statusName: String
  def value: Int
  def threshold: Int
  def minValue: Int
  def maxValue: Int
  def timeout: Int   // in millis
  def isHighThreshold: Boolean
  def isValueSet: Boolean

  def isGood: Boolean =
    if (isHighThreshold) value >= threshold
    else                 value <= threshold

  override def toString: String =
    s"<Signal $name $value ($minValue,$maxValue) ${timeout}ms set:$isValueSet good:$isGood>"
}
