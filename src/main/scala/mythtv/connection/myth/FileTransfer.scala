package mythtv
package connection
package myth

import java.io.{ IOException, InputStream }
import java.nio.channels.{ NonReadableChannelException, NonWritableChannelException, SeekableByteChannel }
import java.nio.ByteBuffer

import EnumTypes.SeekWhence
import model.{ ChanId, Recording }
import util.MythDateTime

final case class FileTransferId(id: Int) extends AnyVal

trait FileTransfer {
  def fileSize: Long
  def fileName: String
  def storageGroup: String
}

private class MythFileTransferObject(val ftId: FileTransferId, val protoApi: MythProtocolAPI) extends MythFileTransferAPILike

private object MythFileTransferObject {
  def apply(controlChannel: BackendAPIConnection, dataChannel: FileTransferConnection) =
    new MythFileTransferObject(dataChannel.transferId, controlChannel)
}

// TODO are we opening data channel with event mode set (or something??)  Actually, it's readahead...
//   Sending command ANN FileTransfer dove 0 1 2000[]:[]Music/Performances/Rolling_in_the_Deep.mp4[]:[]Videos

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

/* *******************************************************************************************************************/

class FileTransferInputStream private[myth](channel: FileTransferChannel) extends InputStream with FileTransfer {
  private[this] var markPosition: Long = -1L

  override def fileName: String = channel.fileName

  override def storageGroup: String = channel.storageGroup

  override def fileSize: Long = channel.fileSize

  // Horribly inefficient implementation, but nobody should be using it ...
  override def read(): Int = {
    val buf = ByteBuffer.allocate(1)
    val n = channel.read(buf)
    if (n > 1) channel.seek(n - 1, SeekWhence.Current)
    buf.flip()
    buf.get(0)
  }

  override def read(bytes: Array[Byte], offset: Int, len: Int): Int =
    channel.read(ByteBuffer.wrap(bytes, offset, len))

  override def skip(n: Long): Long = {
    val oldPosition = channel.position
    channel.seek(n, SeekWhence.Current)
    channel.position - oldPosition
  }

  override def markSupported: Boolean = true

  override def mark(readLimit: Int): Unit = synchronized { markPosition = channel.position }

  override def reset(): Unit = synchronized {
    if (markPosition < 0) throw new IOException("Resetting to invalid mark")
    channel.seek(markPosition, SeekWhence.Begin)
  }
}

/* *******************************************************************************************************************/

abstract class EventingFileTransferChannel(
  controlChannel: MythFileTransferAPI,
  dataChannel: FileTransferConnection,
  eventChannel: EventConnection
) extends FileTransferChannel(controlChannel, dataChannel) {

  eventChannel.addListener(listener)

  protected def listener: EventListener

  override def close(): Unit = {
    super.close()
    eventChannel.removeListener(listener)
    eventChannel.disconnect()  // TODO temporarary
  }
}

class RecordingFileTransferChannel private[myth](
  controlChannel: MythFileTransferAPI,
  dataChannel: FileTransferConnection,
  eventChannel: EventConnection,
  recording: Recording
) extends EventingFileTransferChannel(controlChannel, dataChannel, eventChannel) {

  override def listener: EventListener = updateListener

  // TODO block read if the recording is still in progress but we hit EOF? (wait for the event to arrive...)

  private[this] lazy val updateListener = new EventListener {
    override def listenFor(event: BackendEvent): Boolean = event.isEventName("UPDATE_FILE_SIZE")

    override def handle(event: BackendEvent): Unit = event.parse match {
      case Event.UpdateFileSizeEvent(chanId, startTime, newSize) =>
        if (chanId == recording.chanId && startTime == recording.startTime)
          currentSize = newSize
      case _ => ()
    }
  }
}

object RecordingFileTransferChannel {
  // TODO: method that takes a hostname vs control channel?
  def apply(api: BackendAPIConnection, chanId: ChanId, recStartTs: MythDateTime): RecordingFileTransferChannel = {
    val rec = api.queryRecording(chanId, recStartTs)   // TODO check for failure/not found

    // TODO who is managing these opened connections??  Also, we have no re-use...

    val controlChannel =
      if (rec.hostname == api.host) api
      else BackendAPIConnection(rec.hostname)

    val dataChannel = FileTransferConnection(controlChannel.host, rec.filename, rec.storageGroup, port = controlChannel.port)
    val eventChannel = EventConnection(controlChannel.host, controlChannel.port)

    val fto = MythFileTransferObject(controlChannel, dataChannel)
    new RecordingFileTransferChannel(fto, dataChannel, eventChannel, rec)
  }
}

// TODO what exactly is this class for?
// Reading/writing(?) from a file that is being downloaded to the server using DOWNLOAD_FILE ?
class DownloadFileTransferChannel private[myth](
  controlChannel: MythFileTransferAPI,
  dataChannel: FileTransferConnection,
  eventChannel: EventConnection
) extends EventingFileTransferChannel(controlChannel, dataChannel, eventChannel) {

  override protected def listener: EventListener = downloadListener

  private[this] lazy val downloadListener = new EventListener {
    override def listenFor(event: BackendEvent): Boolean = event.isEventName("DOWNLOAD_FILE")

    override def handle(event: BackendEvent): Unit = event.parse match {
      case Event.DownloadFileUpdateEvent(url, fileName, received, total) =>
        // TODO verify that the url/filename matches the file we're downloading
        currentSize = received  // TODO is this the update we want?
      case Event.DownloadFileFinished(url, fileName, fileSize, err, errCode) =>
        // TODO verify that the url/filename matches the file we're downloading
        currentSize = fileSize
        // TODO initiate finalization of this object/mark as completed?
      case _ => ()
    }
  }
}
