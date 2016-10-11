package mythtv
package connection
package myth

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

import java.io.{ InputStreamReader, OutputStreamWriter, InputStream, OutputStream }
import java.nio.charset.StandardCharsets

import scala.util.Try

private class FrontendSocketReader(in: InputStream) extends SocketReader[String](in) {
  val underlying = new InputStreamReader(in)

  // we should expect "\n# " at the end of all replies (this is the interactive prompt)
  def read(): String = {
    val sb = new StringBuffer

    var n: Int = 0
    var off: Int = 0
    val BUFSZ = 1024

    val buf = new Array[Char](BUFSZ)

    println("Starting read...")
    do {
      n = underlying.read(buf, off, BUFSZ - off)
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
}

private class FrontendSocketWriter(out: OutputStream) extends SocketWriter[String](out) {
  val underlying = new OutputStreamWriter(out, StandardCharsets.UTF_8)

  def write(data: String): Unit = {
    println("Writing message " + data)
    underlying.write(data)
    underlying.flush()
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

  protected def openReader(inStream: InputStream): SocketReader[String] = new FrontendSocketReader(inStream)
  protected def openWriter(outStream: OutputStream): SocketWriter[String] = new FrontendSocketWriter(outStream)

  def postCommand(command: String): Unit = {
    val message = s"$command\n"
    writer.write(message)
    // TODO: swallow any response (asynchronously?!)
  }

  // TODO: convert to Try[] instead of Option[]
  // TODO: need to exclude the trailing newline and prompt from the result!
  def sendCommand(command: String): Option[String] = {
    val message = s"$command\n"
    writer.write(message)
    Some(reader.read())
  }

}
