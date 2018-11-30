package examples.protocol

import java.nio.ByteBuffer

import mythtv.connection.myth.FileTransferChannel
import mythtv.util.{ ByteCount, DecimalByteCount }

trait Xfer {

  final val BUFFER_SIZE: Int = 256 * 1024

  /*
   * Read data from the backend via a FileTransferChannel, taking note of the chunk
   * sizes we receive. Discard the actual data, but return total count of bytes.
   */
  def doTransfer(ft: FileTransferChannel, verbose: Boolean = false): ByteCount = {
    val buf = ByteBuffer.allocate(BUFFER_SIZE)
    val counts: collection.mutable.Set[Int] = collection.mutable.Set.empty
    var totalRequested: Long = 0L
    var totalRead: Long = 0L

    var n = 0
    do {
      totalRequested += buf.remaining
      n = ft.read(buf)
      totalRead += n
      if (!counts.contains(n)) {
        counts += n
        if (verbose) println(counts)
      }
      buf.clear()
    } while (n > 0)

    if (verbose) println(totalRead)
    ft.close()

    DecimalByteCount(totalRead)
  }
}
