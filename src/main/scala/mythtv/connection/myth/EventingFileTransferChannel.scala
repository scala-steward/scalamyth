package mythtv
package connection
package myth

private abstract class EventingFileTransferChannel(
  controlChannel: FileTransferAPI,
  dataChannel: FileTransferConnection,
  eventChannel: EventConnection
) extends FileTransferChannelImpl(controlChannel, dataChannel) {

  eventChannel.addListener(listener)

  protected def listener: EventListener

  override def close(): Unit = {
    super.close()
    eventChannel.removeListener(listener)
    eventChannel.disconnect()  // TODO temporarary
  }
}
