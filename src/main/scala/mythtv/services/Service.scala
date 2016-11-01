package mythtv
package services

import java.time.Duration

import model._
import util.MythDateTime
import EnumTypes.{ NotificationPriority, NotificationType, NotificationVisibility }

trait Service {
  def serviceName: String
}

trait BackendService extends Service
trait FrontendService extends Service

trait DataBytes  // TODO placeholder

trait MythFrontendService extends FrontendService {
  def serviceName: String = "Frontend"

  // query methods

  def getActionList(context: String = ""): FrontendActionMap
  def getContextList: List[String]
  def getStatus: FrontendStatus

  // action methods

  // TODO seems to be a bug in MythTV that if the frontend is not at playbackbox,
  // then playRecording just jumps there and doesn't play. Sending the messsage
  // again will cause playback, but it will restart upon exit (guess cause we
  // send the message twice?)  I think the same buf may affect other control
  // avenues as well.
  def playRecording(chanId: ChanId, startTime: MythDateTime): Boolean

  def playVideo(id: VideoId, useBookmark: Boolean = false): Boolean  // TODO use 0/1 instead of true/false ???

  def sendAction(action: String): Boolean // TODO optional params for SCREENSHOT; this method is controversial?

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
