package mythtv
package connection
package myth

final case class FileTransferId(id: Int) extends AnyVal

/**
  * The API in use over the control channel during Myth protocol file transfers
  * using the QUERY_FILETRANSFER series of protocol commands.
  */
trait MythFileTransferAPI {
  def done(): Unit
  def isOpen: Boolean
  def reopen(newFileName: String): Boolean
  def requestBlock(blockSize: Int): Int
  def requestSize: Long
  def seek(pos: Long, whence: Int, currentPos: Long): Long
  def setTimeout(fast: Boolean): Unit
  def writeBlock(blockSize: Int): Int
}

class MythFileTransferObject(val ftId: FileTransferId, conn: MythProtocolAPI) extends MythFileTransferAPI {
  def done(): Unit = conn.queryFileTransferDone(ftId)
  def isOpen: Boolean = conn.queryFileTransferIsOpen(ftId)
  def reopen(newFileName: String): Boolean = conn.queryFileTransferReopen(ftId, newFileName)
  def requestBlock(blockSize: Int): Int = conn.queryFileTransferRequestBlock(ftId, blockSize)
  def requestSize: Long = conn.queryFileTransferRequestSize(ftId)
  def seek(pos: Long, whence: Int, currentPos: Long): Long = conn.queryFileTransferSeek(ftId, pos, whence, currentPos)
  def setTimeout(fast: Boolean): Unit = conn.queryFileTransferSetTimeout(ftId, fast)
  def writeBlock(blockSize: Int): Int = conn.queryFileTransferWriteBlock(ftId, blockSize)
}

object MythFileTransferObject {
  def apply(controlChannel: BackendAPIConnection, dataChannel: FileTransferConnection) =
    new MythFileTransferObject(dataChannel.transferId, controlChannel)
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

// TODO are we opening data channel with event mode set (or something??)  Actually, it's readahead...
//   Sending command ANN FileTransfer dove 0 1 2000[]:[]Music/Performances/Rolling_in_the_Deep.mp4[]:[]Videos

class FileTransfer private[myth](controlChannel: MythFileTransferAPI, dataChannel: FileTransferConnection) {
  // A file transfer requires two (optionally three) channels:
  //   - control channel  (BackendConnection or BackendAPIConnection)
  //   - data channel     (FileTransferConnection)
  // and optionally
  //   - event channel    (EventConnection)

  // TODO support Java InputStream/OutputStream interfaces?

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

  // current offset in file
  def tell: Long = position

  // seek to offset (relative to whence)
  def seek(offset: Long, whence: Int): Unit = { // TODO enumeration for whence?
    val adjOffset = whence match {
      case 0 => clamp(offset, 0L, size)
      case 1 => clamp(offset, -position, size - position)
      case 2 => clamp(offset, -size, 0L) + size
    }
    val adjWhence = if (whence == 2) 0 else whence
    val newPos: Long = controlChannel.seek(adjOffset, adjWhence, position)
    if (newPos < 0) throw new RuntimeException("failed seek")
    position = newPos
  }

  // seek to beginning
  def rewind(): Unit = seek(0, whence=0)

  // NB It seems that the myth backend won't send any blocks bigger than 128k no
  //    matter what size we ask for. Is this a hard limit in the server code?

  def read(buf: Array[Byte], off: Int, len: Int): Int = {
    if (!dataChannel.isReadble) throw new RuntimeException("data channel is not readable")
    val adjLen = clamp(len, 0, math.min(size - position, Int.MaxValue)).toInt

    // TODO need to loop until we've read size bytes??  what are normal semantics of Java I/O?
    var count = adjLen  // TODO what value is this really supposed to have? (leftover amount we need in loop)

    val rc = controlChannel.requestBlock(count)
    if (rc != count) {
      if (rc < 0) {
        // TODO failure; re-seek to current position and retry?
        ???
      }
      else count = rc
    }

    val n = dataChannel.read(buf, off, count)
    println(s"Read $n bytes from data channel (desired $count)")
    position += n    // can 'n' and 'count' differ here? Yep!
    n
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

class RecordingFileTransfer(
  controlChannel: MythFileTransferAPI,
  dataChannel: FileTransferConnection,
  eventChannel: EventConnection
) extends EventingFileTransfer(controlChannel, dataChannel, eventChannel) {

  override def listener: EventListener = updateListener

  private[this] lazy val updateListener = new EventListener {
    override def listenFor(event: BackendEvent): Boolean = event.isEventName("UPDATE_FILE_SIZE")

    override def handle(event: BackendEvent): Unit = event.parse match {
      case Event.UpdateFileSizeEvent(chanId, recStartTs, newSize) =>
        // TODO ensure that chanId/recStartTs matches the recording we're transferring
        size = newSize
      case _ => ()
    }
  }
}

// TODO what exactly is this class for?
// Reading/writing(?) from a file that is being downloaded to the server using DOWNLOAD_FILE ?
class DownloadFileTransfer(
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
