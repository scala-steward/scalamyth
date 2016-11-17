package mythtv
package connection
package myth

trait BackendAPIConnection extends BackendConnection with MythProtocolAPI {
  // defined here rather than in BackendAPILike so we have access to call disconnect()
  def done(): Unit = disconnect(graceful = true)
}

object BackendAPIConnection {

  private val supportedVersions = Map[Int, BackendAPIConnectionFactory](
    75 -> BackendAPIConnection75,
    77 -> BackendAPIConnection77
  )

  def apply(
    host: String,
    port: Int = BackendConnection.DefaultPort,
    timeout: Int = BackendConnection.DefaultTimeout
  ): BackendAPIConnection = {
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

private sealed trait BackendAPIConnectionFactory {
  def apply(host: String, port: Int, timeout: Int): BackendAPIConnection
}

private class BackendAPIConnection75(host: String, port: Int, timeout: Int)
    extends BackendConnection75(host, port, timeout)
    with BackendAPIConnection
    with BackendAPILike

private object BackendAPIConnection75 extends BackendAPIConnectionFactory {
  def apply(host: String, port: Int, timeout: Int) = new BackendAPIConnection75(host, port, timeout)
}

private class BackendAPIConnection77(host: String, port: Int, timeout: Int)
    extends BackendConnection77(host, port, timeout)
    with BackendAPIConnection
    with BackendAPILike

private object BackendAPIConnection77 extends BackendAPIConnectionFactory {
  def apply(host: String, port: Int, timeout: Int) = new BackendAPIConnection77(host, port, timeout)
}
