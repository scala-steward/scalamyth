// SPDX-License-Identifier: LGPL-2.1-only
/*
 * SocketWriter.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection

import java.io.Closeable
import java.net.SocketTimeoutException
import java.nio.channels.{ SelectionKey, Selector, SocketChannel }
import java.nio.charset.{ CharsetEncoder, CodingErrorAction, StandardCharsets }

trait SocketWriter[A] extends Closeable {
  def write(data: A): Unit
}

abstract class AbstractSocketWriter[A](channel: SocketChannel, conn: SocketConnection)
  extends SocketWriter[A] {

  override def close(): Unit = ()

  // UTF-8 charset encoder
  protected val utf8: CharsetEncoder = (
    StandardCharsets.UTF_8.newEncoder
      onMalformedInput CodingErrorAction.REPLACE
      onUnmappableCharacter CodingErrorAction.REPLACE
    )

  protected def waitForWriteableSocket(): Unit = {
    println("*** NOTICE socket write failed; now calling select() with OP_WRITE")
    val selector = Selector.open
    val key = channel.register(selector, SelectionKey.OP_WRITE)

    val ready = selector.select(conn.timeout * 1000)
    if (ready == 0) throw new SocketTimeoutException(s"write timed out after ${conn.timeout} seconds")

    selector.selectedKeys.clear()

    key.cancel()      // TODO don't deregister until write has succeeded
    selector.close()  // TODO don't open and close selector in loop
  }
}
