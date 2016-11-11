package mythtv
package connection
package myth

import java.lang.{ StringBuilder => JStringBuilder }
import java.nio.{ ByteBuffer, CharBuffer }
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets

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

private class FrontendSocketReader(channel: SocketChannel) extends SocketReader[String] {
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
    var n: Int = 0

    utf8.reset()
    buffer.clear()

    do {
      n = channel.read(buffer)
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
    } while (!endOfInput)

    // Don't include the trailing prompt string in our result
    sb.substring(0, sb.length - PromptString.length)
  }
}

private class FrontendSocketWriter(channel: SocketChannel) extends SocketWriter[String] {
  def write(data: String): Unit = {
    println("Writing message " + data)
    val message = utf8 encode data
    channel.write(message)
  }
}

class FrontendConnection(host: String, port: Int, timeout: Int)
    extends AbstractSocketConnection[String](host, port, timeout)
    with FrontendNetworkControl {

  def this(host: String, port: Int) = this(host, port, 10)

  protected def finishConnect(): Unit = {
    // TODO swallow up connection start returned data
    println(reader.read())
  }

  protected def gracefulDisconnect(): Unit = postCommand("exit")

  protected def openReader(channel: SocketChannel): SocketReader[String] = new FrontendSocketReader(channel)
  protected def openWriter(channel: SocketChannel): SocketWriter[String] = new FrontendSocketWriter(channel)

  // TODO should command end with \r\n instead of just \n ??

  def postCommand(command: String): Unit = {
    val message = s"$command\n"
    writer.write(message)
    // TODO: swallow any response (asynchronously?!)
  }

  // TODO: convert to Try[] instead of Option[]
  def sendCommand(command: String): Option[String] = {
    val message = s"$command\n"
    writer.write(message)
    Some(reader.read())
  }

}
