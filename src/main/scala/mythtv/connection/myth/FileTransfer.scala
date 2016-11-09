package mythtv
package connection
package myth

import EnumTypes.SeekWhence

import model.ChanId
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

class FileTransfer private[myth](controlChannel: MythFileTransferAPI, dataChannel: FileTransferConnection) {
  // A file transfer requires two (optionally three) channels:
  //   - control channel  (BackendConnection or BackendAPIConnection)
  //   - data channel     (FileTransferConnection)
  // and optionally
  //   - event channel    (EventConnection)

  // TODO support full Java InputStream/OutputStream interfaces? + NIO: Readable/Seekwable/Writeable ByteChannel

  @volatile protected[myth] var size: Long = dataChannel.fileSize
  protected[myth] var position: Long = 0L

  private def clamp(value: Long, min: Long, max: Long): Long =
    if (value < min) min
    else if (value > max) max
    else value

  // close the file
  def close(): Unit = {
    controlChannel.done()
    // TODO close the data channel (how? just shut down the socket?)
  }

  def fileName: String = dataChannel.fileName

  def storageGroup: String = dataChannel.storageGroup

  def expectedSize: Long = size

  // current offset in file
  def tell: Long = position

  // seek to offset (relative to whence)
  def seek(offset: Long, whence: SeekWhence): Unit = {
    val adjOffset = whence match {
      case SeekWhence.Begin   => clamp(offset, 0L, size)
      case SeekWhence.Current => clamp(offset, -position, size - position)
      case SeekWhence.End     => clamp(offset, -size, 0L) + size
    }
    val adjWhence = if (whence == SeekWhence.End) SeekWhence.Begin else whence
    val newPos: Long = controlChannel.seek(adjOffset, adjWhence, position)
    if (newPos < 0) throw new RuntimeException("failed seek")
    position = newPos
  }

  // seek to beginning
  def rewind(): Unit = seek(0, SeekWhence.Begin)

  // It seems that the myth backend won't send any blocks bigger than 128k no
  // matter what size we ask for. Is this a hard limit in the server code?
  //
  // Maybe it's a Java socket limit, as the Myth server seems to think I am
  // getting all the bytes?
  //
  // Actually, probably linux, combatting bufferbloat, see:
  //    cat /proc/sys/net/ipv4/tcp_limit_output_bytes

  def read(buf: Array[Byte], off: Int, len: Int): Int = {
    if (!dataChannel.isReadble) throw new RuntimeException("data channel is not readable")
    val length = clamp(len, 0, math.min(size - position, Int.MaxValue)).toInt

    var bytesRead: Int = 0
    var canReadMore: Boolean = true

    while (bytesRead < length && canReadMore) {
      val requestSize = length - bytesRead
      val allotedSize = controlChannel.requestBlock(requestSize)

      if (allotedSize != requestSize) {
        if (allotedSize < 0) {
          // TODO failure; re-seek to current position and retry?
          ???
        }
      }

      var bytesReadThisRequest: Int = 0

      while (bytesReadThisRequest < allotedSize && canReadMore) {
        val toRead = allotedSize - bytesReadThisRequest
        val n = dataChannel.read(buf, off, toRead)
        if (n <= 0) canReadMore = false
        println(s"Read $n bytes from data channel (desired $toRead)")
        bytesReadThisRequest += n
      }
      bytesRead += bytesReadThisRequest
    }
    position += bytesRead
    bytesRead
  }

  def write(buf: Array[Byte], off: Int, len: Int): Unit = {
    if (!dataChannel.isWriteable) throw new RuntimeException("data channel is not writeable")
    if (len > 0) {
      dataChannel.write(buf, off, len)
      controlChannel.writeBlock(len)  // TODO utilize result value? or is it just parroted back to us?
      position = math.max(position + len, size)
    }
  }
}

// TODO what happens if we specify a file that does not exist?
object FileTransfer {  // TODO this doesn't specify read/write mode
def apply(host: String, fileName: String, storageGroup: String): FileTransfer = {
  // TODO how will control channel get closed since it's embeedded here and FT doesn't know that it owns it...
  val controlChannel = BackendAPIConnection(host)
  apply(controlChannel, fileName, storageGroup)
}

  def apply(controlChannel: BackendAPIConnection, fileName: String, storageGroup: String): FileTransfer = {
    val dataChannel = FileTransferConnection(controlChannel.host, fileName, storageGroup, controlChannel.port)
    val fto = MythFileTransferObject(controlChannel, dataChannel)
    new FileTransfer(fto, dataChannel)
  }
}

abstract class EventingFileTransfer(
  controlChannel: MythFileTransferAPI,
  dataChannel: FileTransferConnection,
  eventChannel: EventConnection
) extends FileTransfer(controlChannel, dataChannel) {

  eventChannel.addListener(listener)

  protected def listener: EventListener

  override def close(): Unit = {
    super.close()
    eventChannel.removeListener(listener)
  }
}

class RecordingFileTransfer private[myth](
  controlChannel: MythFileTransferAPI,
  dataChannel: FileTransferConnection,
  eventChannel: EventConnection,
  chanId: ChanId,
  recStartTs: MythDateTime
) extends EventingFileTransfer(controlChannel, dataChannel, eventChannel) {

  override def listener: EventListener = updateListener

  private[this] lazy val updateListener = new EventListener {
    override def listenFor(event: BackendEvent): Boolean = event.isEventName("UPDATE_FILE_SIZE")

    override def handle(event: BackendEvent): Unit = event.parse match {
      case Event.UpdateFileSizeEvent(chan, startTime, newSize) =>
        if (chan == chanId && startTime == recStartTs)
          size = newSize
      case _ => ()
    }
  }
}

object RecordingFileTransfer {
  // TODO: method that takes a hostname vs control channel?
  def apply(api: BackendAPIConnection, chanId: ChanId, recStartTs: MythDateTime): RecordingFileTransfer = {
    val rec = api.queryRecording(chanId, recStartTs)   // TODO check for failure/not found

    // TODO who is managing these opened connections??  Also, we have no re-use...

    val controlChannel =
      if (rec.hostname == api.host) api
      else BackendAPIConnection(rec.hostname)

    val dataChannel = FileTransferConnection(controlChannel.host, rec.filename, rec.storageGroup, controlChannel.port)
    val eventChannel = EventConnection(controlChannel.host, controlChannel.port)

    val fto = MythFileTransferObject(controlChannel, dataChannel)
    new RecordingFileTransfer(fto, dataChannel, eventChannel, chanId, recStartTs)
  }
}

// TODO what exactly is this class for?
// Reading/writing(?) from a file that is being downloaded to the server using DOWNLOAD_FILE ?
class DownloadFileTransfer private[myth](
  controlChannel: MythFileTransferAPI,
  dataChannel: FileTransferConnection,
  eventChannel: EventConnection
) extends EventingFileTransfer(controlChannel, dataChannel, eventChannel) {

  override protected def listener: EventListener = downloadListener

  private[this] lazy val downloadListener = new EventListener {
    override def listenFor(event: BackendEvent): Boolean = event.isEventName("DOWNLOAD_FILE")

    override def handle(event: BackendEvent): Unit = event.parse match {
      case Event.DownloadFileUpdateEvent(url, fileName, received, total) =>
        // TODO verify that the url/filename matches the file we're downloading
        size = received  // TODO is this the update we want?
      case Event.DownloadFileFinished(url, fileName, fileSize, err, errCode) =>
        // TODO verify that the url/filename matches the file we're downloading
        size = fileSize
        // TODO initiate finalization of this object/mark as completed?
      case _ => ()
    }
  }
}
