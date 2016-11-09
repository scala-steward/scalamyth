package mythtv
package connection
package myth

import util.NetworkUtil

trait FileTransferConnection extends SocketConnection {
  def transferId: FileTransferId
  def fileSize: Long

  def isReadble: Boolean = true    // FIXME
  def isWriteable: Boolean = false // FIXME

  def read(buf: Array[Byte], off: Int, len: Int): Int
  def write(buf: Array[Byte], off: Int, len: Int): Unit
}

// TODO are we expected to send "DONE" command when finished with a FileTransfer socket? Python bindings don't seem to.
private abstract class AbstractFileTransferConnection(host: String, port: Int, timeout: Int, fileName: String, storageGroup: String)
  extends AbstractBackendConnection(host, port, timeout)
     with FileTransferConnection {

  self: MythProtocolAPI with AnnouncingConnection =>

  private[this] var ftId: FileTransferId = FileTransferId(0)
  private[this] var filesize: Long = 0L

  /*protected*/ def announce(): Unit = {
    val localHost = NetworkUtil.myHostName
    val (ftID, size) = announceFileTransfer(localHost, fileName, storageGroup)
    this.ftId = ftID
    this.filesize = size.bytes
  }

  override def sendCommand(command: String, args: Any*): Option[_] = {
    if (hasAnnounced) None
    else super.sendCommand(command, args: _*)
  }

  override def transferId: FileTransferId = ftId

  override def fileSize: Long = filesize

  override def read(buf: Array[Byte], off: Int, len: Int): Int =
    inputStream.read(buf, off, len)

  override def write(buf: Array[Byte], off: Int, len: Int): Unit =
    outputStream.write(buf, off, len)
}

private sealed trait FileTransferConnectionFactory {
  def apply(host: String, port: Int, timeout: Int, fileName: String, storageGroup: String): FileTransferConnection
}

object FileTransferConnection {
  private val supportedVersions = Map[Int, FileTransferConnectionFactory](
    75 -> FileTransferConnection75,
    77 -> FileTransferConnection77
  )

  def apply(
    host: String,
    fileName: String,
    storageGroup: String,
    port: Int = BackendConnection.DEFAULT_PORT,
    timeout: Int = BackendConnection.DEFAULT_TIMEOUT
  ): FileTransferConnection = {
    try {
      val factory = supportedVersions(BackendConnection.DEFAULT_VERSION)
      factory(host, port, timeout, fileName, storageGroup)
    } catch {
      case ex @ WrongMythProtocolException(requiredVersion) =>
        if (supportedVersions contains requiredVersion) {
          val factory = supportedVersions(requiredVersion)
          factory(host, port, timeout, fileName, storageGroup)
        }
        else throw new UnsupportedMythProtocolException(ex)
    }
  }
}

private class FileTransferConnection75(host: String, port: Int, timeout: Int, fileName: String, storageGroup: String)
  extends AbstractFileTransferConnection(host, port, timeout, fileName, storageGroup)
     with MythProtocol75
     with MythProtocolAPI
     with BackendAPILike
     with AnnouncingConnection

private object FileTransferConnection75 extends FileTransferConnectionFactory {
  def apply(host: String, port: Int, timeout: Int, fileName: String, storageGroup: String): FileTransferConnection =
    new FileTransferConnection75(host, port, timeout, fileName, storageGroup)
}

private class FileTransferConnection77(host: String, port: Int, timeout: Int, fileName: String, storageGroup: String)
  extends AbstractFileTransferConnection(host, port, timeout, fileName, storageGroup)
    with MythProtocol77
    with MythProtocolAPI
    with BackendAPILike
    with AnnouncingConnection

private object FileTransferConnection77 extends FileTransferConnectionFactory {
  def apply(host: String, port: Int, timeout: Int, fileName: String, storageGroup: String): FileTransferConnection =
    new FileTransferConnection77(host, port, timeout, fileName, storageGroup)
}
