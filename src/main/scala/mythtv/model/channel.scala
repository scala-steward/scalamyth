package mythtv
package model

import java.time.Instant

import EnumTypes.ChannelCommDetectMethod

final case class ChanId(id: Int) extends AnyVal

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
  def sourceId: ListingSourceId  // TODO not included in base results for guide data from services?

  /*
  def xmltvId: String //TODO, expose this here? it is returned by QUERY_RECORDER/GET_CHANNEL_INFO in MythProtocol
   */

  /*
  def visible: Boolean
  def recPriority: Int           // TODO do we want this here?  Not in serivce object?
  def lastRecord: MythDateTime   // TODO do we want this here?  Not in service object?
   */

  override def toString: String = s"<Channel $chanId ${number.num} $callsign>"
}

trait ChannelDetails extends Channel {
  def freqId: Option[String]
  def iconPath: String        // TODO is this a URL or file path (or could be either!)
  def fineTune: Option[Int]   // TODO what is this?
  /* TODO does videofilters field map anywhere? */
  def xmltvId: String
  /* TODO: does recpriority field map anywhere? */
  // also contrast, brightness, colour, hue
  def format: String   // sometimes set to "" even when database entry disagrees (e.g. in return from GetRecorded...)
  def visible: Boolean
  def outputFilters: Option[String]
  def useOnAirGuide: Boolean  // TODO is this really nullable as database schema indicates?
  def mplexId: Option[MultiplexId]
  def serviceId: Option[Int]  // TODO what is this?
  /* TODO does tmoffset map anywhere */
  def atscMajorChan: Option[Int]      // sometimes set to "0" even when data is avail (e.g. in return from GetRecorded...)
  def atscMinorChan: Option[Int]      //     "       "  "   "
  /* TODO last_record? */
  def defaultAuthority: Option[String]  // TODO what is this?
  def commMethod: ChannelCommDetectMethod
  /* iptvid? */
  /* Results not in DB?
       videosource: FrequencyTable
       dtv_multiplex: Frequency, Modulation, NetworkId, SIStandard, TransportId
         (latter set sometimes collectively called "TuningParams"
   */

  def isCommercialFree: Boolean = commMethod == ChannelCommDetectMethod.CommFree
}

final case class ListingSourceId(id: Int) extends AnyVal

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
}

final case class MultiplexId(id: Int) extends AnyVal

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
  def updateTimestamp: Instant
  def defaultAuthority: Option[String]

  override def toString: String =
    s"<VideoMultiplex $mplexId $sourceId $transportId $frequency $modulation>"
}
