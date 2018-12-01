// SPDX-License-Identifier: LGPL-2.1-only
/*
 * SocketReader.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection

import java.io.Closeable
import java.nio.channels.SocketChannel
import java.nio.charset.{ CharsetDecoder, CodingErrorAction, StandardCharsets }

trait SocketReader[A] extends Closeable {
  def read(): A
}

abstract class AbstractSocketReader[A](channel: SocketChannel, conn: SocketConnection)
  extends SocketReader[A] {

  override def close(): Unit = ()

  // UTF-8 charset decoder
  protected val utf8: CharsetDecoder = (
    StandardCharsets.UTF_8.newDecoder
      onMalformedInput CodingErrorAction.REPLACE
      onUnmappableCharacter CodingErrorAction.REPLACE
    )
}
