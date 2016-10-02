package mythtv
package connection
package myth

import java.time.Instant

import util.ByteCount

case class FileStats(
  deviceId: Long,
  inode: Long,
  mode: Int,
  numLinks: Long,
  uid: Int,
  gid: Int,
  rDevId: Long,
  size: ByteCount,
  fsBlockSize: ByteCount,
  blocks: Long,
  accessTime: Instant,
  modifyTime: Instant,
  changeTime: Instant)

object FileStats {
  def apply(data: IndexedSeq[String]): FileStats =
    FileStats(
      data(0).toLong,
      data(1).toLong,
      data(2).toInt,
      data(3).toLong,
      data(4).toInt,
      data(5).toInt,
      data(6).toLong,
      new ByteCount(data(7).toLong),
      new ByteCount(data(8).toLong),
      data(9).toLong,
      Instant.ofEpochSecond(data(10).toLong),
      Instant.ofEpochSecond(data(11).toLong),
      Instant.ofEpochSecond(data(12).toLong)
    )
}
