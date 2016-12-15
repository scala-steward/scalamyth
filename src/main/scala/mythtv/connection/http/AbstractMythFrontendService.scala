package mythtv
package connection
package http

import java.time.Duration

import scala.util.Try

import model._
import util.MythDateTime
import services.{ MythFrontendService, ServiceResult }
import EnumTypes.{ NotificationPriority, NotificationType, NotificationVisibility }
import MythFrontend.KeyName
import RecordedId._

trait AbstractMythFrontendService extends ServiceProtocol with MythFrontendService {

  def getActionList(context: String): ServiceResult[FrontendActionMap] = {
    var params: Map[String, Any] = Map.empty
    if (context.nonEmpty) params += "Context" -> context
    request("GetActionList", params)("FrontendActionList")
  }

  def getContextList: ServiceResult[List[String]] = {
    request("GetContextList")()
  }

  def getStatus: ServiceResult[FrontendStatus] = {
    request("GetStatus")("FrontendStatus")
  }

  def getScreenshot[U](format: ScreenshotFormat, width: Int, height: Int)(f: (HttpStreamResponse) => U): ServiceResult[Unit] = {
    var params: Map[String, Any] = Map("format" -> format.formatString)
    if (width != 0)      params += "width" -> width
    if (height != 0)     params += "height" -> height
    val path = buildPath("MythFE", "GetScreenShot", params)  // Note this has a different service prefix
    Try {
      val response = requestStream(path)
      streamResponse(response, f)
    }
  }

  // post methods

  def playRecording(chanId: ChanId, startTime: MythDateTime): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    post("PlayRecording", params)()
  }

  def playRecording(recordedId: RecordedId): ServiceResult[Boolean] = recordedId match {
    case RecordedIdInt(id) => post("PlayRecording", Map("RecordedId" -> id))()
    case RecordedIdChanTime(chanId, startTime) => playRecording(chanId, startTime)
  }

  def playVideo(id: VideoId, useBookmark: Boolean = false): ServiceResult[Boolean] = {
    var params: Map[String, Any] = Map("Id" -> id.id)
    if (useBookmark) params += "UseBookmark" -> useBookmark
    post("PlayVideo", params)()
  }

  // TODO support screenshot action by parsing WxH out of value string?
  def sendAction(action: Action, value: String): ServiceResult[Boolean] = {
    var params: Map[String, Any] = Map("Action" -> action)
    if (value.nonEmpty) params += "Value" -> value
    post("SendAction", params)()
  }

  def sendKey(key: KeyName): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("Key" -> key)
    post("SendKey", params)()
  }

  def sendMessage(message: String, timeout: Duration): ServiceResult[Boolean] = {
    var params: Map[String, Any] = Map("Message" -> message)
    if (!timeout.isZero) params += "Timeout" -> timeout.getSeconds
    post("SendMessage", params)()
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
  ): ServiceResult[Boolean] = {
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
    post("SendNotification", params)()
  }
}
