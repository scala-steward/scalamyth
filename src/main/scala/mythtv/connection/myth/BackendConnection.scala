package mythtv
package connection
package myth

import java.io.{ InputStream, InputStreamReader, OutputStream, OutputStreamWriter }
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

// TODO consider using scala.io.Codec rather than Java charset stuff directly?

import scala.util.Try

private trait BackendCommandStream {
  final val SIZE_HEADER_BYTES = 8
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

    if (n < 0) throw new RuntimeException("connection has been closed")

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

final case class WrongMythProtocolException(requiredVersion: Int)
    extends RuntimeException("wrong Myth protocol version; need version " + requiredVersion)

final case class UnsupportedMythProtocolException(requiredVersion: Int)
    extends RuntimeException(s"unsupported Myth protocol version $requiredVersion requested") {
  def this(ex: WrongMythProtocolException) = this(ex.requiredVersion)
}

trait BackendConnection extends SocketConnection with MythProtocol {
  // TODO send and post command really belong in MythProtocol w/instead of execute()...
  def sendCommand(command: String, timeout: Option[Int] = None): Try[BackendResponse]
  def postCommand(command: String): Unit
}

abstract class AbstractBackendConnection(host: String, port: Int, timeout: Int)
    extends AbstractSocketConnection(host, port, timeout) with BackendConnection {
  // TODO management of reader/writer lifecycle

  protected def finishConnect(): Unit = {
    checkVersion()
    announce()
  }

  protected def gracefulDisconnect(): Unit = postCommand("DONE")

  protected def announce(): Unit = {
    val localname = InetAddress.getLocalHost().getHostName()  // FIXME uses DNS, ugh..
    val announceType = "Monitor" /*if (blockShutdown) "Playback" else "Monitor"*/
    val response = sendCommand(s"ANN ${announceType} ${localname} 0")
    // TODO get hostname from backend using QUERY_HOSTNAME command
    response.toOption == Some("OK")  // TODO eliminate conversion to Option
  }

  // TODO support passing a list which will be joined using backend sep
  ///  NB some commands put a separator between command string and arguments, others use whitespace
  // TODO move timeout into a DynamicVariable and use withTimeout() rather than method parameter
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

  def execute(command: String, args: Any*): Option[_] = {
    if (!isConnected) throw new IllegalStateException
    if (commands contains command) {
      val (check, serialize, handle) = commands(command)
      if (check(args)) {
        val cmdstring = serialize(command, args)
        val response = sendCommand(cmdstring).get
        handle(response)
      }
      else {
        println("failed argument type check")  // TODO real error handling
        None
      }
    } else {
      println(s"invalid command $command")     // TODO real error handling
      None
    }
  }

  def checkVersion(): Boolean = checkVersion(PROTO_VERSION, PROTO_TOKEN)

  def checkVersion(version: Int, token: String): Boolean = {
    val msg = for {
      response <- sendCommand(s"MYTH_PROTO_VERSION $version $token")
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

  private[myth] val DEFAULT_VERSION = 75

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
