package mythtv
package connection
package myth

import java.io.{ InputStream, IOException }
import java.nio.channels.SeekableByteChannel
import java.nio.ByteBuffer

import EnumTypes.SeekWhence

import model.{ ChanId, Recording }
import util.MythDateTime

final case class FileTransferId(id: Int) extends AnyVal

class MythFileTransferObject(ftID: FileTransferId, conn: MythProtocolAPI) extends MythFileTransferAPILike {
  def ftId = ftID
  protected def protoApi = conn
}

object MythFileTransferObject {
  def apply(controlChannel: BackendAPIConnection, dataChannel: FileTransferConnection) =
    new MythFileTransferObject(dataChannel.transferId, controlChannel)
}

// TODO are we opening data channel with event mode set (or something??)  Actually, it's readahead...
//   Sending command ANN FileTransfer dove 0 1 2000[]:[]Music/Performances/Rolling_in_the_Deep.mp4[]:[]Videos

// TODO support full Java InputStream/OutputStream interfaces? + NIO: Readable/Seekable/Writeable ByteChannel
// TODO arggggh InputStream is an abstract class, not an interface! All the NIO stuff is interfaces...

class FileTransferChannel private[myth](controlChannel: MythFileTransferAPI, dataChannel: FileTransferConnection)
  extends SeekableByteChannel {
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
    // TODO close the data channel (how? just shut down the socket?)
  }

  def fileName: String = dataChannel.fileName

  def storageGroup: String = dataChannel.storageGroup

  // current offset in file
  def tell: Long = currentPosition

  override def size: Long = currentSize

  override def isOpen: Boolean = true // TODO

  private[myth] def available: Int = dataChannel.inputBytesAvailable

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

  override def read(bb: ByteBuffer): Int = {
    if (bb.hasArray) {
      read(bb.array, bb.arrayOffset + bb.position, bb.remaining)
    } else {
      // TODO ugh, we really need to base on ByteBuffer/Channel and adapt to byte[] instead...
      val buf = new Array[Byte](bb.remaining)
      val n = read(buf, 0, buf.length)
      bb.put(buf)
      n
    }
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

  def read(buf: Array[Byte], off: Int, len: Int): Int = {
    if (!dataChannel.isReadable) throw new IOException("data channel is not readable")
    val length = clamp(len, 0, math.min(currentSize - currentPosition, Int.MaxValue)).toInt

    var bytesRead: Int = 0
    var canReadMore: Boolean = true

    while (bytesRead < length && canReadMore) {
      val requestSize = length - bytesRead
      val allotedSize = controlChannel.requestBlock(requestSize)

      if (allotedSize != requestSize) {}  // TODO do I want to take some action here?

      if (allotedSize < 0) {
        // TODO failure; re-seek to current position and retry (a maximum number of times?)
      } else {
        var bytesReadThisRequest: Int = 0

        while (bytesReadThisRequest < allotedSize && canReadMore) {
          val toRead = allotedSize - bytesReadThisRequest
          val n = dataChannel.read(buf, off, toRead)
          if (n <= 0) canReadMore = false
          //println(s"Read $n bytes from data channel (desired $toRead)")
          bytesReadThisRequest += n
        }
        bytesRead += bytesReadThisRequest
      }
    }
    currentPosition += bytesRead
    bytesRead
  }

  /*
  def write(buf: Array[Byte], off: Int, len: Int): Unit = {
    if (!dataChannel.isWriteable) throw new IOException("data channel is not writeable")
    if (len > 0) {
      dataChannel.write(buf, off, len)
      controlChannel.writeBlock(len)  // TODO utilize result value? or is it just parroted back to us?
      position = math.max(position + len, size)
    }
  }
  */

  override def write(bb: ByteBuffer): Int = ???

  override def truncate(size: Long): SeekableByteChannel = {
    throw new IOException("truncate is not currently supported")
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

class FileTransferInputStream private[myth](channel: FileTransferChannel) extends InputStream {
  private[this] var markPosition: Long = -1L

  // Horribly inefficient implementation, but nobody should be using it ...
  override def read(): Int = {
    val buf = new Array[Byte](1)
    val n = channel.read(buf, 0, 1)
    if (n > 1) channel.seek(n - 1, SeekWhence.Current)
    buf(0)
  }

  override def read(bytes: Array[Byte], offset: Int, len: Int): Int = channel.read(bytes, offset, len)

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

  // TODO block read if the recording is still in progress but we hit EOF? (wait for the event to come...)

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
