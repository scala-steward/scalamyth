package mythtv
package connection

trait FrontendNetworkControl {
  /**
    * Commands:
    *
    * help
    * jump JUMPPOINT
    * key { LETTER | NUMBER | CODE }
    * play  < lots of subcommands >
    * query < lots of subcommands >
    * set { verbose MASK }
    * screenshot [WxH]
    * message
    * notification
    * exit
    */
}

import java.net.Socket
import java.io.{ InputStream, InputStreamReader, OutputStream, OutputStreamWriter }
import java.nio.charset.Charset

import scala.util.Try

class FrontendConnection(val host: String, val port: Int, val timeout: Int) {
  private[this] var connected: Boolean = false
  private[this] var socket: Socket = connectSocket()

  // TODO management of reader/writer lifecycle

  finishConnect()

  def this(host: String, port: Int) = this(host, port, 10)

  private def connectSocket(): Socket = {
    val newSocket = new Socket(host, port)
    newSocket.setSoTimeout(10 * 1000)
    connected = true
    newSocket
  }

  protected def finishConnect(): Unit = {
    // TODO swallow up connection start returned data
    println(receive())
  }

  protected def connect(): Unit = {
    if (!connected) {
      socket = connectSocket()
      finishConnect()
    }
  }

  def disconnect(graceful: Boolean = true): Unit = {
    if (connected) {
      if (graceful) postCommand("exit")
      connected = false
      socket.shutdownOutput()
      socket.close()
    }
  }

  protected def transmit(message: String): Unit = {
    val writer = new OutputStreamWriter(socket.getOutputStream, Charset.forName("UTF-8"))
    println("Writing message " + message)
    writer.write(message)
    writer.flush()
  }

  // TODO we should expect "\n# " at the end of all replies (this is the interactive prompt)
  protected def receive(): String = {
    val sb = new StringBuffer

    var n: Int = 0
    var off: Int = 0
    val BUFSZ = 1024

    val reader = new InputStreamReader(socket.getInputStream)
    val buf = new Array[Char](BUFSZ)

    println("Starting read...")
    do {
      n = reader.read(buf, off, BUFSZ - off)
      println(" .. read " + n + " bytes")
      off += n
      if (off >= BUFSZ) {
        sb.append(buf, 0, BUFSZ)
        off = 0
      }
    } while (n == BUFSZ)

    if (off > 0) sb.append(buf, 0, off)

    val prompt = "\r\n# "
    val pn = prompt.length
    val sn = sb.length
    val end = if (sn >= pn && sb.substring(sn - pn, sn) == prompt) sn - pn else sn
    sb.substring(0, end)
  }

  def postCommand(command: String): Unit = {
    val message = s"$command\n"
    transmit(message)
    // TODO: swallow any response (asynchronously?!)
  }

  // TODO: convert to Try[] instead of Option[]
  // TODO: need to exclude the trailing newline and prompt from the result!
  def sendCommand(command: String): Option[String] = {
    val message = s"$command\n"
    transmit(message)
    Some(receive())
  }

}
