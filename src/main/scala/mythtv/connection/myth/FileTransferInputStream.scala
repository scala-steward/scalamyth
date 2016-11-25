package mythtv
package connection
package myth

import java.io.{ IOException, InputStream }
import java.nio.ByteBuffer

class FileTransferInputStream private[myth](channel: FileTransferChannel) extends InputStream with FileTransfer {
  private[this] var markPosition: Long = -1L

  override def fileName: String = channel.fileName

  override def storageGroup: String = channel.storageGroup

  override def fileSize: Long = channel.fileSize
  override def close(): Unit = channel.close()

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
