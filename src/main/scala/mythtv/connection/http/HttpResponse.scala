package mythtv
package connection
package http

import java.io.InputStream
import java.net.URL

trait HttpResponse {
  def statusCode: Int
  def headers: HttpHeaders
}

case class HttpStreamResponse(
  statusCode: Int,
  headers: HttpHeaders,
  stream: InputStream
) extends HttpResponse

case class HttpResponseException(
  url: URL,
  responseCode: Int
) extends RuntimeException(responseCode + " response from " + url)
