package mythtv
package connection
package http

abstract class BackendServiceConnection(protocol: String, host: String, port: Int)
    extends AbstractHttpConnection(protocol, host, port)

object BackendServiceConnection {
  final val DefaultPort: Int = 6544
  final val DefaultProtocol: String = "http"
}
