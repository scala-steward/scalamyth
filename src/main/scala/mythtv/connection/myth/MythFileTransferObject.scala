package mythtv
package connection
package myth

private class MythFileTransferObject(
  val ftId: FileTransferId,
  val protoApi: MythProtocolAPI
) extends FileTransferAPILike

private object MythFileTransferObject {
  def apply(controlChannel: BackendAPIConnection, dataChannel: FileTransferConnection) =
    new MythFileTransferObject(dataChannel.transferId, controlChannel)
}
