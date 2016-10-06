package mythtv
package connection

import java.net.Socket
import java.io.{ InputStream, OutputStream }

trait SocketConnection extends NetworkConnection {
  def isConnected: Boolean
  def disconnect(graceful: Boolean = true): Unit
  def reconnect(graceful: Boolean = true): Unit
}

// TODO is 'host' a hostname or IP address (or either?)
abstract class AbstractSocketConnection(val host: String, val port: Int, val timeout: Int)
    extends SocketConnection {
  private[this] var connected: Boolean = false
  private[this] var socket: Socket = connectSocket()

  // TODO management of reader/writer lifecycle

  finishConnect()

  def isConnected: Boolean = connected

  private def connectSocket(): Socket = {
    val newSocket = new Socket(host, port)
    newSocket.setSoTimeout(10 * 1000)
    connected = true
    newSocket
  }

  protected def finishConnect(): Unit

  protected def connect(): Unit = {
    if (!connected) {
      socket = connectSocket()
      finishConnect()
    }
  }

  def disconnect(graceful: Boolean): Unit = {
    if (connected) {
      if (graceful) gracefulDisconnect()
      connected = false
      socket.shutdownOutput()
      socket.close()
    }
  }

  protected def gracefulDisconnect(): Unit

  def reconnect(graceful: Boolean): Unit = {
    disconnect(graceful)
    connect()
  }

  protected def inputStream: InputStream = socket.getInputStream
  protected def outputStream: OutputStream = socket.getOutputStream
}
