package mythtv
package connection
package http
package json

import services.GuideService
import model.{ ChanId, Channel, Guide, Program }
import util.{ MythDateTime, OptionalCount, OptionalCountSome }

class JsonGuideService(conn: BackendJsonConnection)
  extends JsonBackendService(conn)
     with GuideService {
  def getProgramGuide(
    startTime: MythDateTime,
    endTime: MythDateTime,
    startChanId: ChanId,
    numChannels: OptionalCount[Int],
    details: Boolean
  ): Guide[Channel, Program] = {
    // There seems to be an off-by-one error on my 0.27 backend implementation of NumChannels
    // NumChannels=1 returns 1 channels, NumChannels={n|n>1} returns n-1 channels
    // FIXME BUG upstream: this is because it ignores channel visibility when computing start/end chanid
    var params: Map[String, Any] = Map("StartTime" -> startTime.toIsoFormat, "EndTime" -> endTime.toIsoFormat)
    if (startChanId.id != 0) params += "StartChanId" -> startChanId.id
    if (details) params += "Details" -> details
    numChannels match {
      case OptionalCountSome(n) => params += "NumChannels" -> n
      case _ => ()
    }
    val response = request("GetProgramGuide", params)
    val root = responseRoot(response, "ProgramGuide")
    root.convertTo[Guide[Channel, Program]]
  }

  /*
   * Note: for a program currently recording, the StartTS field inside the Recording
   * object will contain the actual time the recording started (which may not be the
   * same as the program start time). However, once the recording is completed, this
   * field reverts to containing the actual start time of the program, and thus is
   * unsuitable for looking up the actual recording in all cases.  TODO example
   */
  def getProgramDetails(chanId: ChanId, startTime: MythDateTime): Program = {
    val params: Map[String, Any] = Map("StartTime" -> startTime.toIsoFormat, "ChanId" -> chanId.id)
    val response = request("GetProgramDetails", params)
    val root = responseRoot(response, "Program")
    root.convertTo[Program]
  }

  def getChannelIcon(chanId: ChanId, width: Int, height: Int): HttpStreamResponse = {
    var params: Map[String, Any] = Map("ChanId" -> chanId.id)
    if (width != 0)  params += "Width" -> width
    if (height != 0) params += "Height" -> height
    requestStream("GetChannelIcon", params)
  }
}
