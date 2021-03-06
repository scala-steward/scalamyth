// SPDX-License-Identifier: LGPL-2.1-only
/*
 * EventConnection.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth

import java.net.SocketException
import java.util.concurrent.{ BlockingQueue, LinkedBlockingQueue }

import util.NetworkUtil
import EnumTypes.MythProtocolEventMode
import MythProtocol.MythProtocolFailure

private[myth] trait BackendEventResponse extends Any with BackendResponse {
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
  val eventMode: MythProtocolEventMode,
  initialListener: EventListener
) extends AbstractBackendConnection(host, port, BackendConnection.DefaultTimeout)
     with EventConnection {

  self: AnnouncingConnection =>

  private[this] var listenerSet: Set[EventListener] = Set.empty
  private[this] var eventLoopThread: Thread = _

  // NB be sure to implement this as a lazy val in subclasses to avoid
  // an NPE due to class initialization order!
  protected val eventProtocol: EventParser

  def announce(): Unit = {
    val localHost = NetworkUtil.myHostName
    val result = sendCommand("ANN", "Monitor", localHost, eventMode)
    // assert(result.right.get == MythProtocol.AnnounceResult.AnnounceAcknowledgement)

    // use infinite timeout in event loop, but not for initial connection
    changeTimeout(0)

    // add an initial event listener if one was specified
    if (initialListener ne null) addListener(initialListener)
  }

  override def disconnect(graceful: Boolean): Unit = {
    clearListeners()
    super.disconnect(graceful)
    if (eventLoopThread ne null) eventLoopThread.interrupt()
  }

  override def sendCommand(command: String, args: Any*): MythProtocolResult[_] = {
    if (hasAnnounced) Left(MythProtocolFailure.MythProtocolFailureUnknown)
    else super.sendCommand(command, args: _*)
  }

  def listeners: Set[EventListener] = synchronized { listenerSet }

  def addListener(listener: EventListener): Unit = {
    synchronized {
      listenerSet = listenerSet + listener
      if (!isEventLoopRunning) eventLoopThread = startEventLoop
    }
  }

  def removeListener(listener: EventListener): Unit = {
    synchronized {
      listenerSet = listenerSet - listener
    }
  }

  protected def clearListeners(): Unit = {
    synchronized {
      listenerSet = Set.empty
    }
  }

  protected def newEventResponse(eventString: String): BackendEventResponse = {
    val protocol = eventProtocol
    new BackendEventResponse {
      def raw = eventString
      def parse = protocol.parse(this)
    }
  }

  // blocking read to wait for the next event
  protected def readEvent(): BackendEventResponse = newEventResponse(reader.read())

  private def isEventLoopRunning: Boolean =
    if (eventLoopThread eq null) false
    else eventLoopThread.isAlive

  /*
  private def startEventLoopOld: Thread = {
    val thread = new Thread(new EventLoop, "Myth Event Loop")
    thread.start()
    thread
  }
  */

  private def startEventLoop: Thread = {
    val eventQueue = new LinkedBlockingQueue[BackendEventResponse]
    val monitorThread = new Thread(new EventMonitor(eventQueue), "Myth Event Monitor")
    val dispatchThread = new Thread(new EventDispatcher(eventQueue), "Myth Event Dispatcher")
    dispatchThread.start()
    monitorThread.start()
    monitorThread
  }

  // TODO is isConnected thread safe?
  private class EventMonitor(queue: BlockingQueue[BackendEventResponse]) extends Runnable {
    def run(): Unit = {
      while (isConnected) {
        try {
          val eventResponse = readEvent()
          queue.put(eventResponse)
        } catch {
          case _: SocketException => ()
          case _: InterruptedException => ()  // force next iteration of loop to re-check connected status
          case _: EndOfStreamException => disconnect(graceful = false)
        }
      }

      // Tell dispatcher thread to shut down
      var stillRunning = true
      while (stillRunning) {
        try {
          queue.put(newEventResponse(MythProtocol.Separator + "NO_MORE_EVENTS"))
          stillRunning = false
        }
        catch { case _: InterruptedException => () }
      }
    }
  }

  private class EventDispatcher(queue: BlockingQueue[BackendEventResponse]) extends Runnable {
    def run(): Unit = {
      var moreEvents = true
      while (moreEvents) {
        val eventResponse = queue.take()
        val event = eventResponse.parse

        if (event == Event.NoMoreEvents) moreEvents = false
        else dispatch(event)
      }
    }

    private def dispatch(event: Event): Unit = {
      val myListeners = listeners
      if (myListeners.nonEmpty) {
        for (ear <- myListeners) {
          if (ear.listenFor(event))
            ear.handle(event)
        }
      }
    }
  }

  private class EventLoop extends Runnable {
    def run(): Unit = {
      while (isConnected) {
        try {
          val eventResponse = readEvent()
          val event = eventResponse.parse
          val myListeners = listeners
          for (ear <- myListeners) {
            if (ear.listenFor(event))
              ear.handle(event)
          }
        } catch {
          case _: SocketException => ()
          case _: InterruptedException => ()
        }
      }
    }
  }
}

