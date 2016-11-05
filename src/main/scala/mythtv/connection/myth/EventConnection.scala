package mythtv
package connection
package myth

import java.net.SocketException

import util.NetworkUtil
import EnumTypes.MythProtocolEventMode

trait BackendEvent extends Any with BackendResponse {
  def isSystemEvent: Boolean = raw.substring(20,32) == "SYSTEM_EVENT"
}

private object BackendEvent {
  def apply(r: String): BackendResponse = Event(r)
}

private final case class Event(raw: String) extends AnyVal with BackendEvent


/*
 * Some BACKEND_MESSAGE examples
 *
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT NET_CTRL_CONNECTED SENDER mythfe1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT NET_CTRL_DISCONNECTED SENDER mythfe2[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT SCREEN_TYPE DESTROYED playbackbox SENDER mythfe1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT SCREEN_TYPE CREATED mythscreentypebusydialog SENDER mythfe1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT CLIENT_CONNECTED HOSTNAME myth1 SENDER myth1[]:[]empty
 *  BACKEND_MESSAGE[]:[]SYSTEM_EVENT CLIENT_DISCONNECTED HOSTNAME myth1 SENDER myth1[]:[]empty
 *  BACKEND_MESSAGE[]:[]VIDEO_LIST_NO_CHANGE[]:[]empty
 *  BACKEND_MESSAGE[]:[]RECORDING_LIST_CHANGE UPDATE[]:[]Survivor[]:[]Not Going Down Without a Fight[]:[]Castaways from all three tribes remain, and one will be crowned the Sole Survivor.[]:[]32[]:[]14[]:[]3214[]:[]Reality[]:[]1081[]:[]8-1[]:[]KFMB-DT[]:[]KFMBDT (KFMB-DT)[]:[]1081_20160519030000.mpg[]:[]13323011228[]:[]1463626800[]:[]1463634000[]:[]0[]:[]myth1[]:[]0[]:[]0[]:[]0[]:[]0[]:[]-3[]:[]380[]:[]0[]:[]15[]:[]6[]:[]1463626800[]:[]1463634001[]:[]11583492[]:[]Reality TV[]:[][]:[]EP00367078[]:[]EP003670780116[]:[]76733[]:[]1471849923[]:[]0[]:[]2016-05-18[]:[]Default[]:[]0[]:[]0[]:[]Default[]:[]9[]:[]17[]:[]1[]:[]0[]:[]0[]:[]0
 *  BACKEND_MESSAGE[]:[]GENERATED_PIXMAP[]:[]OK[]:[]1081_2016-05-19T05:00:00Z[]:[]Generated on myth1 in 3.919 seconds, starting at 16:19:33[]:[]2016-10-05T23:19:33Z[]:[]83401[]:[]39773[]:[] <<< base64 data + extra tokens redacted >>>
 *  BACKEND_MESSAGE[]:[]GENERATED_PIXMAP[]:[]OK[]:[]1151_2016-07-30T20:30:00Z[]:[]On Disk[]:[]2016-10-05T23:19:42Z[]:[]87547[]:[]13912[]:[] << base64 data + extra tokens redacted >>
 */

/*
 * Some MESSAGE examples
 *
 *   MESSAGE[]:[]MASTER_UPDATE_PROG_INFO 1151 2016-04-14T04:00:00Z
 */

/*
 * Format of SYSTEM_EVENT
 *   SYSTEM_EVENT %s SENDER %s
 *
 *   also look into libs/libmythtv/mythsystemevent.cpp
 *              and libs/libmythbase/mythcorecontext.cpp
 */

/*
 * Format of GENERATED_PIXMAP
 *  On success:
 *      "OK"
 *      <programInfoKey>   (from pginfo->MakeUniqueKey())
 *      <msg>              (e.g "On Disk" or "Generated...")
 *      <datetime>         from the PREVIEW_SUCCESS Qt event received by the backend
 *      <dataSize>         byte count of the data (in binary, not encoded base 64)
 *      <checksum>         CRC-16 of the binary data (computed by qChecksum)
 *      <base64Data>       data encoded in base-64 format
 *      <token> *          may not be present, or may be multiple!
 *  On failure:
 *      "ERROR"
 *      <programInfoKey>
 *      <msg>              from the PREVIEW_FAILURE Qt event received by the backend
 *      <token> *          may not be present, or may be multiple!
 *   requestor token should be the first one in the token list?
 */

/*
 * Format of VIDEO_LIST_CHANGE
 *   ["added:%d"]*            %d is VideoId; repeated for each addition
 *   ["moved:%d"]*                 "       ;     "     "    "  file move
 *   ["deleted:%d"]*               "       ;     "     "    "  deletion
 */

/*
 * Format of VIDEO_LIST_NO_CHANGE
 *   <empty>
 */

/*
 * Format of RECORDING_LIST_CHANGE
 *   <may have no body>
 *   ADD     <chanId>
 *   DELETE  <recstartts:ISO>
 *   UPDATE  ?? see code in mainserver.cpp
 *     update is sent by mainserver in response to Qt event MASTER_UPDATE_PROG_INFO
 */

/*
 * Format of DOWNLOAD_FILE
 *   UPDATE   <url> <outFile> <bytesReceived> <bytesTotal>
 *   FINISHED <url> <outfile> <fileSize> <errorStringPlaceholder> <errorCode>
 */

/*
 * Format of UPDATE_FILE_SIZE
 *   <chanId> <recstartts:ISO> <fileSize>
 */

/*
 * Format of MASTER_UPDATE_PROG_INFO
 *   <chanId> <recstartts:ISO>
 */

