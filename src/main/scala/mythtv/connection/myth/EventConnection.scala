package mythtv
package connection
package myth

import java.net.InetAddress

import EnumTypes.MythProtocolEventMode

trait BackendEvent extends BackendResponse

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
