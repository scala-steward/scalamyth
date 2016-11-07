package mythtv
package connection
package http
package json

import java.time.Duration

import model._
import util.MythDateTime
import services.MythFrontendService
import EnumTypes.{ NotificationPriority, NotificationType, NotificationVisibility }

import RichJsonObject._

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

  def getScreenshot(format: ScreenshotFormat, width: Int, height: Int): HttpStreamResponse = {
    var params: Map[String, Any] = Map("format" -> format.formatString)
    if (width != 0)      params += "width" -> width
    if (height != 0)     params += "height" -> height
    val path = buildPath("MythFE", "GetScreenShot", params)
    conn.requestStream(path)
  }

  // post methods

  def playRecording(chanId: ChanId, startTime: MythDateTime): Boolean = {
    val params: Map[String, Any] = Map(
      "ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    val response = post("PlayRecording", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  def playVideo(id: VideoId, useBookmark: Boolean = false): Boolean = {
    var params: Map[String, Any] = Map("Id" -> id.id)
    if (useBookmark) params += "UseBookmark" -> useBookmark
    val response = post("PlayVideo", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  // TODO support screenshot action by parsing WxH out of value string?
  def sendAction(action: Action, value: String): Boolean = {
    var params: Map[String, Any] = Map("Action" -> action)
    if (value.nonEmpty) params += "Value" -> value
    val response = post("SendAction", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  def sendMessage(message: String, timeout: Duration): Boolean = {
    var params: Map[String, Any] = Map("Message" -> message)
    if (!timeout.isZero) params += "Timeout" -> timeout.getSeconds
    val response = post("SendMessage", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  def sendNotification(
    message: String,
    origin: String,
    description: String,
    extra: String,
    progressText: String,
    progress: Float,
    fullScreen: Boolean,
    timeout: Duration,
    notifyType: NotificationType,
    priority: NotificationPriority,
    visibility: NotificationVisibility
  ): Boolean = {
    var params: Map[String, Any] = Map("Message" -> message)
    if (origin.nonEmpty)       params +=       "Origin" -> origin
    if (description.nonEmpty)  params +=  "Description" -> description
    if (extra.nonEmpty)        params +=        "Extra" -> extra
    if (progressText.nonEmpty) params += "ProgressText" -> progressText
    if (progress != 0f)        params +=     "Progress" -> progress
    if (fullScreen)            params +=   "Fullscreen" -> fullScreen
    if (!timeout.isZero)       params +=      "Timeout" -> timeout.getSeconds
    if (notifyType != NotificationType.New)       params +=       "Type" -> notifyType.toString.toLowerCase
    if (priority != NotificationPriority.Default) params +=   "Priority" -> priority.id
    if (visibility != NotificationVisibility.All) params += "Visibility" -> visibility.id

    val response = post("SendNotification", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }
}
