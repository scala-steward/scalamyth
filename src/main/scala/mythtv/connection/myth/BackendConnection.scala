package mythtv
package connection
package myth

import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.channels.{ SelectionKey, Selector, SocketChannel }

import scala.util.Try

private trait BackendCommandStream {
  final val HeaderSizeBytes = 8
}

private class BackendCommandReader(channel: SocketChannel, conn: SocketConnection)
  extends AbstractSocketReader[String](channel, conn)
     with BackendCommandStream {
  private[this] var buffer = ByteBuffer.allocate(1024)  // will reallocate as needed

  private def readStringWithLength(length: Int): String = {
    if (length > buffer.capacity) buffer = ByteBuffer.allocate(length)
    buffer.clear().limit(length)

    val selector = Selector.open
    val key = channel.register(selector, SelectionKey.OP_READ)

    var n: Int = 0
    do {
      val ready = selector.select(conn.timeout * 1000)
      if (ready == 0) throw new SocketTimeoutException(s"read timed out after ${conn.timeout} seconds")

      if (key.isReadable) {
        n = channel.read(buffer)
        selector.selectedKeys.clear()
        println("Read " + n + " bytes")
      }
    } while (n > 0 && buffer.hasRemaining)

    selector.close()

    if (n < 0) throw new RuntimeException("connection has been closed")  // TODO is this still valid in nonblocking mode?
    utf8.decode({ buffer.flip(); buffer }).toString
  }

  def read(): String = {
    //println("Waiting for size header")
    val size = readStringWithLength(HeaderSizeBytes).trim.toInt
    println("Waiting for response of length " + size)
    val response = readStringWithLength(size)
    //println("Received response: " + response)
    println("Received response.")
    response
  }
}

private class BackendCommandWriter(channel: SocketChannel, conn: SocketConnection)
  extends AbstractSocketWriter[String](channel, conn)
     with BackendCommandStream {
  private final val HeaderFormat= "%-" + HeaderSizeBytes + "d"

  private[this] val buffers = new Array[ByteBuffer](2)

  def write(command: String): Unit = {
    val message = utf8 encode command
    val header = utf8 encode (HeaderFormat format message.limit)

    buffers(0) = header
    buffers(1) = message
    println("Sending command " + command)

    while (message.hasRemaining) {
      val n = channel.write(buffers)
      if (n == 0) waitForWriteableSocket()
    }
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

/*private*/ abstract class AbstractBackendConnection(host: String, port: Int, timeout: Int)
    extends AbstractSocketConnection[String](host, port, timeout)
    with BackendConnection {

  protected def finishConnect(): Unit = {
    checkVersion()
  }

  protected def gracefulDisconnect(): Unit = postCommandRaw("DONE")

  protected def openReader(channel: SocketChannel): SocketReader[String] =
    new BackendCommandReader(channel, this)

  protected def openWriter(channel: SocketChannel): SocketWriter[String] =
    new BackendCommandWriter(channel, this)

  /*protected*/ def sendCommandRaw(command: String): Try[BackendResponse] = {
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
    else throw UnsupportedBackendCommandException(command, ProtocolVersion)
  }

  def checkVersion(): Boolean = checkVersion(ProtocolVersion, ProtocolToken)

  def checkVersion(version: Int, token: String): Boolean = {
    val msg = for {
      response <- sendCommandRaw(s"MYTH_PROTO_VERSION $version $token")
      split <- Try(response.split)
    } yield (split(0), split(1))

    val (status, requiredVersion) = msg.get
    if (status == "ACCEPT") true
    else {
      disconnect(false)
      throw WrongMythProtocolException(requiredVersion.toInt)
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


private sealed trait BackendConnectionFactory {
  def apply(host: String, port: Int, timeout: Int): BackendConnection
}

object BackendConnection {
  final val DefaultPort = 6543
  final val DefaultTimeout = 10

  private val supportedVersions = Map[Int, BackendConnectionFactory](
    75 -> BackendConnection75,
    77 -> BackendConnection77
  )

  private[myth] val DefaultVersion = 75  // TODO just for now for testing

  def apply(host: String, port: Int = DefaultPort, timeout: Int = DefaultTimeout): BackendConnection = {
    try {
      val factory = supportedVersions(DefaultVersion)
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
