package mythtv
package connection

import java.net.Socket
import java.io.{ InputStream, OutputStream }

import scala.util.DynamicVariable

trait SocketConnection extends NetworkConnection {
  def isConnected: Boolean
  def disconnect(graceful: Boolean = true): Unit
  def reconnect(graceful: Boolean = true): Unit
  def timeout: Int
  def withTimeout[T](timeOut: Int)(thunk: => T): T
}

// TODO is 'host' a hostname or IP address (or either?)
abstract class AbstractSocketConnection[A](val host: String, val port: Int, timeoutSecs: Int)
    extends SocketConnection {
  private[this] var timeoutVar = new DynamicVariable[Int](timeoutSecs)
  private[this] var connected: Boolean = false
  private[this] var socket: Socket = connectSocket()
  private[this] var socketReader: SocketReader[A] = openReader(inputStream)
  private[this] var socketWriter: SocketWriter[A] = openWriter(outputStream)

  // TODO management of reader/writer lifecycle

  finishConnect()

  def isConnected: Boolean = connected

  private def connectSocket(): Socket = {
    val newSocket = new Socket(host, port)
    setSocketTimeout(newSocket, timeout)
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

  protected def openReader(inStream: InputStream): SocketReader[A]

  protected def openWriter(outStream: OutputStream): SocketWriter[A]

  def timeout: Int = timeoutVar.value

  def withTimeout[T](timeOut: Int)(thunk: => T): T = {
    val result = timeoutVar.withValue(timeOut) {
      setSocketTimeout(socket, timeout)
      thunk
    }
    setSocketTimeout(socket, timeout)
    result
  }

  private def setSocketTimeout(sock: Socket, secs: Int): Unit =
    sock.setSoTimeout(secs * 1000)

  protected def inputStream: InputStream = socket.getInputStream
  protected def outputStream: OutputStream = socket.getOutputStream

  protected def reader: SocketReader[A] = socketReader
  protected def writer: SocketWriter[A] = socketWriter
}
