package mythtv
package connection

import java.net.Socket
import java.io.InputStream

abstract class SocketReader[A](inputStream: InputStream) {
  def this(sock: Socket) = this(sock.getInputStream)
  def read(): A
}
