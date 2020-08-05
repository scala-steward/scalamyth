// SPDX-License-Identifier: LGPL-2.1-only
/*
 * FileTransferChannel.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth

import java.io.{ IOException, InputStream, OutputStream }
import java.nio.ByteBuffer
import java.nio.channels.{ Channels, SeekableByteChannel }
import java.nio.channels.{ NonReadableChannelException, NonWritableChannelException }

import EnumTypes.SeekWhence

trait FileTransferChannel extends SeekableByteChannel with Seekable with FileTransfer

private[myth] class FileTransferChannelImpl(controlConn: FileTransferAPI, dataConn: FileTransferConnection)
  extends FileTransferChannel {
  // A file transfer requires two (optionally three) connections:
  //   - control connection  (BackendConnection or BackendAPIConnection)
  //   - data connection     (FileTransferConnection)
  // and optionally
  //   - event connection    (EventConnection)

  @volatile protected var currentSize: Long = dataConn.fileSize
  protected var currentPosition: Long = 0L
  private[this] var openStatus: Boolean = true

  private def clamp(value: Long, min: Long, max: Long): Long =
    if (value < min) min
    else if (value > max) max
    else value

  // close the file
  override def close(): Unit = {
    controlConn.done()
    dataConn.close()
    openStatus = false
  }

  override def fileName: String = dataConn.fileName

  override def storageGroup: String = dataConn.storageGroup

  override def fileSize: Long = dataConn.fileSize

  override def size: Long = currentSize

  override def isOpen: Boolean = openStatus

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
    val newPos: Long = controlConn.seek(adjOffset, adjWhence, currentPosition) match {
      case Right(x) => x        // TODO more detailed exception info
      case Left(e) => throw new IOException("failed seek API call on file transfer")
    }
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

  protected def waitForMoreData(oldSize: Long): Boolean = false

  private def readableLength(len: Int, size: Long): Int =
    clamp(len, 0, math.min(size - currentPosition, Int.MaxValue)).toInt

  private def waitableReadableLength(len: Int): Int = {
    val origSize = currentSize
    val origLength = readableLength(len, origSize)
    if (origLength == 0 && waitForMoreData(origSize)) readableLength(len, currentSize)
    else origLength
  }

  override def read(bb: ByteBuffer): Int = {
    if (!dataConn.isReadable) throw new NonReadableChannelException
    val length = waitableReadableLength(bb.remaining)
    if (length < bb.remaining) bb.limit(bb.position() + length)

    var bytesRead: Int = 0
    var canReadMore: Boolean = true

    while (bb.hasRemaining && canReadMore) {
      val requestSize = length - bytesRead
      val allotedSize = controlConn.requestBlock(requestSize) match {
        case Right(x) => x        // TODO more detailed exception info
        case Left(e) => throw new IOException("failed requestBlock API call on file transfer")
      }
      assert(requestSize == bb.remaining)

      if (allotedSize != requestSize) {}  // TODO do I want to take some action here?

      if (allotedSize < 0) {
        // TODO failure; re-seek to current position and retry (a maximum number of times?)
      } else if (allotedSize == 0) {
        canReadMore = false
      } else {
        var bytesReadThisRequest: Int = 0

        while (bytesReadThisRequest < allotedSize && canReadMore) {
          val n = dataConn.read(bb)
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
    if (!dataConn.isWritable) throw new NonWritableChannelException

    var bytesWritten = 0
    while (bb.hasRemaining) {
      // TODO is there a limit or guideline on how much data I can write here at once? 128k seems to be a good idea
      // dataConn has a blocking socket, so no need to select for waitable here
      val n = dataConn.write(bb)
      bytesWritten += n
    }

    var serverWritten = 0
    if (bytesWritten > 0) {
      serverWritten = controlConn.writeBlock(bytesWritten).get  // TODO may throw exception
      if (serverWritten < 0) {
        // the write failed on the server (after reading from the socket)
        // we don't really have enough info to be able to recover
        throw new IOException("write failed on backend")
      }
      else {
        currentPosition = math.max(currentPosition + serverWritten, currentSize)
        if (serverWritten != bytesWritten) println("WARN: server wrote fewer bytes than we sent")  // TODO
      }
    }

    serverWritten
  }

  override def truncate(size: Long): SeekableByteChannel = {
    throw new IOException("truncate is not supported")
    this
  }
}

object FileTransferChannel {
  def apply(
    controlConn: MythProtocolAPIConnection,
    fileName: String,
    storageGroup: String,
    writeMode: Boolean = false,
    useReadAhead: Boolean = false
  ): FileTransferChannel = {
    val dataConn = FileTransferConnection(
      controlConn.host,
      fileName,
      storageGroup,
      writeMode,
      useReadAhead,
      controlConn.port
    )
    val fto = MythFileTransferObject(controlConn, dataConn)
    new FileTransferChannelImpl(fto, dataConn)
  }

  def newInputStream(channel: FileTransferChannel): InputStream = new FileTransferInputStream(channel)

  def newOutputStream(channel: FileTransferChannel): OutputStream = Channels.newOutputStream(channel)
}
