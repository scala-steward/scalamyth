package mythtv
package connection
package myth

import java.io.{ BufferedOutputStream, InputStream, OutputStream }
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

// TODO consider using scala.io.Codec rather than Java charset stuff directly?

import scala.util.Try

private trait BackendCommandStream {
  final val SIZE_HEADER_BYTES = 8
}

private class BackendCommandReader(in: InputStream) extends SocketReader[String](in) with BackendCommandStream {
  private[this] var buffer = new Array[Byte](1024)

  private def readStringWithLength(length: Int): String = {
    if (length > buffer.length) buffer = new Array[Byte](length)
    var off: Int = 0
    var n: Int = 0

    do {
      n = in.read(buffer, off, length - off)
      //println("Read " + n + " bytes")
      off += n
    } while (n > 0)

    if (n < 0) throw new RuntimeException("connection has been closed")

    assert(off == length)
    new String(buffer, 0, length, StandardCharsets.UTF_8)
  }

  def read(): String = {
    //println("Waiting for size header")
    val size = readStringWithLength(SIZE_HEADER_BYTES).trim.toInt
    println("Waiting for response of length " + size)
    val response = readStringWithLength(size)
    //println("Received response: " + response)
    println("Received response.")
    response
  }
}

private class BackendCommandWriter(out: OutputStream) extends SocketWriter[String](out) with BackendCommandStream {
  private final val HEADER_FORMAT = "%-" + SIZE_HEADER_BYTES + "d"
  private final val utf8 = StandardCharsets.UTF_8

  private def write(bb: ByteBuffer): Unit = {
    if (bb.hasArray) {
      out.write(bb.array, bb.arrayOffset, bb.limit)
    } else {
      val buf = new Array[Byte](bb.remaining)
      bb.get(buf)
      out.write(buf)
    }
  }

  def write(command: String): Unit = {
    val message = utf8 encode command
    val header = utf8 encode (HEADER_FORMAT format message.limit)
    println("Sending command " + command)
    write(header)
    write(message)
    out.flush()
  }
}

final case class WrongMythProtocolException(requiredVersion: Int)
    extends RuntimeException("wrong Myth protocol version; need version " + requiredVersion)

final case class UnsupportedMythProtocolException(requiredVersion: Int)
    extends RuntimeException(s"unsupported Myth protocol version $requiredVersion requested") {
  def this(ex: WrongMythProtocolException) = this(ex.requiredVersion)
}

final case class UnsupportedBackendCommandException(command: String, protocolVersion: Int)
    extends UnsupportedOperationException(
  s"unsupported backend command $command in protocol version $protocolVersion")

trait BackendConnection extends SocketConnection with MythProtocol

private abstract class AbstractBackendConnection(host: String, port: Int, timeout: Int)
    extends AbstractSocketConnection[String](host, port, timeout)
    with BackendConnection {

  protected def finishConnect(): Unit = {
    checkVersion()
  }

  protected def gracefulDisconnect(): Unit = postCommandRaw("DONE")

  protected def openReader(inStream: InputStream): SocketReader[String] =
    new BackendCommandReader(inStream)

  protected def openWriter(outStream: OutputStream): SocketWriter[String] =
    new BackendCommandWriter(new BufferedOutputStream(outStream))

  protected def sendCommandRaw(command: String): Try[BackendResponse] = {
    Try {
      writer.write(command)
      Response(reader.read())
    }
  }

  protected def postCommandRaw(command: String): Unit = {
    // write the message
    writer.write(command)
    // TODO: swallow any response (asynchronously?!?), but socket may be closed
  }

  def sendCommand(command: String, args: Any*): Option[_] = {
    if (!isConnected) throw new IllegalStateException  // TODO attempt reconnection?
    if (commands contains command) {
      val (serialize, handle) = commands(command)
      val cmdstring = serialize(command, args)
      val request = BackendRequest(command, args, cmdstring)
      val response = sendCommandRaw(cmdstring).get
      handle(request, response)
    }
    else throw new UnsupportedBackendCommandException(command, PROTO_VERSION)
  }

  def checkVersion(): Boolean = checkVersion(PROTO_VERSION, PROTO_TOKEN)

  def checkVersion(version: Int, token: String): Boolean = {
    val msg = for {
      response <- sendCommandRaw(s"MYTH_PROTO_VERSION $version $token")
      split <- Try(response.split)
    } yield (split(0), split(1))

    val (status, requiredVersion) = msg.get
    if (status == "ACCEPT") true
    else {
      disconnect(false)
      throw new WrongMythProtocolException(requiredVersion.toInt)
    }
  }
}

private sealed trait BackendConnectionFactory {
  def apply(host: String, port: Int, timeout: Int): BackendConnection
}

object BackendConnection {
  final val DEFAULT_PORT = 6543
  final val DEFAULT_TIMEOUT = 10

  private val supportedVersions = Map[Int, BackendConnectionFactory](
    75 -> BackendConnection75,
    77 -> BackendConnection77
  )

  private[myth] val DEFAULT_VERSION = 75  // TODO just for now for testing

  def apply(host: String, port: Int = DEFAULT_PORT, timeout: Int = DEFAULT_TIMEOUT): BackendConnection = {
    try {
      val factory = supportedVersions(DEFAULT_VERSION)
      factory(host, port, timeout)
    } catch {
      case ex @ WrongMythProtocolException(requiredVersion) =>
        if (supportedVersions contains requiredVersion) {
          val factory = supportedVersions(requiredVersion)
          factory(host, port, timeout)
        }
        else throw new UnsupportedMythProtocolException(ex)
    }
  }
}


private class BackendConnection75(host: String, port: Int, timeout: Int)
    extends AbstractBackendConnection(host, port, timeout) with MythProtocol75

private object BackendConnection75 extends BackendConnectionFactory {
  def apply(host: String, port: Int, timeout: Int) = new BackendConnection75(host, port, timeout)
}

private class BackendConnection77(host: String, port: Int, timeout: Int)
    extends AbstractBackendConnection(host, port, timeout) with MythProtocol77

private object BackendConnection77 extends BackendConnectionFactory {
  def apply(host: String, port: Int, timeout: Int) = new BackendConnection77(host, port, timeout)
}
