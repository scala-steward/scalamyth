package mythtv
package model

import util.LooseEnum
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

object ChannelBrowseDirection extends LooseEnum {
  type ChannelBrowseDirection = Value
  val Invalid   = Value(-1)
  val Same      = Value(0)  // Current channel and time
  val Up        = Value(1)  // Previous channel
  val Down      = Value(2)  // Next channel
  val Left      = Value(3)  // Current channel in the past
  val Right     = Value(4)  // Current channel in the future
  val Favorite  = Value(5)  // Next favorite channel
}

object ChannelChangeDirection extends LooseEnum {
  type ChannelChangeDirection = Value
  val Up        = Value(0)
  val Down      = Value(1)
  val Favorite  = Value(2)
  val Same      = Value(3)
}

object PictureAdjustType extends LooseEnum {
  type PictureAdjustType = Value
  val None      = Value(0)
  val Playback  = Value(1)
  val Channel   = Value(2)
  val Recording = Value(3)
}

/* We explicitly specify the names for the values here because the reflection
   based default implementation fails once we start subclassing. Additionally,
   we use the withName() method to translate state strings to values, so the
   names specified here must match those in MythTV's StateToString */
private[model] abstract class AbstractTvStateEnum extends LooseEnum {
  val Error               = Value(-1, "Error")
  val None                = Value( 0, "None")
  val WatchingLiveTv      = Value( 1, "WatchingLiveTV")
  val WatchingPreRecorded = Value( 2, "WatchingPreRecorded")
  val WatchingVideo       = Value( 3, "WatchingVideo")
  val WatchingDvd         = Value( 4, "WatchingDVD")
  val WatchingBd          = Value( 5, "WatchingBD")
  val WatchingRecording   = Value( 6, "WatchingRecording")
  val RecordingOnly       = Value( 7, "RecordingOnly")
  val ChangingState       = Value( 8, "ChangingState")
}

object TvState extends AbstractTvStateEnum {
  type TvState = Value
}

object SleepStatus extends LooseEnum {
  type SleepStatus = Value
  val Awake         = Value(0x0)
  val Asleep        = Value(0x1)
  val FallingAsleep = Value(0x3)
  val Waking        = Value(0x5)
  val Undefined     = Value(0x8)
}
