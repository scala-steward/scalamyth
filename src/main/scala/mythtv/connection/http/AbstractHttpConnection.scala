package mythtv
package connection
package http

import java.io.InputStream
import java.net.{ HttpURLConnection, URL, URLEncoder }

import scala.collection.JavaConverters._

case class StreamHttpResponse(statusCode: Int, headers: HttpHeaders, stream: InputStream) extends HttpResponse

// TODO make a scala version of the header fields map, but do so lazily so not to incur cost
//      overhead when we never look at headers...

// TODO need somewhere to clean up our resource(s): connection, stream?

abstract class AbstractHttpConnection(val protocol: String, val host: String, val port: Int)
    extends NetworkConnection {

  def setupConnection(conn: HttpURLConnection): Unit

  def request(path: String): HttpResponse = {
    val url = new URL(protocol, host, port, path)
    url.openConnection() match {
      case conn: HttpURLConnection =>
        setupConnection(conn)
        val stream = conn.getInputStream
        StreamHttpResponse(conn.getResponseCode, conn.getHeaderFields.asScala.toMap, stream)
    }
  }

object AbstractHttpConnection {
  def encodeParameters(params: Map[String, Any], builder: StringBuilder): StringBuilder = {
    if (params.isEmpty) builder
    else params.iterator.map {
      case (k, v) => k + "=" + URLEncoder.encode(v.toString, "UTF-8")
    }.addString(builder, "&")
  }
}
