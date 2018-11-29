package mythtv
package connection
package myth

trait FrontendNetworkControlConnection extends FrontendConnection with FrontendNetworkControl

object FrontendNetworkControlConnection {
  def apply(
    host: String,
    port: Int = FrontendConnection.DefaultPort,
    timeout: Int = FrontendConnection.DefaultTimeout
  ): FrontendNetworkControlConnection = {
    new FrontendNetworkControlConnectionImpl(host, port, timeout)
  }
}

private class FrontendNetworkControlConnectionImpl(host: String, port: Int, timeout: Int)
  extends FrontendConnectionImpl(host, port, timeout)
     with FrontendNetworkControlConnection
     with FrontendNetworkControlLike
