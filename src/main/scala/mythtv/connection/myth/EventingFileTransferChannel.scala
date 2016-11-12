package mythtv
package connection
package myth

abstract class EventingFileTransferChannel(
  controlChannel: MythFileTransferAPI,
  dataChannel: FileTransferConnection,
  eventChannel: EventConnection
) extends FileTransferChannel(controlChannel, dataChannel) {

  eventChannel.addListener(listener)

  protected def listener: EventListener

  override def close(): Unit = {
    super.close()
    eventChannel.removeListener(listener)
    eventChannel.disconnect()  // TODO temporarary
  }
}
