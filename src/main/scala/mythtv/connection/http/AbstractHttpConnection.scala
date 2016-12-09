package mythtv
package connection
package http

import java.io.OutputStreamWriter
import java.net.{ HttpURLConnection, URL, URLEncoder }
import java.util.{ List => JList }

import scala.collection.JavaConverters._

// TODO need somewhere to clean up our resource(s): stream(s)?

abstract class AbstractHttpConnection(val protocol: String, val host: String, val port: Int)
    extends NetworkConnection {

  import AbstractHttpConnection._

  def setupConnection(conn: HttpURLConnection): Unit = {
    // TODO need a centralized mechanism for specifying default HTTP connect and read timeouts
    conn.setConnectTimeout(10000)
    conn.setReadTimeout(10000)
  }

  // TODO do we need to pick up errorStream and read it out?
  def request(path: String): HttpResponse = {
    val url = new URL(protocol, host, port, path)
    //println("Requesting: " + url)
    url.openConnection() match {
      case conn: HttpURLConnection =>
        setupConnection(conn)
        response(url, conn)
    }
  }

  def post(path: String, params: Map[String, Any]): HttpResponse = {
    val url = new URL(protocol, host, port, path)
    url.openConnection() match {
      case conn: HttpURLConnection =>
        setupConnection(conn)

        conn.setDoOutput(true)
        conn.setRequestMethod("POST")
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

        val data = encodeParameters(params, new StringBuilder).toString
        println("Data to post: " + data)

        val writer = new OutputStreamWriter(conn.getOutputStream)
        writer.write(data)
        writer.close()

        response(url, conn)
    }
  }

  private def getHeaders(conn: HttpURLConnection)(): Map[String, JList[String]] = {
    conn.getHeaderFields.asScala.toMap
  }

  private def response(url: URL, conn: HttpURLConnection): HttpStreamResponse = {
    val code = conn.getResponseCode
    if (code >= 400) throw HttpResponseException(url, code)

    val stream = conn.getInputStream
    HttpStreamResponse(code, getHeaders(conn) _, stream)
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
