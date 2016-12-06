package mythtv
package connection
package myth

trait MythProtocolAPIConnection extends BackendConnection with MythProtocolAPI {
  // defined here rather than in BackendAPILike so we have access to call disconnect()
  def done(): Unit = disconnect(graceful = true)
}

object MythProtocolAPIConnection {

  private val supportedVersions = Map[Int, MythProtocolAPIConnectionFactory](
    75 -> MythProtocolAPIConnection75,
    77 -> MythProtocolAPIConnection77,
    88 -> MythProtocolAPIConnection88
  )

  def apply(
    host: String,
    port: Int = BackendConnection.DefaultPort,
    timeout: Int = BackendConnection.DefaultTimeout
  ): MythProtocolAPIConnection = {
    try {
      val factory = supportedVersions(BackendConnection.DefaultVersion)
      factory(host, port, timeout)
    } catch {
      case ex @ WrongMythProtocolException(requiredVersion) =>
        if (supportedVersions contains requiredVersion) {
          val factory = supportedVersions(requiredVersion)
          factory(host, port, timeout)
        }
        else throw new UnsupportedMythProtocolException(ex)
    }
  }
}

private sealed trait MythProtocolAPIConnectionFactory {
  def apply(host: String, port: Int, timeout: Int): MythProtocolAPIConnection
}

private class MythProtocolAPIConnection75(host: String, port: Int, timeout: Int)
  extends BackendConnection75(host, port, timeout)
     with MythProtocolAPIConnection
     with MythProtocolApi75
     with MythProtocolAPILike

private class MythProtocolAPIConnection77(host: String, port: Int, timeout: Int)
  extends BackendConnection77(host, port, timeout)
     with MythProtocolAPIConnection
     with MythProtocolApi77
     with MythProtocolAPILike

private class MythProtocolAPIConnection88(host: String, port: Int, timeout: Int)
  extends BackendConnection88(host, port, timeout)
    with MythProtocolAPIConnection
    with MythProtocolApi88
    with MythProtocolAPILike

private object MythProtocolAPIConnection75 extends MythProtocolAPIConnectionFactory {
  def apply(host: String, port: Int, timeout: Int) = new MythProtocolAPIConnection75(host, port, timeout)
}

private object MythProtocolAPIConnection77 extends MythProtocolAPIConnectionFactory {
  def apply(host: String, port: Int, timeout: Int) = new MythProtocolAPIConnection77(host, port, timeout)
}

private object MythProtocolAPIConnection88 extends MythProtocolAPIConnectionFactory {
  def apply(host: String, port: Int, timeout: Int) = new MythProtocolAPIConnection88(host, port, timeout)
}
