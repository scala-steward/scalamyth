package mythtv
package util

import java.nio.{ ByteOrder, ByteBuffer }
import java.nio.file.{ Files, Path, StandardOpenOption }

class MythFileHash(val hash: String) extends AnyVal {
  def verify(data: Array[Byte]): Boolean = this == MythFileHash(data)
  def verify(path: Path): Boolean = this == MythFileHash(path)
  override def toString: String = hash
}

object MythFileHash {
  private final val ChunkSize = 65536
  private final val LongSize = java.lang.Long.SIZE / 8
  private final val NumChunks = ChunkSize / LongSize

  private def chunk(bb: ByteBuffer, initHash: Long): Long = {
    val buf = bb.asLongBuffer
    val n = math.min(buf.remaining, NumChunks)
    var hash = initHash
    var i = 0
    while (i < n) {
      hash += buf.get()
      i += 1
    }
    hash
  }

  def apply(data: Array[Byte]): MythFileHash = {
    val initialsize = data.length

    if (initialsize == 0) new MythFileHash("NULL")
    else {
      val bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
      var hash: Long = initialsize

      hash = chunk(bb, hash)

      val repos = initialsize - ChunkSize
      if (repos >= 0) {
        bb.position(repos)
        hash = chunk(bb, hash)
      }

      new MythFileHash(hash.toHexString)
    }
  }

  def apply(path: Path): MythFileHash = {
    val initialsize = Files.size(path)
    if (initialsize == 0) new MythFileHash("NULL")
    else {
      var hash: Long = initialsize
      val channel = Files.newByteChannel(path, StandardOpenOption.READ)

      try {
        val bb = ByteBuffer.allocate(ChunkSize).order(ByteOrder.LITTLE_ENDIAN)

        bb.clear()
        val nRead = channel.read(bb)  // TODO check # bytes read was sufficient

        bb.flip()
        hash = chunk(bb, hash)

        val repos = initialsize - ChunkSize
        if (repos >= 0) {
          channel.position(repos)

          bb.clear()
          val nRead = channel.read(bb)    // TODO check # bytes read was sufficient

          bb.flip()
          hash = chunk(bb, hash)
        }
      } finally {
        channel.close()
      }

      new MythFileHash(hash.toHexString)
    }
  }
}
