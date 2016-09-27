package mythtv
package connection
package http

trait HttpResponse {
  def statusCode: Int
  def headers: HttpHeaders
}
