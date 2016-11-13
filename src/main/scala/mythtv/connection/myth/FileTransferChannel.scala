package mythtv
package connection
package myth

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.{ NonReadableChannelException, NonWritableChannelException, SeekableByteChannel }

import EnumTypes.SeekWhence

class FileTransferChannel private[myth](controlChannel: MythFileTransferAPI, dataChannel: FileTransferConnection)
  extends FileTransfer with SeekableByteChannel {
  // A file transfer requires two (optionally three) channels:
  //   - control channel  (BackendConnection or BackendAPIConnection)
  //   - data channel     (FileTransferConnection)
  // and optionally
  //   - event channel    (EventConnection)

  @volatile protected[myth] var currentSize: Long = dataChannel.fileSize
  protected[myth] var currentPosition: Long = 0L

  private def clamp(value: Long, min: Long, max: Long): Long =
    if (value < min) min
    else if (value > max) max
    else value

  // close the file
  override def close(): Unit = {
    controlChannel.done()
    dataChannel.disconnect()  // TODO close the data channel (how? just shut down the socket? call a close() method?)
  }

  override def fileName: String = dataChannel.fileName

  override def storageGroup: String = dataChannel.storageGroup

  override def fileSize: Long = dataChannel.fileSize

  override def size: Long = currentSize

  override def isOpen: Boolean = true // TODO need an open status

  // current offset in file
  def tell: Long = currentPosition

  override def position: Long = currentPosition

  override def position(newPosition: Long): SeekableByteChannel = {
    seek(newPosition, SeekWhence.Begin)
    this
  }

  // seek to beginning
  def rewind(): Unit = seek(0, SeekWhence.Begin)

  // seek to offset (relative to whence)
  def seek(offset: Long, whence: SeekWhence): Unit = {
    val adjOffset = whence match {
      case SeekWhence.Begin   => clamp(offset, 0L, currentSize)
      case SeekWhence.Current => clamp(offset, -currentPosition, currentSize - currentPosition)
      case SeekWhence.End     => clamp(offset, -currentSize, 0L) + currentSize
    }
    val adjWhence = if (whence == SeekWhence.End) SeekWhence.Begin else whence
    val newPos: Long = controlChannel.seek(adjOffset, adjWhence, currentPosition)
    if (newPos < 0) throw new IOException("failed seek")
    currentPosition = newPos
  }

  // TODO: automatic management of request block size?

  // It seems that the myth backend won't send any blocks bigger than 128k no
  // matter what size we ask for. Is this a hard limit in the server code?
  //
  // Maybe it's a Java socket limit, as the Myth server seems to think I am
  // getting all the bytes?
  //
  // Actually, probably linux, combatting bufferbloat, see:
  //    cat /proc/sys/net/ipv4/tcp_limit_output_bytes

  override def read(bb: ByteBuffer): Int = {
    if (!dataChannel.isReadable) throw new NonReadableChannelException
    val length = clamp(bb.remaining, 0, math.min(currentSize - currentPosition, Int.MaxValue)).toInt
    if (length < bb.remaining) bb.limit(bb.position + length)

    var bytesRead: Int = 0
    var canReadMore: Boolean = true

    while (bb.hasRemaining && canReadMore) {
      val requestSize = length - bytesRead
      val allotedSize = controlChannel.requestBlock(requestSize)
      assert(requestSize == bb.remaining)

      if (allotedSize != requestSize) {}  // TODO do I want to take some action here?

      if (allotedSize < 0) {
        // TODO failure; re-seek to current position and retry (a maximum number of times?)
      } else {
        var bytesReadThisRequest: Int = 0

        while (bytesReadThisRequest < allotedSize && canReadMore) {
          val n = dataChannel.read(bb)
          if (n <= 0) canReadMore = false
          bytesReadThisRequest += n
        }
        bytesRead += bytesReadThisRequest
      }
    }
    currentPosition += bytesRead
    bytesRead
  }

  override def write(bb: ByteBuffer): Int = {
    if (!dataChannel.isWritable) throw new NonWritableChannelException
    val bytesWritten = dataChannel.write(bb)  // TODO may need to loop here...
    controlChannel.writeBlock(bytesWritten)  // TODO utilize result value? or is it just parroted back to us?
    currentPosition = math.max(currentPosition + bytesWritten, currentSize)
    bytesWritten
  }

  override def truncate(size: Long): SeekableByteChannel = {
    throw new IOException("truncate is not supported")
    this
  }
}

// TODO what happens if we specify a file that does not exist?
object FileTransferChannel {  // TODO this doesn't specify read/write mode
  def apply(host: String, fileName: String, storageGroup: String): FileTransferChannel = {
    // TODO how will control channel get closed since it's embeedded here and FT doesn't know that it owns it...
    val controlChannel = BackendAPIConnection(host)
    apply(controlChannel, fileName, storageGroup)
  }

  def apply(controlChannel: BackendAPIConnection, fileName: String, storageGroup: String): FileTransferChannel = {
    val dataChannel = FileTransferConnection(controlChannel.host, fileName, storageGroup, port = controlChannel.port)
    val fto = MythFileTransferObject(controlChannel, dataChannel)
    new FileTransferChannel(fto, dataChannel)
  }
}
