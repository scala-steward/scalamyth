package mythtv
package connection
package myth

// TODO what exactly is this class for?
// Reading/writing(?) from a file that is being downloaded to the server using DOWNLOAD_FILE ?
class DownloadFileTransferChannel private[myth](
  controlChannel: MythFileTransferAPI,
  dataChannel: FileTransferConnection,
  eventChannel: EventConnection
) extends EventingFileTransferChannel(controlChannel, dataChannel, eventChannel) {

  override protected def listener: EventListener = downloadListener

  private[this] lazy val downloadListener = new EventListener {
    override def listenFor(event: BackendEvent): Boolean = event.isEventName("DOWNLOAD_FILE")

    override def handle(event: BackendEvent): Unit = event.parse match {
      case Event.DownloadFileUpdateEvent(url, fileName, received, total) =>
        // TODO verify that the url/filename matches the file we're downloading
        currentSize = received  // TODO is this the update we want?
      case Event.DownloadFileFinished(url, fileName, fileSize, err, errCode) =>
        // TODO verify that the url/filename matches the file we're downloading
        currentSize = fileSize
        // TODO initiate finalization of this object/mark as completed?
      case _ => ()
    }
  }
}
