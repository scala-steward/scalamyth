package mythtv
package connection
package myth

import java.nio.ByteBuffer

import util.NetworkUtil
import MythProtocol.MythProtocolFailure

trait FileTransferConnection extends FileTransfer with SocketConnection {
  def transferId: FileTransferId

  def isReadable: Boolean
  def isWritable: Boolean

  def read(buf: ByteBuffer): Int
  def write(buf: ByteBuffer): Int

  def read(buf: Array[Byte], off: Int, len: Int): Int
  def write(buf: Array[Byte], off: Int, len: Int): Unit
}

private abstract class AbstractFileTransferConnection(
  host: String,
  port: Int,
  timeout: Int,
  val fileName: String,
  val storageGroup: String,
  writeMode: Boolean,
  useReadAhead: Boolean
) extends AbstractBackendConnection(host, port, timeout)
     with FileTransferConnection {

  self: MythProtocolAPI with AnnouncingConnection =>

  private[this] var ftId: FileTransferId = FileTransferId(0)
  private[this] var filesize: Long = 0L

  def announce(): Unit = {
    val localHost = NetworkUtil.myHostName
    val (ftID, size) = announceFileTransfer(localHost, fileName, storageGroup, writeMode, useReadAhead).right.get
    this.ftId = ftID
    this.filesize = size.bytes

    // perform file transfers in blocking mode, at least for now
    underlyingChannel.configureBlocking(true)
  }

  override def sendCommand(command: String, args: Any*): MythProtocolResult[_] = {
    if (hasAnnounced) Left(MythProtocolFailure.MythProtocolFailureUnknown)
    else super.sendCommand(command, args: _*)
  }

  override def transferId: FileTransferId = ftId

  override def fileSize: Long = filesize

  override def isReadable: Boolean = !writeMode

  override def isWritable: Boolean = writeMode

  override def read(buf: ByteBuffer): Int = underlyingChannel.read(buf)
  override def write(buf: ByteBuffer): Int = underlyingChannel.write(buf)

  override def read(buf: Array[Byte], off: Int, len: Int): Int =
    read(ByteBuffer.wrap(buf, off, len))

  override def write(buf: Array[Byte], off: Int, len: Int): Unit =
    write(ByteBuffer.wrap(buf, off, len))
}

private sealed trait FileTransferConnectionFactory {
  def apply(host: String, port: Int, timeout: Int, fileName: String, storageGroup: String, writeMode: Boolean, useReadAhead: Boolean): FileTransferConnection
}

private class FileTransferConnection75(host: String, port: Int, timeout: Int, fileName: String, storageGroup: String, writeMode: Boolean, useReadAhead: Boolean)
  extends AbstractFileTransferConnection(host, port, timeout, fileName, storageGroup, writeMode, useReadAhead)
     with MythProtocol75
     with BackendAPIConnection
     with MythProtocolAPILike
     with AnnouncingConnection

private object FileTransferConnection75 extends FileTransferConnectionFactory {
  def apply(host: String, port: Int, timeout: Int, fileName: String, storageGroup: String, writeMode: Boolean, useReadAhead: Boolean): FileTransferConnection =
    new FileTransferConnection75(host, port, timeout, fileName, storageGroup, writeMode, useReadAhead)
}

private class FileTransferConnection77(host: String, port: Int, timeout: Int, fileName: String, storageGroup: String, writeMode: Boolean, useReadAhead: Boolean)
  extends AbstractFileTransferConnection(host, port, timeout, fileName, storageGroup, writeMode, useReadAhead)
     with MythProtocol77
     with BackendAPIConnection
     with MythProtocolAPILike
     with AnnouncingConnection

private object FileTransferConnection77 extends FileTransferConnectionFactory {
  def apply(host: String, port: Int, timeout: Int, fileName: String, storageGroup: String, writeMode: Boolean, useReadAhead: Boolean): FileTransferConnection =
    new FileTransferConnection77(host, port, timeout, fileName, storageGroup, writeMode, useReadAhead)
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
    writeMode: Boolean = false,
    useReadAhead: Boolean = false,
    port: Int = BackendConnection.DefaultPort,
    timeout: Int = BackendConnection.DefaultTimeout
  ): FileTransferConnection = {
    try {
      val factory = supportedVersions(BackendConnection.DefaultVersion)
      factory(host, port, timeout, fileName, storageGroup, writeMode, useReadAhead)
    } catch {
      case ex @ WrongMythProtocolException(requiredVersion) =>
        if (supportedVersions contains requiredVersion) {
          val factory = supportedVersions(requiredVersion)
          factory(host, port, timeout, fileName, storageGroup, writeMode, useReadAhead)
        }
        else throw new UnsupportedMythProtocolException(ex)
    }
  }
}