/*
 * Some other events:
 *   LOCAL_RECONNECT_TO_MASTER
 *   LOCAL_SLAVE_BACKEND_ONLINE %2
 *   LOCAL_SLAVE_BACKEND_OFFLINE %1
 *   LOCAL_SLAVE_BACKEND_ENCODERS_OFFLINE
 *   THEME_INSTALLED
 *   THEME_RELOAD
 */

/*
 * Some system events:
 *   CLIENT_CONNECTED HOSTNAME %1
 *   CLIENT_DISCONNECTED HOSTNAME %1
 *   SLAVE_CONNECTED HOSTNAME %1
 *   SLAVE_DISCONNECTED HOSTNAME %1
 *   REC_DELETED CHANID %1 STARTTIME %2
 *   AIRTUNES_NEW_CONNECTION
 *   AIRTUNES_DELETE_CONNECTION
 *   AIRPLAY_NEW_CONNECTION
 *   AIRPLAY_DELETE_CONNECTION
 *   LIVETV_STARTED
 *   PLAY_STOPPED
 *   TUNING_SIGNAL_TIMEOUT CARDID %1
 *   MASTER_STARTED
 *   MASTER_SHUTDOWN
 *   SCHEDULER_RAN
 *   NET_CTRL_DISCONNECTED
 *   NET_CTRL_CONNECTED
 *   MYTHFILLDATABASE_RAN
 *   SCREEN_TYPE CREATED %1
 *   SCREEN_TYPE DESTROYED %1
 *   THEME_INSTALLED PATH %1
 */

trait EventListener {
  def listenFor(event: BackendEvent): Boolean  // TODO rename to filter?
  def handle(event: BackendEvent): Unit
}

trait EventConnection extends SocketConnection { /* with EventProtocol ?? */
  def addListener(listener: EventListener): Unit
  def removeListener(listener: EventListener): Unit

  def listeners: Set[EventListener]

  final def += (listener: EventListener): Unit = addListener(listener)
  final def -= (listener: EventListener): Unit = removeListener(listener)
}

// TODO don't use infinite timeout for protocol negotiation
private abstract class AbstractEventConnection(
  host: String, port: Int, val eventMode: MythProtocolEventMode)
    extends AbstractBackendConnection(host, port, 0) with EventConnection {

  self: AnnouncingConnection =>

  private[this] var listenerSet: Set[EventListener] = Set.empty
  private[this] var eventLoopThread: Thread = _

  def announce(): Unit = {
    val localHost = NetworkUtil.myHostName
    val result = sendCommand("ANN", "Monitor", localHost, eventMode)
  }

  override def sendCommand(command: String, args: Any*): Option[_] = {
    if (hasAnnounced) None
    else super.sendCommand(command, args: _*)
  }

  def listeners: Set[EventListener] = synchronized { listenerSet }

  def addListener(listener: EventListener): Unit = {
    synchronized { listenerSet = listenerSet + listener }
    if (!isEventLoopRunning) eventLoopThread = startEventLoop
  }

  def removeListener(listener: EventListener): Unit = {
    synchronized { listenerSet = listenerSet - listener }
  }

  // blocking read to wait for the next event
  // TODO this only blocks for 'timeout' seconds!
  protected def readEvent(): BackendEvent = Event(reader.read())

  private def isEventLoopRunning: Boolean =
    if (eventLoopThread eq null) false
    else eventLoopThread.isAlive

  private def startEventLoop: Thread = {
    val thread = new Thread(new EventLoop)
    thread.start()
    thread
  }

  private class EventLoop extends Runnable {
    // TODO : this approach has the disadvantage that event listener de-/registration
    //   does not become visible until after the next event is received (which may not
    //   be for some time)  Can we interrupt the blocked call to process?

    // TODO need to catch SocketException: Socket closed (when disconnect() is called)
    def run(): Unit = {
      var myListeners = listeners
      while (myListeners.nonEmpty && isConnected) {
        try {
          val event = readEvent()
          myListeners = listeners
          for (ear <- myListeners) {
            if (ear.listenFor(event))
              ear.handle(event)
          }
        } catch {
          case ex: SocketException =>
        }
      }
    }
  }
}

private sealed trait EventConnectionFactory {
  def apply(host: String, port: Int, eventMode: MythProtocolEventMode): EventConnection
}

object EventConnection {
  private val supportedVersions = Map[Int, EventConnectionFactory](
    75 -> EventConnection75,
    77 -> EventConnection77
  )

  def apply(
    host: String,
    port: Int = BackendConnection.DEFAULT_PORT,
    eventMode: MythProtocolEventMode = MythProtocolEventMode.Normal
  ): EventConnection = {
    try {
      val factory = supportedVersions(BackendConnection.DEFAULT_VERSION)
      factory(host, port, eventMode)
    } catch {
      case ex @ WrongMythProtocolException(requiredVersion) =>
        if (supportedVersions contains requiredVersion) {
          val factory = supportedVersions(requiredVersion)
          factory(host, port, eventMode)
        }
        else throw new UnsupportedMythProtocolException(ex)
    }
  }
}

// NB Important that AnnouncingConnection is listed last, for initialization order

private class EventConnection75(host: String, port: Int, eventMode: MythProtocolEventMode)
    extends AbstractEventConnection(host, port, eventMode)
    with MythProtocol75
    with AnnouncingConnection

private object EventConnection75 extends EventConnectionFactory {
  def apply(host: String, port: Int, eventMode: MythProtocolEventMode) =
    new EventConnection75(host, port, eventMode)
}

private class EventConnection77(host: String, port: Int, eventMode: MythProtocolEventMode)
    extends AbstractEventConnection(host, port, eventMode)
    with MythProtocol77
    with AnnouncingConnection

private object EventConnection77 extends EventConnectionFactory {
  def apply(host: String, port: Int, eventMode: MythProtocolEventMode) =
    new EventConnection77(host, port, eventMode)
}
