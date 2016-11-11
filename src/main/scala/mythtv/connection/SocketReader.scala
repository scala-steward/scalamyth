package mythtv
package connection

import java.nio.channels.SocketChannel
import java.nio.charset.{ CharsetDecoder, CodingErrorAction, StandardCharsets }

trait SocketReader[A] {
  def read(): A
}

abstract class AbstractSocketReader[A](channel: SocketChannel, conn: SocketConnection)
  extends SocketReader[A] {

  // UTF-8 charset decoder
  protected val utf8: CharsetDecoder = (
    StandardCharsets.UTF_8.newDecoder
      onMalformedInput CodingErrorAction.REPLACE
      onUnmappableCharacter CodingErrorAction.REPLACE
    )
}