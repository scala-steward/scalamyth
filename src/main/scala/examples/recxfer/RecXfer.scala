package examples.recxfer

import mythtv._
import model._
import connection.myth._

object RecXfer {
  def doTransfer(api: MythProtocolAPIConnection, rec: Recording): Long = {
    val ft = RecordingTransferChannel(api, rec)
    val buf = java.nio.ByteBuffer.allocate(256*1024)
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
        println(counts)
      }
      buf.clear()
    } while (n > 0)

    println(totalRead)
    ft.close()
    totalRead
  }

  def main(args: Array[String]): Unit = {
    val api = MythProtocolAPIConnection("myth1")
    api.announce("Monitor", "dove")

    for {
      rec <- api.queryRecorderGetCurrentRecording(CaptureCardId(4))
      totalRead = doTransfer(api, rec)
    } yield totalRead

    println("done")
  }
}
