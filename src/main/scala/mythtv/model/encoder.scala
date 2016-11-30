package mythtv
package model

import EnumTypes.{ TvState, SleepStatus }

final case class CaptureCardId(id: Int) extends AnyVal with IntegerIdentifier

trait Tuner {
  def cardId: CaptureCardId
  def videoDevice: Option[String]
  def audioDevice: Option[String]
  def vbiDevice: Option[String]
}

trait CaptureCard extends Tuner {
  def cardType: Option[String]
  def audioRateLimit: Option[Int]
  def hostName: String                 // TODO can this really be nullable as the DB schema says?
  def dvbSwFilter: Option[Int]         // re-introduced by commit 291dd7f97, UNUSED since?
  def dvbSatType: Option[Int]          // is this UNUSED since ~2005, git commit 540b9f58d ??
  def dvbWaitForSeqStart: Boolean
  def skipBtAudio: Boolean
  def dvbOnDemand: Boolean             // when enabled, only open the DVB card when required
  def dvbDiseqcType: Option[Int]       // is this UNUSED since ~2006 git commit 818423230 ??
  def firewireSpeed: Option[Int]       // firewire speed Mbps: { 0=100, 1=200, 2=400, 3=800 }
  def firewireModel: Option[String]    // firewire cable box model
  def firewireConnection: Option[Int]  // firewire communications protocol indicator (0=point-to-point, 1=broadcast)
  def signalTimeout: Int   // in millis, timeout waiting for signal when scanning for channels
  def channelTimeout: Int  // in millis, timeout waiting for channel lock; doubled for recordings
  def dvbTuningDelay: Int  // in millis, intentionally slows down the tuning process, required by some cards
  def contrast: Int
  def brightness: Int
  def colour: Int
  def hue: Int
  def diseqcId: Option[Int]
  def dvbEitScan: Boolean               // use DVB card for EIT scan?
  // field 'defaultinput' from the DB capturecard table is excluded here

  override def toString: String =
    s"<CaptureCard $cardId $hostName ${cardType.getOrElse("")} ${videoDevice.getOrElse("")}>"
}

trait RemoteEncoder {
  def cardId: CaptureCardId
  def host: String
  def port: Int

  override def toString: String = s"<RemoteEncoder $cardId $host $port>"
}

// this does not seem to include "port" data, though, hmm...
trait RemoteEncoderState extends RemoteEncoder {
  def local: Boolean
  def connected: Boolean
  def lowFreeSpace: Boolean
  def state: TvState
  def sleepStatus: SleepStatus
  def currentRecording: Option[Recording]

  override def toString: String = s"<RemoteEncoderState $cardId $host $state>"
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
