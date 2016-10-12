package mythtv
package connection
package myth

import java.net.InetAddress

trait FileTransferConnection extends SocketConnection

// TODO do I want a base class without the [String] reader/writer so I can read/write raw byte blocks?
private abstract class AbstractFileTransferConnection(host: String, port: Int, timeout: Int, fileName: String, storageGroup: String)
    extends AbstractBackendConnection(host, port, timeout)
    with FileTransferConnection {

  self: MythProtocolAPI with AnnouncingConnection =>

  protected def announce(): Unit = {
    val localHost = InetAddress.getLocalHost().getHostName()
    val (ftID, size) = announceFileTransfer(localHost, fileName, storageGroup)
  }

  override def sendCommand(command: String, args: Any*): Option[_] = {
    if (hasAnnounced) None
    else super.sendCommand(command, args: _*)
  }

}
