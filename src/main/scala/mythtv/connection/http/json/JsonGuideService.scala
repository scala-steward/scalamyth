package mythtv
package connection
package http
package json

import scala.util.Try

import services.{ GuideService, ServiceResult }
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
  ): ServiceResult[Guide[Channel, Program]] = {
    // There seems to be an off-by-one error on my 0.27 backend implementation of NumChannels
    // NumChannels=1 returns 1 channels, NumChannels={n|n>1} returns n-1 channels
    // FIXME UPSTREAM: this is because it ignores channel visibility when computing start/end chanid
    var params: Map[String, Any] = Map("StartTime" -> startTime.toIsoFormat, "EndTime" -> endTime.toIsoFormat)
    if (startChanId.id != 0) params += "StartChanId" -> startChanId.id
    if (details) params += "Details" -> details
    numChannels match {
      case OptionalCountSome(n) => params += "NumChannels" -> n
      case _ => ()
    }
    for {
      response <- request("GetProgramGuide", params)
      root     <- responseRoot(response, "ProgramGuide")
      result   <- Try(root.convertTo[Guide[Channel, Program]])
    } yield result
  }

  /*
   * Note: for a program currently recording, the StartTS field inside the Recording
   * object will contain the actual time the recording started (which may not be the
   * same as the program start time). However, once the recording is completed, this
   * field reverts to containing the actual start time of the program, and thus is
   * unsuitable for looking up the actual recording in all cases.  TODO example
   */
  def getProgramDetails(chanId: ChanId, startTime: MythDateTime): ServiceResult[Program] = {
    val params: Map[String, Any] = Map("StartTime" -> startTime.toIsoFormat, "ChanId" -> chanId.id)
    for {
      response <- request("GetProgramDetails", params)
      root     <- responseRoot(response, "Program")
      result   <- Try(root.convertTo[Program])
    } yield result
  }

  def getChannelIcon[U](chanId: ChanId, width: Int, height: Int)(f: (HttpStreamResponse) => U): ServiceResult[Unit] = {
    var params: Map[String, Any] = Map("ChanId" -> chanId.id)
    if (width != 0)  params += "Width" -> width
    if (height != 0) params += "Height" -> height

    Try {
      val response = requestStream("GetChannelIcon", params)
      streamResponse(response, f)
    }
  }
}
