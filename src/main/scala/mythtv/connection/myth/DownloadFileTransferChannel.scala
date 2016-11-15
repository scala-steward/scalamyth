package mythtv
package connection
package myth

// TODO what exactly is this class for?
// Reading/writing(?) from a file that is being downloaded to the server using DOWNLOAD_FILE ?
private class DownloadFileTransferChannel(
  controlChannel: MythFileTransferAPI,
  dataChannel: FileTransferConnection,
  eventChannel: EventConnection
) extends EventingFileTransferChannel(controlChannel, dataChannel, eventChannel) {

  import Event.{ DownloadFileFinishedEvent, DownloadFileUpdateEvent }

  override protected def listener: EventListener = downloadListener

  private[this] lazy val downloadListener = new EventListener {
    override def listenFor(event: Event): Boolean = event match {
      case e: DownloadFileUpdateEvent => true
      case e: DownloadFileFinishedEvent => true
      case _ => false
    }

    override def handle(event: Event): Unit = event match {
      case DownloadFileUpdateEvent(url, fileName, received, total) =>
        // TODO verify that the url/filename matches the file we're downloading
        currentSize = received.bytes // TODO is this the update we want?
      case DownloadFileFinishedEvent(url, fileName, fileSize, err, errCode) =>
        // TODO verify that the url/filename matches the file we're downloading
        currentSize = fileSize.bytes
        // TODO initiate finalization of this object/mark as completed?
      case _ => ()
    }
  }
}
