// SPDX-License-Identifier: LGPL-2.1-only
/*
 * MythFileHash.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package util

import java.nio.{ ByteBuffer, ByteOrder }
import java.nio.channels.SeekableByteChannel
import java.nio.file.{ Files, Path, StandardOpenOption }

class MythFileHash(val hash: String) extends AnyVal {
  def verify(data: ByteBuffer): Boolean = this == MythFileHash(data)
  def verify(data: Array[Byte]): Boolean = this == MythFileHash(data)
  def verify(path: Path): Boolean = this == MythFileHash(path)
  def verify(channel: SeekableByteChannel): Boolean = this == MythFileHash(channel)
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

  def apply(data: Array[Byte]): MythFileHash =
    apply(ByteBuffer.wrap(data))

  def apply(bb: ByteBuffer): MythFileHash = {
    val initialsize = bb.remaining

    if (initialsize == 0) new MythFileHash("NULL")
    else {
      val origByteOrder = bb.order
      bb.order(ByteOrder.LITTLE_ENDIAN)

      var hash: Long = initialsize
      hash = chunk(bb, hash)

      val repos = initialsize - ChunkSize
      if (repos >= 0) {
        bb.position(repos)
        hash = chunk(bb, hash)
      }

      bb.order(origByteOrder)

      new MythFileHash(hash.toHexString)
    }
  }

  def apply(path: Path): MythFileHash = {
    val channel = Files.newByteChannel(path, StandardOpenOption.READ)
    try {
      apply(channel)
    } finally {
      channel.close()
    }
  }

  def apply(channel: SeekableByteChannel): MythFileHash = {
    val totalSize = channel.size
    if (totalSize == 0) new MythFileHash("NULL")
    else {
      var hash: Long = totalSize
      val bb = ByteBuffer.allocate(ChunkSize).order(ByteOrder.LITTLE_ENDIAN)

      channel.position(0)
      bb.position(0)
      bb.limit(math.min(bb.capacity, totalSize).toInt)
      while (bb.hasRemaining) channel.read(bb)
      hash = chunk({ bb.flip(); bb }, hash)

      val repos = totalSize - ChunkSize
      if (repos >= 0) {
        bb.clear()
        channel.position(repos)
        while (bb.hasRemaining) channel.read(bb)
        hash = chunk({ bb.flip(); bb }, hash)
      }

      new MythFileHash(hash.toHexString)
    }
  }
}
