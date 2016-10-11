package mythtv
package connection

import java.net.Socket
import java.io.OutputStream

abstract class SocketWriter[A](outStream: OutputStream) {
  def this(sock: Socket) = this(sock.getOutputStream)
  def write(data: A): Unit
}
