// SPDX-License-Identifier: LGPL-2.1-only
/*
 * SocketConnection.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection

import java.io.Closeable
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

import scala.util.{ DynamicVariable, Try }

trait SocketConnection extends NetworkConnection with Closeable {
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

  finishConnect()

  def isConnected: Boolean = connected && channel.isConnected

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
      Try(if (graceful) gracefulDisconnect())
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

  override def close(): Unit = {
    writer.close()
    reader.close()
    disconnect()
  }

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
