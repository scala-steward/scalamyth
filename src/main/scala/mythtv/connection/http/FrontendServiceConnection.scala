package mythtv
package connection
package http

abstract class FrontendServiceConnection(protocol: String, host: String, port: Int)
    extends AbstractHttpConnection(protocol, host, port)

object FrontendServiceConnection {
  final val DefaultPort: Int = 6547
  final val DefaultProtocol: String = "http"
}
