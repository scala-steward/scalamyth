package mythtv
package services

import model._
import util.{ OptionalCount, MythDateTime, MythFileHash }

trait Service {
  def serviceName: String
}

trait BackendService extends Service
trait FrontendService extends Service

trait DataBytes  // TODO placeholder

trait MythFrontendService extends FrontendService {
  def serviceName: String = "Frontend"

  // query methods

  def getActionList: List[FrontendAction]   // the data here is really more like a map (action is a k/v tuple)
  def getContextList: List[String]
  def getStatus: FrontendStatus

  // action methods

  def playRecording(chanId: ChanId, startTime: MythDateTime): Boolean
  def playVideo(id: Int, useBookmark: Boolean = false): Boolean  // TODO use 0/1 instead of true/false ?
  def sendAction(action: String): Boolean // TODO optional params for SCREENSHOT; this method is controversial?
  def sendMessage(message: String): Boolean
  def sendNotification(message: String): Boolean  // TODO lots and lots of optional parameters (12) make a Notification class?
}
