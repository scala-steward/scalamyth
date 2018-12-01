// SPDX-License-Identifier: LGPL-2.1-only
/*
 * DownloadTransferChannel.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth

import java.io.Closeable
import java.net.URI

import Event.{ DownloadFileFinishedEvent, DownloadFileUpdateEvent }

private class DownloadTransfer(api: MythProtocolAPIConnection, sourceUri: URI, storageGroup: String, fileName: String)
  extends Closeable { outer =>
  @volatile private[this] var inProgress: Boolean = true
  @volatile private[this] var channel: TransferChannel = _

  private[this] val channelMonitor = new AnyRef

  private[this] val eventConnection = EventConnection(api.host, api.port, listener = downloadListener)
  private[this] val targetUri = startDownload(sourceUri, storageGroup, fileName)

  override def close(): Unit = {
    eventConnection.close()
  }

  private def startDownload(uri: URI, sg: String, file: String): URI = {
    api.downloadFile(uri, sg, file).get
  }

  private def startTransfer(srcUri: URI, tgtUri: URI, sg: String, file: String): TransferChannel = {
    val dataChannel = FileTransferConnection(api.host, file, sg, port = api.port)
    val fto = MythFileTransferObject(api, dataChannel)
    new TransferChannel(fto, dataChannel, srcUri, tgtUri)
  }

  private def setupChannel(): Unit = {
    if (channel eq null) {
      val xfer = startTransfer(sourceUri, targetUri, storageGroup, fileName)
      channelMonitor.synchronized {
        channel = xfer
        channelMonitor.notifyAll()
      }
    }
  }

  final def waitForTransferChannel(): DownloadTransferChannel = {
    channelMonitor.synchronized {
      while (channel eq null)
        channelMonitor.wait()
      channel
    }
  }

  private[this] lazy val downloadListener = new EventListener {
    override def listenFor(event: Event): Boolean = event match {
      case _: DownloadFileUpdateEvent => true
      case _: DownloadFileFinishedEvent => true
      case _ => false
    }

    override def handle(event: Event): Unit = event match {
      case DownloadFileUpdateEvent(srcUri, _, received, _) =>
        if (srcUri == sourceUri) {
          setupChannel()
          channel.updateSize(received.bytes)
        }
      case DownloadFileFinishedEvent(srcUri, _, fileSize, _, _) =>
        if (srcUri == sourceUri) {
          setupChannel()
          channel.updateSize(fileSize.bytes)
          inProgress = false
        }
      case _ => ()
    }
  }

  class TransferChannel(controlChannel: FileTransferAPI, dataChannel: FileTransferConnection,
    val targetUri: URI, val sourceUri: URI)
    extends FileTransferChannelImpl(controlChannel, dataChannel)
       with WaitableFileTransferChannel
       with DownloadTransferChannel {

    override def close(): Unit = {
      outer.close()
      super.close()
    }

    final def updateSize(sz: Long): Unit = {
      currentSize = sz
      //println("SIZE UPDATED " + sz)
      signalSizeChanged()
    }

    override final def isInProgress: Boolean = inProgress

    final def isDownloadInProgress: Boolean = inProgress
  }
}

trait DownloadTransferChannel extends FileTransferChannel {
  def sourceUri: URI
  def targetUri: URI
  def isDownloadInProgress: Boolean
}

object DownloadTransferChannel {
  def apply(api: MythProtocolAPIConnection, sourceUri: URI, storageGroup: String, fileName: String): DownloadTransferChannel = {
    val xfer = new DownloadTransfer(api, sourceUri, storageGroup, fileName)
    xfer.waitForTransferChannel()
  }
}
