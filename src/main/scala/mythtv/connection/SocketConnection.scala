package mythtv
package connection

import java.io.{ InputStream, OutputStream }
import java.net.{ InetSocketAddress, Socket }
import java.nio.channels.SocketChannel

import scala.util.DynamicVariable

trait SocketConnection extends NetworkConnection {
  def isConnected: Boolean
  def disconnect(graceful: Boolean = true): Unit
  def reconnect(graceful: Boolean = true): Unit
  def timeout: Int
  def withTimeout[T](timeOut: Int)(thunk: => T): T
}

abstract class AbstractSocketConnection[A](val host: String, val port: Int, timeoutSecs: Int)
    extends SocketConnection {
  private[this] val timeoutVar = new DynamicVariable[Int](timeoutSecs)
  private[this] var connected: Boolean = false
  private[this] var channel: SocketChannel = connectSocketChannel()
  private[this] var socketReader: SocketReader[A] = openReader(channel)
  private[this] var socketWriter: SocketWriter[A] = openWriter(channel)

  // TODO management of reader/writer lifecycle

  finishConnect()

  def isConnected: Boolean = connected

  private def connectSocketChannel(): SocketChannel = {
    val newChannel = SocketChannel.open(new InetSocketAddress(host, port))
    //newChannel.configureBlocking(false)
    connected = true
    newChannel
  }

  /*
  private def connectSocket(): Socket = {
    val newSocket = new Socket(host, port)
    if (timeout > 0) setSocketTimeout(newSocket, timeout)
    connected = true
    newSocket
  }
  */

  protected def finishConnect(): Unit

  protected def connect(): Unit = {
    if (!connected) {
      channel = connectSocketChannel()
      socketReader = openReader(channel)
      socketWriter = openWriter(channel)
      finishConnect()
    }
  }

  def disconnect(graceful: Boolean): Unit = {
    if (connected) {
      if (graceful) gracefulDisconnect()
      connected = false
      channel.shutdownOutput()
      channel.close()
    }
  }

  protected def gracefulDisconnect(): Unit

  def reconnect(graceful: Boolean): Unit = {
    disconnect(graceful)
    connect()
  }

  protected def openReader(channel: SocketChannel): SocketReader[A]

  protected def openWriter(channel: SocketChannel): SocketWriter[A]

  def timeout: Int = timeoutVar.value

  def withTimeout[T](timeOut: Int)(thunk: => T): T = {
    /*
    val result = timeoutVar.withValue(timeOut) {
      setSocketTimeout(socket, timeout)
      thunk
    }
    setSocketTimeout(socket, timeout)
    result
    */
    thunk   // TODO re-implement
  }

  /*
  private def setSocketTimeout(sock: Socket, secs: Int): Unit =
    sock.setSoTimeout(secs * 1000)
    */

  protected def inputStream: InputStream = channel.socket.getInputStream
  protected def outputStream: OutputStream = channel.socket.getOutputStream

  protected def reader: SocketReader[A] = socketReader
  protected def writer: SocketWriter[A] = socketWriter
}
