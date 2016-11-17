package mythtv
package connection
package myth

import java.lang.{ StringBuilder => JStringBuilder }
import java.net.SocketTimeoutException
import java.nio.{ ByteBuffer, CharBuffer }
import java.nio.channels.{ SelectionKey, Selector, SocketChannel }
import java.nio.charset.StandardCharsets

import scala.util.Try

trait FrontendNetworkControl {
  /*
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

private class FrontendSocketReader(channel: SocketChannel, conn: SocketConnection)
  extends AbstractSocketReader[String](channel, conn) {
  final val PromptString = "\r\n# "  // expect this at the end of every response (interactive prompt)

  // For detecting the prompt string bytes at the end of a response
  private val promptBytes = PromptString.getBytes(StandardCharsets.UTF_8)
  private val promptCompare = new Array[Byte](promptBytes.length)

  // Buffers for reading from the socket and for character decoding
  private[this] val buffer = ByteBuffer.allocate(2048)
  private[this] val charBuffer = CharBuffer.allocate(2048)

  // Returns true iff the bytes immediately preceding the current
  // buffer position are the same as those in 'bytes' argument
  private def bufferEndsWith(bytes: Array[Byte]): Boolean = {
    buffer.position(buffer.position - promptCompare.length)
    buffer.get(promptCompare)
    promptCompare sameElements promptBytes
  }

  def read(): String = {
    var endOfInput: Boolean = false
    val sb = new JStringBuilder

    val selector = Selector.open
    val key = channel.register(selector, SelectionKey.OP_READ)

    utf8.reset()
    buffer.clear()

    do {
      val ready = selector.select(conn.timeout * 1000)
      if (ready == 0) throw new SocketTimeoutException(s"read timed out after ${conn.timeout} seconds")

      if (key.isReadable) {
        val n = channel.read(buffer)
        selector.selectedKeys.clear()
        endOfInput = bufferEndsWith(promptBytes)

        if (n >= 0) {
          // decode UTF-8 bytes to charBuffer
          utf8.decode({ buffer.flip(); buffer }, charBuffer, endOfInput)
          if (endOfInput) utf8.flush(charBuffer)

          // drain the charBuffer into string builder
          sb.append({ charBuffer.flip(); charBuffer })
          charBuffer.clear()

          // get ready for next round, preserve any un-decoded bytes
          if (buffer.hasRemaining) buffer.compact()
          else                     buffer.clear()
        }
      }
    } while (!endOfInput)

    selector.close()

    // Don't include the trailing prompt string in our result
    sb.substring(0, sb.length - PromptString.length)
  }
}

private class FrontendSocketWriter(channel: SocketChannel, conn: SocketConnection)
  extends AbstractSocketWriter[String](channel, conn) {

  def write(data: String): Unit = {
    println("Writing message " + data)
    val message = utf8 encode data

    while (message.hasRemaining) {
      val n = channel.write(message)
      if (n == 0) waitForWriteableSocket()
    }
  }
}

class FrontendConnection(host: String, port: Int, timeout: Int)
    extends AbstractSocketConnection[String](host, port, timeout)
    with FrontendNetworkControl {

  def this(host: String) = this(host, FrontendConnection.DefaultPort, FrontendConnection.DefaultTimeout)
  def this(host: String, port: Int) = this(host, port, FrontendConnection.DefaultTimeout)

  protected def finishConnect(): Unit = {
    // TODO swallow up connection start returned data
    println(reader.read())
  }

  protected def gracefulDisconnect(): Unit = postCommand("exit")

  protected def openReader(channel: SocketChannel): SocketReader[String] = new FrontendSocketReader(channel, this)
  protected def openWriter(channel: SocketChannel): SocketWriter[String] = new FrontendSocketWriter(channel, this)

  def postCommand(command: String): Unit = {
    val message = s"$command\n"
    writer.write(message)
    // TODO: swallow any response (asynchronously?!)
  }

  def sendCommand(command: String): Try[String] = {
    val message = s"$command\n"
    Try {
      writer.write(message)
      reader.read()
    }
  }
}

object FrontendConnection {
  final val DefaultPort: Int = 6546
  final val DefaultTimeout: Int = 10  // seconds
}
