package mythtv
package connection
package http
package json

abstract class JsonBackendService(conn: BackendJsonConnection)
  extends JsonService(conn)
     with BackendServiceProtocol
     with BackendJsonProtocol
