package mythtv
package connection
package myth

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.{ NonReadableChannelException, NonWritableChannelException, SeekableByteChannel }

import EnumTypes.SeekWhence

trait FileTransferChannel extends SeekableByteChannel with Seekable with FileTransfer

private[myth] class FileTransferChannelImpl(controlChannel: FileTransferAPI, dataChannel: FileTransferConnection)
  extends FileTransferChannel {
  // A file transfer requires two (optionally three) channels:
  //   - control channel  (BackendConnection or BackendAPIConnection)
  //   - data channel     (FileTransferConnection)
  // and optionally
  //   - event channel    (EventConnection)

  @volatile protected var currentSize: Long = dataChannel.fileSize
  protected var currentPosition: Long = 0L
  private[this] var openStatus: Boolean = true

  private def clamp(value: Long, min: Long, max: Long): Long =
    if (value < min) min
    else if (value > max) max
    else value

  // close the file
  override def close(): Unit = {
    controlChannel.done()
    dataChannel.close()
    openStatus = false
  }

  override def fileName: String = dataChannel.fileName

  override def storageGroup: String = dataChannel.storageGroup

  override def fileSize: Long = dataChannel.fileSize

  override def size: Long = currentSize

  override def isOpen: Boolean = openStatus

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
    val newPos: Long = controlChannel.seek(adjOffset, adjWhence, currentPosition).right.get
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

  protected def waitForMoreData(oldSize: Long): Boolean = false

  private def readableLength(len: Int, size: Long): Int =
    clamp(len, 0, math.min(size - currentPosition, Int.MaxValue)).toInt

  private def waitableReadableLength(len: Int): Int = {
    val origSize = currentSize
    val origLength = readableLength(len, origSize)
    if (origLength == 0 && waitForMoreData(origSize)) readableLength(len, currentSize)
    else origLength
  }

  override def read(bb: ByteBuffer): Int = {
    if (!dataChannel.isReadable) throw new NonReadableChannelException
    val length = waitableReadableLength(bb.remaining)
    if (length < bb.remaining) bb.limit(bb.position + length)

    var bytesRead: Int = 0
    var canReadMore: Boolean = true

    while (bb.hasRemaining && canReadMore) {
      val requestSize = length - bytesRead
      val allotedSize = controlChannel.requestBlock(requestSize).right.get
      assert(requestSize == bb.remaining)

      if (allotedSize != requestSize) {}  // TODO do I want to take some action here?

      if (allotedSize < 0) {
        // TODO failure; re-seek to current position and retry (a maximum number of times?)
      } else if (allotedSize == 0) {
        canReadMore = false   // TODO is this the right thing to do here?
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
    // TODO is there a limit on how much data I can write here at once?
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

/* TODO what happens if we specify a file that does not exist?  This:
   Sending command ANN FileTransfer dove 0 0 2000[]:[]ittybitty.jpg[]:[]coverart
   java.util.NoSuchElementException: Either.right.get on Left
    at scala.util.Either$RightProjection.get(Either.scala:653)
    at mythtv.connection.myth.AbstractFileTransferConnection.announce(FileTransferConnection.scala:42)
 */

object FileTransferChannel {
  def apply(
    controlChannel: MythProtocolAPIConnection,
    fileName: String,
    storageGroup: String,
    writeMode: Boolean = false,
    useReadAhead: Boolean = false
  ): FileTransferChannel = {
    val dataChannel = FileTransferConnection(
      controlChannel.host,
      fileName,
      storageGroup,
      writeMode,
      useReadAhead,
      controlChannel.port
    )
    val fto = MythFileTransferObject(controlChannel, dataChannel)
    new FileTransferChannelImpl(fto, dataChannel)
  }
}
