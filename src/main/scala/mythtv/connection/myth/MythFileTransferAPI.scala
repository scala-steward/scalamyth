package mythtv
package connection
package myth

import EnumTypes.SeekWhence
import MythProtocol.MythProtocolFailure

/**
  * The API in use over the control channel during Myth protocol file transfers
  * using the QUERY_FILETRANSFER series of protocol commands.
  */
trait MythFileTransferAPI {
  def done(): Unit
  def isOpen: Either[MythProtocolFailure, Boolean]
  def reopen(newFileName: String): Either[MythProtocolFailure, Boolean]
  def requestBlock(blockSize: Int): Either[MythProtocolFailure, Int]
  def requestSize: Either[MythProtocolFailure, Long]
  def seek(pos: Long, whence: SeekWhence, currentPos: Long): Either[MythProtocolFailure, Long]
  def setTimeout(fast: Boolean): Unit
  def writeBlock(blockSize: Int): Either[MythProtocolFailure, Int]
}

trait MythFileTransferAPILike extends MythFileTransferAPI {
  def ftId: FileTransferId
  def protoApi: MythProtocolAPI

  def done(): Unit =
    protoApi.queryFileTransferDone(ftId)

  def isOpen: Either[MythProtocolFailure, Boolean] =
    protoApi.queryFileTransferIsOpen(ftId)

  def reopen(newFileName: String): Either[MythProtocolFailure, Boolean] =
    protoApi.queryFileTransferReopen(ftId, newFileName)

  def requestBlock(blockSize: Int): Either[MythProtocolFailure, Int] =
    protoApi.queryFileTransferRequestBlock(ftId, blockSize)

  def requestSize: Either[MythProtocolFailure, Long] =
    protoApi.queryFileTransferRequestSize(ftId)

  def seek(pos: Long, whence: SeekWhence, currentPos: Long): Either[MythProtocolFailure, Long] =
    protoApi.queryFileTransferSeek(ftId, pos, whence, currentPos)

  def setTimeout(fast: Boolean): Unit =
    protoApi.queryFileTransferSetTimeout(ftId, fast)

  def writeBlock(blockSize: Int): Either[MythProtocolFailure, Int] =
    protoApi.queryFileTransferWriteBlock(ftId, blockSize)
}