private sealed trait EventConnectionFactory {
  def apply(host: String, port: Int, eventMode: MythProtocolEventMode, listener: EventListener): EventConnection
}

// NB Important that AnnouncingConnection is listed last, for initialization order

private class EventConnection75(host: String, port: Int, eventMode: MythProtocolEventMode, listener: EventListener)
 extends AbstractEventConnection(host, port, eventMode, listener)
    with MythProtocol75
    with AnnouncingConnection {
  override protected lazy val eventProtocol = new EventProtocol75
}

private class EventConnection77(host: String, port: Int, eventMode: MythProtocolEventMode, listener: EventListener)
 extends AbstractEventConnection(host, port, eventMode, listener)
    with MythProtocol77
    with AnnouncingConnection {
  override protected lazy val eventProtocol = new EventProtocol77
}

private class EventConnection88(host: String, port: Int, eventMode: MythProtocolEventMode, listener: EventListener)
 extends AbstractEventConnection(host, port, eventMode, listener)
    with MythProtocol88
    with AnnouncingConnection {
  override protected lazy val eventProtocol = new EventProtocol88
}

private class EventConnection91(host: String, port: Int, eventMode: MythProtocolEventMode, listener: EventListener)
 extends AbstractEventConnection(host, port, eventMode, listener)
    with MythProtocol91
    with AnnouncingConnection {
  override protected lazy val eventProtocol = new EventProtocol91
}

private object EventConnection75 extends EventConnectionFactory {
  def apply(host: String, port: Int, eventMode: MythProtocolEventMode, listener: EventListener) =
    new EventConnection75(host, port, eventMode, listener)
}

private object EventConnection77 extends EventConnectionFactory {
  def apply(host: String, port: Int, eventMode: MythProtocolEventMode, listener: EventListener) =
    new EventConnection77(host, port, eventMode, listener)
}

private object EventConnection88 extends EventConnectionFactory {
  def apply(host: String, port: Int, eventMode: MythProtocolEventMode, listener: EventListener) =
    new EventConnection88(host, port, eventMode, listener)
}

private object EventConnection91 extends EventConnectionFactory {
  def apply(host: String, port: Int, eventMode: MythProtocolEventMode, listener: EventListener) =
    new EventConnection91(host, port, eventMode, listener)
}

object EventConnection {
  private val supportedVersions = Map[Int, EventConnectionFactory](
    75 -> EventConnection75,
    77 -> EventConnection77,
    88 -> EventConnection88,
    91 -> EventConnection91,
  )

  def apply(
    host: String,
    port: Int = BackendConnection.DefaultPort,
    eventMode: MythProtocolEventMode = MythProtocolEventMode.Normal,
    listener: EventListener = null
  ): EventConnection = {
    try {
      val factory = supportedVersions(BackendConnection.DefaultVersion)
      factory(host, port, eventMode, listener)
    } catch {
      case ex @ WrongMythProtocolException(requiredVersion) =>
        if (supportedVersions contains requiredVersion) {
          val factory = supportedVersions(requiredVersion)
          factory(host, port, eventMode, listener)
        }
        else throw new UnsupportedMythProtocolException(ex)
    }
  }
}
