package mythtv
package connection
package myth

import java.net.SocketException

import util.NetworkUtil
import EnumTypes.MythProtocolEventMode

trait BackendEvent extends Any with BackendResponse {
  def isSystemEvent: Boolean = raw.startsWith("SYSTEM_EVENT", 20)
  def isEventName(name: String): Boolean = raw.startsWith(name, 20)
  def parse: Event = (new EventParser).parse(this)  // TODO
}

private object BackendEvent {
  def apply(r: String): BackendResponse = RawEvent(r)
}

private final case class RawEvent(raw: String) extends AnyVal with BackendEvent

trait EventListener {
  def listenFor(event: BackendEvent): Boolean  // TODO rename to filter?
  def handle(event: BackendEvent): Unit
}

trait EventConnection extends SocketConnection { /* with EventProtocol ?? */
  def addListener(listener: EventListener): Unit
  def removeListener(listener: EventListener): Unit

  def listeners: Set[EventListener]

  final def += (listener: EventListener): Unit = addListener(listener)
  final def -= (listener: EventListener): Unit = removeListener(listener)
}

// TODO don't use infinite timeout for protocol negotiation
private abstract class AbstractEventConnection(
  host: String, port: Int, val eventMode: MythProtocolEventMode)
    extends AbstractBackendConnection(host, port, 0) with EventConnection {

  self: AnnouncingConnection =>

  private[this] var listenerSet: Set[EventListener] = Set.empty
  private[this] var eventLoopThread: Thread = _

  def announce(): Unit = {
    val localHost = NetworkUtil.myHostName
    val result = sendCommand("ANN", "Monitor", localHost, eventMode)
  }

  override def sendCommand(command: String, args: Any*): Option[_] = {
    if (hasAnnounced) None
    else super.sendCommand(command, args: _*)
  }

  def listeners: Set[EventListener] = synchronized { listenerSet }

  def addListener(listener: EventListener): Unit = {
    synchronized { listenerSet = listenerSet + listener }
    if (!isEventLoopRunning) eventLoopThread = startEventLoop
  }

  def removeListener(listener: EventListener): Unit = {
    synchronized { listenerSet = listenerSet - listener }
  }

  // blocking read to wait for the next event
  // TODO this only blocks for 'timeout' seconds!
  protected def readEvent(): BackendEvent = RawEvent(reader.read())

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
      var myListeners = listeners
      while (myListeners.nonEmpty && isConnected) {
        try {
          val event = readEvent()
          myListeners = listeners
          for (ear <- myListeners) {
            if (ear.listenFor(event))
              ear.handle(event)
          }
        } catch {
          case ex: SocketException =>
        }
      }
    }
  }
}

private sealed trait EventConnectionFactory {
  def apply(host: String, port: Int, eventMode: MythProtocolEventMode): EventConnection
}

// NB Important that AnnouncingConnection is listed last, for initialization order

private class EventConnection75(host: String, port: Int, eventMode: MythProtocolEventMode)
    extends AbstractEventConnection(host, port, eventMode)
    with MythProtocol75
    with AnnouncingConnection

private object EventConnection75 extends EventConnectionFactory {
  def apply(host: String, port: Int, eventMode: MythProtocolEventMode) =
    new EventConnection75(host, port, eventMode)
}

private class EventConnection77(host: String, port: Int, eventMode: MythProtocolEventMode)
    extends AbstractEventConnection(host, port, eventMode)
    with MythProtocol77
    with AnnouncingConnection

private object EventConnection77 extends EventConnectionFactory {
  def apply(host: String, port: Int, eventMode: MythProtocolEventMode) =
    new EventConnection77(host, port, eventMode)
}


object EventConnection {
  private val supportedVersions = Map[Int, EventConnectionFactory](
    75 -> EventConnection75,
    77 -> EventConnection77
  )

  def apply(
    host: String,
    port: Int = BackendConnection.DefaultPort,
    eventMode: MythProtocolEventMode = MythProtocolEventMode.Normal
  ): EventConnection = {
    try {
      val factory = supportedVersions(BackendConnection.DefaultVersion)
      factory(host, port, eventMode)
    } catch {
      case ex @ WrongMythProtocolException(requiredVersion) =>
        if (supportedVersions contains requiredVersion) {
          val factory = supportedVersions(requiredVersion)
          factory(host, port, eventMode)
        }
        else throw new UnsupportedMythProtocolException(ex)
    }
  }
}
