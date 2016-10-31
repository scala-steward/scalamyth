package mythtv
package connection
package http

import model.{ ChanId, FrontendActionMap, FrontendStatus }
import services.MythFrontendService
import util.MythDateTime


abstract class JsonFrontendService(conn: FrontendJsonConnection)
  extends JsonService(conn)
     with FrontendServiceProtocol
     with FrontendJsonProtocol


class JsonMythFrontendService(conn: FrontendJsonConnection)
  extends JsonFrontendService(conn)
     with MythFrontendService {

  def getActionList(context: String = ""): FrontendActionMap = {
    var params: Map[String, Any] = Map.empty
    if (context.nonEmpty) params += "Context" -> context
    val response = request("GetActionList", params)
    val root = responseRoot(response, "FrontendActionList")
    root.convertTo[FrontendActionMap]
  }

  def getContextList: List[String] = {
    val response = request("GetContextList")
    val root = responseRoot(response)
    root.convertTo[List[String]]
  }

  def getStatus: FrontendStatus = {
    val response = request("GetStatus")
    val root = responseRoot(response, "FrontendStatus")
    root.convertTo[FrontendStatus]
  }

  // action methods

  def playRecording(chanId: ChanId, startTime: MythDateTime): Boolean = ???
  def playVideo(id: Int, useBookmark: Boolean = false): Boolean = ???
  def sendAction(action: String): Boolean = ???
  def sendMessage(message: String): Boolean = ???
  def sendNotification(message: String): Boolean = ???
}
