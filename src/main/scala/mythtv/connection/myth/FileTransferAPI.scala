package mythtv
package connection
package myth

import EnumTypes.SeekWhence
import MythProtocol.MythProtocolFailure

/**
  * The API in use over the control channel during Myth protocol file transfers
  * using the QUERY_FILETRANSFER series of protocol commands.
  */
trait FileTransferAPI {
  def done(): Unit
  def isOpen: MythProtocolResult[Boolean]
  def reopen(newFileName: String): MythProtocolResult[Boolean]
  def requestBlock(blockSize: Int): MythProtocolResult[Int]
  def requestSize: MythProtocolResult[Long]
  def seek(pos: Long, whence: SeekWhence, currentPos: Long): MythProtocolResult[Long]
  def setTimeout(fast: Boolean): Unit
  def writeBlock(blockSize: Int): MythProtocolResult[Int]
}

trait FileTransferAPILike extends FileTransferAPI {
  def ftId: FileTransferId
  def protoApi: MythProtocolAPI

  def done(): Unit =
    protoApi.queryFileTransferDone(ftId)

  def isOpen: MythProtocolResult[Boolean] =
    protoApi.queryFileTransferIsOpen(ftId)

  def reopen(newFileName: String): MythProtocolResult[Boolean] =
    protoApi.queryFileTransferReopen(ftId, newFileName)

  def requestBlock(blockSize: Int): MythProtocolResult[Int] =
    protoApi.queryFileTransferRequestBlock(ftId, blockSize)

  def requestSize: MythProtocolResult[Long] =
    protoApi.queryFileTransferRequestSize(ftId)

  def seek(pos: Long, whence: SeekWhence, currentPos: Long): MythProtocolResult[Long] =
    protoApi.queryFileTransferSeek(ftId, pos, whence, currentPos)

  def setTimeout(fast: Boolean): Unit =
    protoApi.queryFileTransferSetTimeout(ftId, fast)

  def writeBlock(blockSize: Int): MythProtocolResult[Int] =
    protoApi.queryFileTransferWriteBlock(ftId, blockSize)
}
