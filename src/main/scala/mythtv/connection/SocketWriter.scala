package mythtv
package connection

import java.nio.charset.{ CharsetEncoder, CodingErrorAction, StandardCharsets }

trait SocketWriter[A] {
  def write(data: A): Unit

  // UTF-8 charset encoder
  protected val utf8: CharsetEncoder = (
    StandardCharsets.UTF_8.newEncoder
      onMalformedInput CodingErrorAction.REPLACE
      onUnmappableCharacter CodingErrorAction.REPLACE
    )
}
