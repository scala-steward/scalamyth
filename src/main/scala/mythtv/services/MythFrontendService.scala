package mythtv
package services

import java.time.Duration

import model._
import util.MythDateTime
import connection.http.HttpStreamResponse
import EnumTypes.{ NotificationPriority, NotificationType, NotificationVisibility }
import MythFrontend.KeyName

trait MythFrontendService extends FrontendService {
  final def serviceName: String = "Frontend"

  // query methods

  def getActionList(context: String = ""): ServiceResult[FrontendActionMap]
  def getContextList: ServiceResult[List[String]]
  def getStatus: ServiceResult[FrontendStatus]

  def getScreenshot[U](format: ScreenshotFormat = ScreenshotFormat.Png, width: Int = 0, height: Int = 0)(f: (HttpStreamResponse) => U): ServiceResult[Unit]

  // action methods

  // FIXME UPSTREAM seems to be a bug in MythTV that if the frontend is not at playbackbox,
  // then playRecording just jumps there and doesn't play. Sending the messsage
  // again will cause playback, but it will restart upon exit (guess cause we
  // send the message twice?)  I think the same buf may affect other control
  // avenues as well.
  def playRecording(chanId: ChanId, startTime: MythDateTime): ServiceResult[Boolean]
  def playRecording(recordedId: RecordedId): ServiceResult[Boolean]

  // FIXME UPSTREAM BUG? if UseBookmark=true, then dialog still pops up about bookmark
  def playVideo(id: VideoId, useBookmark: Boolean = false): ServiceResult[Boolean]

  def sendAction(action: Action, value: String = ""): ServiceResult[Boolean]

  // sendKey is new for MythTV 0.28
  def sendKey(key: KeyName): ServiceResult[Boolean]

  def sendMessage(message: String, timeout: Duration = Duration.ZERO): ServiceResult[Boolean]

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
  ): ServiceResult[Boolean]
}
