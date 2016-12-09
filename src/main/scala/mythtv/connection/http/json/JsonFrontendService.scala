package mythtv
package connection
package http
package json

abstract class JsonFrontendService(conn: FrontendJsonConnection)
  extends JsonService(conn)
     with FrontendServiceProtocol
     with FrontendJsonProtocol
