package mythtv
package connection
package http

import java.io.InputStream

trait HttpResponse {
  def statusCode: Int
  def headers: HttpHeaders
}

case class HttpStreamResponse(
  statusCode: Int,
  headers: HttpHeaders,
  stream: InputStream
) extends HttpResponse
