package mythtv
package services

import java.time.Duration

import model._
import util.MythDateTime
import connection.http.HttpStreamResponse
import EnumTypes.{ NotificationPriority, NotificationType, NotificationVisibility }

trait MythFrontendService extends FrontendService {
  def serviceName: String = "Frontend"

  // query methods

  def getActionList(context: String = ""): FrontendActionMap
  def getContextList: List[String]
  def getStatus: FrontendStatus

  def getScreenshot(format: ScreenshotFormat = ScreenshotFormat.Png, width: Int = 0, height: Int = 0): HttpStreamResponse

  // action methods

  // TODO UPSTREAM seems to be a bug in MythTV that if the frontend is not at playbackbox,
  // then playRecording just jumps there and doesn't play. Sending the messsage
  // again will cause playback, but it will restart upon exit (guess cause we
  // send the message twice?)  I think the same buf may affect other control
  // avenues as well.
  def playRecording(chanId: ChanId, startTime: MythDateTime): Boolean

  // FIXME UPSTREAM BUG? if UseBookmark=true, then dialog still pops up about bookmark
  def playVideo(id: VideoId, useBookmark: Boolean = false): Boolean

  def sendAction(action: Action, value: String = ""): Boolean

  def sendMessage(message: String, timeout: Duration = Duration.ZERO): Boolean

  /* The implementation of notifications through services seems to be a bit buggy
     still in 0.27. If visibility is set, then the message text may be invisible.
     Also, the progress bar seems to always apppear unless notifyType is set to
     something other then the default (in which case a status icon appears instead).
     Additionally, the progress bar is painted offset to its background shadow. */
  def sendNotification(
    message: String,
    origin: String = "",
    description: String = "",
    extra: String = "",
    progressText: String = "",
    progress: Float = 0f,   /* should be decimal between 0 and 1 */
    fullScreen: Boolean = false,
    timeout: Duration = Duration.ZERO,
    notifyType: NotificationType = NotificationType.New,
    priority: NotificationPriority = NotificationPriority.Default,
    visibility: NotificationVisibility = NotificationVisibility.All
  ): Boolean
}
