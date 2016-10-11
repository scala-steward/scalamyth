package mythtv
package connection
package myth

import java.net.InetAddress

import EnumTypes.MythProtocolEventMode

trait BackendEvent extends BackendResponse {
  def isSystemEvent: Boolean = raw.substring(20,32) == "SYSTEM_EVENT"
}

/*
 * Some BACKEND_MESSAGE examples
 *
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT NET_CTRL_CONNECTED SENDER mythfe1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT NET_CTRL_DISCONNECTED SENDER mythfe2[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT SCREEN_TYPE DESTROYED playbackbox SENDER mythfe1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT SCREEN_TYPE CREATED mythscreentypebusydialog SENDER mythfe1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT CLIENT_CONNECTED HOSTNAME myth1 SENDER myth1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT CLIENT_DISCONNECTED HOSTNAME myth1 SENDER myth1[]:[]empty
 *  BACKEND_MESSAGE[]:[]VIDEO_LIST_NO_CHANGE[]:[]empty
 *  BACKEND_MESSAGE[]:[]RECORDING_LIST_CHANGE UPDATE[]:[]Survivor[]:[]Not Going Down Without a Fight[]:[]Castaways from all three tribes remain, and one will be crowned the Sole Survivor.[]:[]32[]:[]14[]:[]3214[]:[]Reality[]:[]1081[]:[]8-1[]:[]KFMB-DT[]:[]KFMBDT (KFMB-DT)[]:[]1081_20160519030000.mpg[]:[]13323011228[]:[]1463626800[]:[]1463634000[]:[]0[]:[]myth1[]:[]0[]:[]0[]:[]0[]:[]0[]:[]-3[]:[]380[]:[]0[]:[]15[]:[]6[]:[]1463626800[]:[]1463634001[]:[]11583492[]:[]Reality TV[]:[][]:[]EP00367078[]:[]EP003670780116[]:[]76733[]:[]1471849923[]:[]0[]:[]2016-05-18[]:[]Default[]:[]0[]:[]0[]:[]Default[]:[]9[]:[]17[]:[]1[]:[]0[]:[]0[]:[]0
 *  BACKEND_MESSAGE[]:[]GENERATED_PIXMAP[]:[]OK[]:[]1081_2016-05-19T05:00:00Z[]:[]Generated on myth1 in 3.919 seconds, starting at 16:19:33[]:[]2016-10-05T23:19:33Z[]:[]83401[]:[]39773[]:[] <<< base64 data redacted >>>
 */

trait EventListener {
  def listenFor(event: BackendEvent): Boolean
  def handle(event: BackendEvent): Unit
}

trait EventConnection extends SocketConnection { /* with EventProtocol ?? */
  def addListener(listener: EventListener): Unit
  def removeListener(listener: EventListener): Unit

  def += (listener: EventListener): Unit = addListener(listener)
  def -= (listener: EventListener): Unit = removeListener(listener)
}

abstract class AbstractEventConnection(host: String, port: Int, timeout: Int,
  val eventMode: MythProtocolEventMode = MythProtocolEventMode.Normal)
    extends AbstractBackendConnection(host, port, timeout) with EventConnection {

  private[this] var announced = false
  private[this] var listeners: Set[EventListener] = Set.empty

  override protected def finishConnect(): Unit = {
    super.finishConnect()
    announce()
  }

  protected def announce(): Unit = {
    val localHost = InetAddress.getLocalHost().getHostName()   // TODO
    val result = sendCommand("ANN", "Monitor", localHost, eventMode)
    announced = true
  }

  override def sendCommand(command: String, args: Any*): Option[_] = {
    if (announced) None
    else super.sendCommand(command, args)
  }

  def addListener(listener: EventListener): Unit = {
    synchronized { listeners = listeners + listener }
  }

  def removeListener(listener: EventListener): Unit = {
    synchronized { listeners = listeners - listener }
  }

  protected def readEvent(): BackendEvent  // TODO blocking read to wait for the next event

  private def eventLoop(): Unit = {
    // TODO : this approach has the disadvantage that event listener de-/registration
    //   does not become visible until after the next event is received (which may not
    //   be for some time)  Can we interrupt the blocked call to process?
    var myListeners = synchronized { listeners }
    while (myListeners.nonEmpty && isConnected) {
      val event = readEvent()
      myListeners = synchronized { listeners }
      for (ear <- myListeners) {
        if (ear.listenFor(event))
          ear.handle(event)
      }
    }
  }
}
