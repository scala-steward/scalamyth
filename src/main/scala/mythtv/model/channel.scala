package mythtv
package model

import java.time.Instant

import EnumTypes.ChannelCommDetectMethod

final case class ChanId(id: Int) extends AnyVal with IntegerIdentifier

object ChanId {
  object ChanIdOrdering extends Ordering[ChanId] {
    def compare(x: ChanId, y: ChanId): Int = x.id compare y.id
  }
  implicit def ordering: Ordering[ChanId] = ChanIdOrdering
}

final case class ChannelNumber(num: String) extends AnyVal

object ChannelNumber {
  object ChannelNumberOrdering extends Ordering[ChannelNumber] {
    def split(n: ChannelNumber): Array[String] = n.num split "[^0-9]"
    def compare(x: ChannelNumber, y: ChannelNumber): Int = {
      val xs = split(x)
      val ys = split(y)
      val len = math.min(xs.length, ys.length)
      var i, cmp = 0
      while (cmp == 0 && i < len) {
        cmp = xs(i).toInt compare ys(i).toInt
        i += 1
      }
      cmp
    }
  }
  implicit def ordering: Ordering[ChannelNumber] = ChannelNumberOrdering
}

/* These five items are included in the channel info we get from the MythProtocol
   command QUERY_RECORDER/GET_CHANNEL_INFO (+xmltvId) ; but there are many more fields
   in the database and returned from services API */
trait Channel {
  def chanId: ChanId
  def name: String
  def number: ChannelNumber
  def callsign: String
  def sourceId: ListingSourceId  // TODO not included in channel of GuideService.GetProgamGuide when Details=false

  override def toString: String = s"<Channel $chanId ${number.num} $callsign>"
}

trait XmlTvChannel {
  def xmltvId: String
}

trait ChannelDetails extends Channel with XmlTvChannel {
  def freqId: Option[String]
  def iconPath: String        // should be a file path relative to the ChannelIcons storage group?
  def fineTune: Option[Int]   // used to adjust the frequency, e.g. to compensate for carrier drift in a cable system (in kHz?)
  def format: String          // sometimes set to "" even when database entry disagrees (e.g. in return from GetRecorded...)
  def visible: Boolean
  def outputFilters: Option[String]
  def useOnAirGuide: Boolean  // TODO is this really nullable as database schema indicates?
  def mplexId: Option[MultiplexId]
  def serviceId: Option[Int]          // MPEG service/program number when describing a DVB service
  def atscMajorChan: Option[Int]      // sometimes set to "0" even when data is avail (e.g. in return from GetRecorded...)
  def atscMinorChan: Option[Int]      //     "       "  "   "
  def defaultAuthority: Option[String]  // TODO what is this?
  def commMethod: ChannelCommDetectMethod

  /* TODO fields from DB 'channel' table not in ChannelInfo from ChannelService:
   *   videofilters
   *   contrast
   *   brightness
   *   colour
   *   hue
   *   tmoffset
   *   recpriority
   *   last_record
   *   iptvid
   */

  /* Results from ChannelService but stored in a DB table other than 'channel':
   *    videosource table: FrequencyTable
   *    dtv_multiplex table: Frequency, Modulation, NetworkId, SIStandard, TransportId
   *        (sometimes collectively called "TuningParams")
   */

  def isCommercialFree: Boolean = commMethod == ChannelCommDetectMethod.CommFree
}

trait Lineup {
  def lineupId: String
  def name: String
  def displayName: String
  def lineupType: String
  def postalCode: String
  def device: String

  override def toString: String = s"<Lineup $displayName>"
}


final case class ListingSourceId(id: Int) extends AnyVal with IntegerIdentifier

trait ListingSource {
  def sourceId: ListingSourceId
  def name: String
  def grabber: Option[String]
  def freqTable: String
  def lineupId: Option[String]
  def userId: Option[String]
  def password: Option[String]
  def useEit: Boolean
  def configPath: Option[String]
  def dvbNitId: Option[Int]

  override def toString: String = s"<ListingSource $sourceId $name $lineupId>"
}

final case class MultiplexId(id: Int) extends AnyVal with IntegerIdentifier

// from the dtv_multiplex table and the GetVideoMultiplex services command
trait VideoMultiplex {
  def mplexId: MultiplexId
  def sourceId: ListingSourceId  // TODO can sourceId/transportId really be null?
  def transportId: Int
  def networkId: Option[Int]
  def frequency: Int
  def inversion: Char
  def symbolRate: Int
  def fec: String
  def polarity: Char
  def modulation: String
  def bandwidth: Char
  def lpCodeRate: String
  def transmissionMode: Char
  def guardInterval: String
  def visible: Boolean
  def constellation: String
  def hierarchy: String
  def hpCodeRate: String
  def modulationSystem: String
  def rolloff: String
  def siStandard: String
  def serviceVersion: Int
  def updatedTimestamp: Instant
  def defaultAuthority: Option[String]

  override def toString: String =
    s"<VideoMultiplex $mplexId $sourceId $transportId $frequency $modulation>"
}
