package mythtv
package connection
package myth

import java.net.InetAddress

import scala.concurrent.duration.Duration

import EnumTypes.MythProtocolEventMode

trait BackendEvent extends Any with BackendResponse {
  def isSystemEvent: Boolean = raw.substring(20,32) == "SYSTEM_EVENT"
}

private object BackendEvent {
  def apply(r: String): BackendResponse = Event(r)
}

private final case class Event(raw: String) extends AnyVal with BackendEvent


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
  def listenFor(event: BackendEvent): Boolean  // TODO rename to filter?
  def handle(event: BackendEvent): Unit
}

trait EventConnection extends SocketConnection { /* with EventProtocol ?? */
  def addListener(listener: EventListener): Unit
  def removeListener(listener: EventListener): Unit

  final def += (listener: EventListener): Unit = addListener(listener)
  final def -= (listener: EventListener): Unit = removeListener(listener)
}

final class EventLock(eventConn: EventConnection, eventFilter: (BackendEvent) => Boolean)
    extends EventListener {
  private[this] var locked = true

  eventConn.addListener(this)

  def listenFor(event: BackendEvent): Boolean = eventFilter(event)

  def handle(event: BackendEvent): Unit = {
    synchronized {
      locked = false
      notifyAll()
    }
    eventConn.removeListener(this)
  }

  // TODO timeout doesn't do what you might think, and is of questionable use here
  def waitFor(timeout: Duration = Duration.Inf): Unit = {
    val millis = if (timeout.isFinite()) timeout.toMillis else 0
    synchronized { while (locked) wait(millis) }
  }
}

private abstract class AbstractEventConnection(host: String, port: Int, timeout: Int,
  val eventMode: MythProtocolEventMode = MythProtocolEventMode.Normal)
    extends AbstractBackendConnection(host, port, timeout) with EventConnection {

  self: AnnouncingConnection =>

  private[this] var listeners: Set[EventListener] = Set.empty
  private[this] var eventLoopThread: Thread = _

  def announce(): Unit = {
    val localHost = InetAddress.getLocalHost().getHostName() // TODO
    val result = sendCommand("ANN", "Monitor", localHost, eventMode)
  }

  override def sendCommand(command: String, args: Any*): Option[_] = {
    if (hasAnnounced) None
    else super.sendCommand(command, args: _*)
  }

  def addListener(listener: EventListener): Unit = {
    synchronized { listeners = listeners + listener }
    if (!isEventLoopRunning) eventLoopThread = startEventLoop
  }

  def removeListener(listener: EventListener): Unit = {
    synchronized { listeners = listeners - listener }
  }

  // blocking read to wait for the next event
  // TODO this only blocks for 'timeout' seconds!
  protected def readEvent(): BackendEvent = Event(reader.read())

  private def isEventLoopRunning: Boolean =
    if (eventLoopThread eq null) false
    else eventLoopThread.isAlive

  private def startEventLoop: Thread = {
    val thread = new Thread(new EventLoop)
    thread.start()
    thread
  }

  private class EventLoop extends Runnable {
    // TODO : this approach has the disadvantage that event listener de-/registration
    //   does not become visible until after the next event is received (which may not
    //   be for some time)  Can we interrupt the blocked call to process?

    // TODO need to catch SocketException: Socket closed (when disconnect() is called)
    def run(): Unit = {
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
}

// NB Important that AnnouncingConnection is listed last, for initialization order
private class EventConnection77(host: String, port: Int, eventMode: MythProtocolEventMode)
    extends AbstractEventConnection(host, port, 0, eventMode) // TODO don't use infinite timeout for protocol negotiation
    with MythProtocol77
    with AnnouncingConnection

object EventConnection {
  def apply(host: String, eventMode: MythProtocolEventMode): EventConnection =
    // TODO negotiate protocol version, use default port, etc.
    new EventConnection77(host, 6543, eventMode)
}
