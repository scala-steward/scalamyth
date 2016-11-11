package mythtv
package connection

import java.nio.{ ByteBuffer, CharBuffer }
import java.nio.charset.CharsetEncoder

package object myth {
  // Allows for the same encode(String) convenience method that Charset provides
  implicit class CharsetStringEncoder(val enc: CharsetEncoder) extends AnyVal {
    def encode(str: String): ByteBuffer = enc.encode(CharBuffer.wrap(str))
  }
}
