package mythtv
package connection
package myth

import java.io.{ InputStream, InputStreamReader, OutputStream, OutputStreamWriter }
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

// TODO consider using scala.io.Codec rather than Java charset stuff directly?

import scala.util.Try

private trait BackendCommandStream {
  final val SIZE_HEADER_BYTES = 8
}

sealed trait BackendResponse extends Any {
  def raw: String
  def split: Array[String]
}

object BackendResponse {
  def apply(r: String): BackendResponse = Response(r)
}

private final case class Response(raw: String) extends AnyVal with BackendResponse {
  def split: Array[String] = raw split MythProtocol.SPLIT_PATTERN
}

private class BackendCommandReader(in: InputStream) extends BackendCommandStream {
  def readString(length: Int): String = {
    // TODO for efficiency re-use an existing buffer?
    val buf = new Array[Byte](length)
    var off: Int = 0
    var n: Int = 0

    do {
      n = in.read(buf, off, length - off)
      //println("Read " + n + " bytes")
      off += n
    } while (n > 0)

    assert(off == length)
    new String(buf, StandardCharsets.UTF_8)  // TODO need to replace call if we switch to a shared buffer
  }

  def readResponse: String = {
    println("Waiting for size header")
    val size = readString(SIZE_HEADER_BYTES).trim.toInt
    println("Waiting for response of length " + size)
    val response = readString(size)
    //println("Received response: " + response)
    println("Received response.")
    response
  }
}

private class BackendCommandWriter(out: OutputStream) extends BackendCommandStream {
  private final val HEADER_FORMAT = "%-" + SIZE_HEADER_BYTES + "d"
  private final val utf8 = StandardCharsets.UTF_8

  def write(bb: ByteBuffer): Unit = {
    if (bb.hasArray) {
      out.write(bb.array, bb.arrayOffset, bb.limit)
    } else {
      val buf = new Array[Byte](bb.remaining)
      bb.get(buf)
      out.write(buf)
    }
  }

  def sendCommand(command: String): Unit = {
    val message = utf8 encode command
    val header = utf8 encode (HEADER_FORMAT format message.limit)
    println("Sending command " + command)
    write(header)
    write(message)
    out.flush()
  }
}

class BackendConnection(host: String, port: Int, timeout: Int, val blockShutdown: Boolean)
    extends SocketConnection(host, port, timeout) with MythProtocol {
  // TODO management of reader/writer lifecycle

  def this(host: String, port: Int, timeout: Int) = this(host, port, timeout, false)
  def this(host: String, port: Int) = this(host, port, 10)

  protected def finishConnect(): Unit = {
    checkVersion
    announce()
  }

  protected def gracefulDisconnect(): Unit = postCommand("DONE")

  protected def announce(): Unit = {
    val localname = InetAddress.getLocalHost().getHostName()  // FIXME uses DNS, ugh..
    val announceType = if (blockShutdown) "Playback" else "Monitor"
    val response = sendCommand(s"ANN ${announceType} ${localname} 0")
    // TODO get hostname from backend using QUERY_HOSTNAME command
    response.toOption == Some("OK")  // TODO eliminate conversion to Option
  }

  // TODO convert below to use Try[] instead of Option[]
  // TODO support passing a list which will be joined using backend sep
  ///  NB some commands put a separator between command string and arguments, others use whitespace
  def sendCommand(command: String, timeout: Option[Int] = None): Try[BackendResponse] = {
    val writer = new BackendCommandWriter(outputStream)
    val reader = new BackendCommandReader(inputStream)

    Try {
      // write the message
      writer.sendCommand(command)

      // wait for and retrieve the response
      val response = reader.readResponse
      Response(response)
      // TODO split response based on split pattern?
    }
  }

  def postCommand(command: String): Unit = {
    val writer = new BackendCommandWriter(outputStream)
    val reader = new BackendCommandReader(inputStream)

    // write the message
    writer.sendCommand(command)

    // TODO: swallow any response, but socket may be closed
  }

  def checkVersion: Boolean = {
    val splitPat = MythProtocol.SPLIT_PATTERN
    val msg = for {
      response <- sendCommand(s"MYTH_PROTO_VERSION ${PROTO_VERSION} ${PROTO_TOKEN}")
      word <- Try(response.split.head)
    } yield word
    msg.get == "ACCEPT"
  }
}
