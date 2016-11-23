package mythtv
package connection

import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

import scala.util.DynamicVariable

trait SocketConnection extends NetworkConnection with AutoCloseable {
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

  def isConnected: Boolean = connected  // TODO also check channel.isConnected ?

  private def connectSocketChannel(): SocketChannel = {
    val newChannel = SocketChannel.open(new InetSocketAddress(host, port))
    newChannel.configureBlocking(false)
    connected = true
    newChannel
  }

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
      if (graceful) gracefulDisconnect()  // TODO enclose in try block?
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

  override def close(): Unit = disconnect()

  protected def openReader(channel: SocketChannel): SocketReader[A]

  protected def openWriter(channel: SocketChannel): SocketWriter[A]

  def timeout: Int = timeoutVar.value

  def withTimeout[T](seconds: Int)(thunk: => T): T = {
    timeoutVar.withValue(seconds) { thunk }
  }

  private[connection] def changeTimeout(seconds: Int) = timeoutVar.value = seconds

  protected def underlyingChannel: SocketChannel = channel

  protected def reader: SocketReader[A] = socketReader
  protected def writer: SocketWriter[A] = socketWriter
}
