package mythtv
package connection
package http

import services.Service

trait FrontendServiceProtocol extends MythServiceProtocol {
  self: Service =>
  /*
   * Frontend/
   *   GetActionList         GET ==> { FrontendActionList }    [Context]
   *   GetContextList        GET ==> { StringList }            ()
   *   GetStatus             GET ==> { FrontendStatus }        ()
   *
   *   SendMessage
   *   SendNotification
   *   SendAction
   *
   *   PlayRecording
   *   PlayVideo
   */
}
