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
   *   SendMessage           POST ==> Boolean                  (Message)[Timeout]
   *        Timeout only applies during playback? Unit is seconds, valid range (0, 1000) exclusive
   *   SendNotification      POST ==> Boolean                  (Message)[lots of optional params]
   *
   *   SendAction
   *
   *   PlayRecording         POST ==> Boolean                  (ChanId, StartTime)
   *   PlayVideo             POST ==> Boolean                  (Id)[UseBookmark]
   */
}
