package mythtv
package connection
package http
package json

import java.time.Duration

import scala.util.Try

import model._
import util.MythDateTime
import services.{ MythFrontendService, ServiceResult }
import EnumTypes.{ NotificationPriority, NotificationType, NotificationVisibility }
import MythFrontend.KeyName
import RecordedId._

import RichJsonObject._

class JsonMythFrontendService(conn: FrontendJsonConnection)
  extends JsonFrontendService(conn)
     with MythFrontendService {

  def getActionList(context: String = ""): ServiceResult[FrontendActionMap] = {
    var params: Map[String, Any] = Map.empty
    if (context.nonEmpty) params += "Context" -> context
    for {
      response <- request("GetActionList", params)
      root     <- responseRoot(response, "FrontendActionList")
      result   <- Try(root.convertTo[FrontendActionMap])
    } yield result
  }

  def getContextList: ServiceResult[List[String]] = {
    for {
      response <- request("GetContextList")
      root     <- responseRoot(response, "StringList")
      result   <- Try(root.convertTo[List[String]])
    } yield result
  }

  def getStatus: ServiceResult[FrontendStatus] = {
    for {
      response <- request("GetStatus")
      root     <- responseRoot(response, "FrontendStatus")
      result   <- Try(root.convertTo[FrontendStatus])
    } yield result
  }

  def getScreenshot[U](format: ScreenshotFormat, width: Int, height: Int)(f: (HttpStreamResponse) => U): ServiceResult[Unit] = {
    var params: Map[String, Any] = Map("format" -> format.formatString)
    if (width != 0)      params += "width" -> width
    if (height != 0)     params += "height" -> height
    val path = buildPath("MythFE", "GetScreenShot", params)  // Note this has a different service prefix
    Try {
      val response = conn.requestStream(path)
      streamResponse(response, f)
    }
  }

  // post methods

  def playRecording(chanId: ChanId, startTime: MythDateTime): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "ChanId" -> chanId.id, "StartTime" -> startTime.toIsoFormat)
    for {
      response <- post("PlayRecording", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def playRecording(recordedId: RecordedId): ServiceResult[Boolean] = recordedId match {
    case RecordedIdInt(id) =>
      val params: Map[String, Any] = Map("RecordedId" -> id)
      for {
        response <- post("PlayRecording", params)
        root     <- responseRoot(response)
        result   <- Try(root.booleanField("bool"))
      } yield result
    case RecordedIdChanTime(chanId, startTime) => playRecording(chanId, startTime)
  }

  def playVideo(id: VideoId, useBookmark: Boolean = false): ServiceResult[Boolean] = {
    var params: Map[String, Any] = Map("Id" -> id.id)
    if (useBookmark) params += "UseBookmark" -> useBookmark
    for {
      response <- post("PlayVideo", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  // TODO support screenshot action by parsing WxH out of value string?
  def sendAction(action: Action, value: String): ServiceResult[Boolean] = {
    var params: Map[String, Any] = Map("Action" -> action)
    if (value.nonEmpty) params += "Value" -> value
    for {
      response <- post("SendAction", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def sendKey(key: KeyName): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("Key" -> key)
    for {
      response <- post("SendKey", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def sendMessage(message: String, timeout: Duration): ServiceResult[Boolean] = {
    var params: Map[String, Any] = Map("Message" -> message)
    if (!timeout.isZero) params += "Timeout" -> timeout.getSeconds
    for {
      response <- post("SendMessage", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
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

    for {
      response <- post("SendNotification", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }
}
