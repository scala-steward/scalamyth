package mythtv
package connection
package http

abstract class JsonBackendService(conn: BackendJsonConnection)
  extends JsonService(conn)
     with BackendServiceProtocol
     with BackendJsonProtocol
