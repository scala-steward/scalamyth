package mythtv
package connection
package myth

import EnumTypes.SeekWhence

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
  def seek(pos: Long, whence: SeekWhence, currentPos: Long): Long
  def setTimeout(fast: Boolean): Unit
  def writeBlock(blockSize: Int): Int
}

trait MythFileTransferAPILike extends MythFileTransferAPI {
  def ftId: FileTransferId
  protected def protoApi: MythProtocolAPI

  def done(): Unit =
    protoApi.queryFileTransferDone(ftId)

  def isOpen: Boolean =
    protoApi.queryFileTransferIsOpen(ftId)

  def reopen(newFileName: String): Boolean =
    protoApi.queryFileTransferReopen(ftId, newFileName)

  def requestBlock(blockSize: Int): Int =
    protoApi.queryFileTransferRequestBlock(ftId, blockSize)

  def requestSize: Long =
    protoApi.queryFileTransferRequestSize(ftId)

  def seek(pos: Long, whence: SeekWhence, currentPos: Long): Long =
    protoApi.queryFileTransferSeek(ftId, pos, whence, currentPos)

  def setTimeout(fast: Boolean): Unit =
    protoApi.queryFileTransferSetTimeout(ftId, fast)

  def writeBlock(blockSize: Int): Int =
    protoApi.queryFileTransferWriteBlock(ftId, blockSize)
}
