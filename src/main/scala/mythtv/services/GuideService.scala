package mythtv
package services

import model.{ ChanId, Channel, Guide, Program }
import util.{ OptionalCount, MythDateTime }

trait GuideService extends BackendService {
  def serviceName: String = "Guide"

  def getProgramGuide(
    startTime: MythDateTime,
    endTime: MythDateTime,
    startChanId: ChanId = ChanId(0),
    numChannels: OptionalCount[Int] = OptionalCount.all,
    details: Boolean = false
  ): Guide[Channel, Program]

  def getProgramDetails(chanId: ChanId, startTime: MythDateTime): Program

  def getChannelIcon(chanId: ChanId): DataBytes  // TODO optional width and height
}
