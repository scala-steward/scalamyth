// SPDX-License-Identifier: LGPL-2.1-only
/*
 * MythFileTransferObject.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth

private class MythFileTransferObject(
  val ftId: FileTransferId,
  val protoApi: MythProtocolAPI
) extends FileTransferAPILike

private object MythFileTransferObject {
  def apply(controlChannel: MythProtocolAPIConnection, dataChannel: FileTransferConnection) =
    new MythFileTransferObject(dataChannel.transferId, controlChannel)
}
