package mythtv
package services

import model.{ ChanId, Channel, Guide, Program }
import util.{ OptionalCount, MythDateTime }
import connection.http.HttpStreamResponse

trait GuideService extends BackendService {
  def serviceName: String = "Guide"

  def getProgramGuide(
    startTime: MythDateTime,
    endTime: MythDateTime,
    startChanId: ChanId = ChanId(0),
    numChannels: OptionalCount[Int] = OptionalCount.all,
    details: Boolean = false
  ): ServiceResult[Guide[Channel, Program]]

  def getProgramDetails(chanId: ChanId, startTime: MythDateTime): ServiceResult[Program]

  def getChannelIcon[U](chanId: ChanId, width: Int = 0, height: Int = 0)(f: (HttpStreamResponse) => U): ServiceResult[Unit]
}
