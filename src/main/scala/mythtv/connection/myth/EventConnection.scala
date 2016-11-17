package mythtv
package connection
package myth

import java.net.SocketException
import java.util.concurrent.{ BlockingQueue, LinkedBlockingQueue }

import util.NetworkUtil
import EnumTypes.MythProtocolEventMode
import MythProtocol.MythProtocolFailure

trait BackendEventResponse extends Any with BackendResponse {
  def isSystemEvent: Boolean = raw.startsWith("SYSTEM_EVENT", 20)
  def isEventName(name: String): Boolean = raw.startsWith(name, 20)
  def parse: Event
}

trait EventListener {
  def listenFor(event: Event): Boolean
  def handle(event: Event): Unit
}

trait EventConnection extends SocketConnection { /* with EventProtocol ?? */
  def addListener(listener: EventListener): Unit
  def removeListener(listener: EventListener): Unit

  def listeners: Set[EventListener]

  final def += (listener: EventListener): Unit = addListener(listener)
  final def -= (listener: EventListener): Unit = removeListener(listener)
}

private abstract class AbstractEventConnection(
  host: String,
  port: Int,
  val eventMode: MythProtocolEventMode
) extends AbstractBackendConnection(host, port, BackendConnection.DefaultTimeout)
     with EventConnection {

  self: AnnouncingConnection =>

  private[this] var listenerSet: Set[EventListener] = Set.empty
  private[this] var eventLoopThread: Thread = _

  def announce(): Unit = {
    val localHost = NetworkUtil.myHostName
    val result = sendCommand("ANN", "Monitor", localHost, eventMode)
    assert(result.right.get == MythProtocol.AnnounceResult.AnnounceAcknowledgement)

    // use infinite timeout in event loop, but not for initial connection
    changeTimeout(0)
  }

  override def sendCommand(command: String, args: Any*): MythProtocolResult[_] = {
    if (hasAnnounced) Left(MythProtocolFailure.MythProtocolFailureUnknown)
    else super.sendCommand(command, args: _*)
  }

  def listeners: Set[EventListener] = synchronized { listenerSet }

  def addListener(listener: EventListener): Unit = {
    synchronized { listenerSet = listenerSet + listener }
    if (!isEventLoopRunning) eventLoopThread = startEventLoop  // TODO this probably needs synchronization too
  }

  def removeListener(listener: EventListener): Unit = {
    synchronized { listenerSet = listenerSet - listener }
  }

  protected def newEventResponse(eventString: String): BackendEventResponse

  // blocking read to wait for the next event
  protected def readEvent(): BackendEventResponse = newEventResponse(reader.read())

  private def isEventLoopRunning: Boolean =
    if (eventLoopThread eq null) false
    else eventLoopThread.isAlive

  private def startEventLoopOld: Thread = {
    val thread = new Thread(new EventLoop)
    thread.start()
    thread
  }

  private def startEventLoop: Thread = {
    val eventQueue = new LinkedBlockingQueue[BackendEventResponse]
    val monitorThread = new Thread(new EventMonitor(eventQueue))
    val dispatchThread = new Thread(new EventDispatcher(eventQueue))
    dispatchThread.start()
    monitorThread.start()
    monitorThread
  }

  // TODO is isConnected thread safe?
  private class EventMonitor(queue: BlockingQueue[BackendEventResponse]) extends Runnable {
    def run(): Unit = {
      while (listeners.nonEmpty && isConnected) {
        try {
          val eventResponse = readEvent()
          queue.put(eventResponse)
        } catch {
          case e: SocketException => ()
        }
      }
    }
  }

  private class EventDispatcher(queue: BlockingQueue[BackendEventResponse]) extends Runnable {
    def run(): Unit = {
      while (true) {  // TODO have some way to exit the thread, maybe a "posion pill" ?
        val eventResponse = queue.take()
        val myListeners = listeners
        if (myListeners.nonEmpty) {
          val event = eventResponse.parse
          for (ear <- myListeners) {
            if (ear.listenFor(event))
              ear.handle(event)
          }
        }
      }
    }
  }

  private class EventLoop extends Runnable {
    // TODO : this approach has the disadvantage that event listener de-/registration
    //   does not become visible until after the next event is received (which may not
    //   be for some time)  Can we interrupt the blocked call to process?
    def run(): Unit = {
      var myListeners = listeners
      while (myListeners.nonEmpty && isConnected) {
        try {
          val eventResponse = readEvent()
          val event = eventResponse.parse
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
    with AnnouncingConnection {
  private[this] val eventParser = new EventParserImpl

  override protected def newEventResponse(eventString: String): BackendEventResponse = {
    val parser = eventParser
    new BackendEventResponse {
      def raw = eventString
      def parse = parser.parse(this)
    }
  }
}

private object EventConnection75 extends EventConnectionFactory {
  def apply(host: String, port: Int, eventMode: MythProtocolEventMode) =
    new EventConnection75(host, port, eventMode)
}

private class EventConnection77(host: String, port: Int, eventMode: MythProtocolEventMode)
 extends AbstractEventConnection(host, port, eventMode)
    with MythProtocol77
    with AnnouncingConnection {
  private[this] val eventParser = new EventParserImpl

  override protected def newEventResponse(eventString: String): BackendEventResponse = {
    val parser = eventParser
    new BackendEventResponse {
      def raw = eventString
      def parse = parser.parse(this)
    }
  }
}

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
