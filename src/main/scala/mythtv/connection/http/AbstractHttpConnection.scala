package mythtv
package connection
package http

import java.io.OutputStreamWriter
import java.net.{ HttpURLConnection, URL, URLEncoder }

import scala.collection.JavaConverters._

// TODO make a scala version of the header fields map, but do so lazily so not to incur cost
//      overhead when we never look at headers...

// TODO need somewhere to clean up our resource(s): connection, stream?

abstract class AbstractHttpConnection(val protocol: String, val host: String, val port: Int)
    extends NetworkConnection {

  import AbstractHttpConnection._

  def setupConnection(conn: HttpURLConnection): Unit

  def request(path: String): HttpResponse = {
    val url = new URL(protocol, host, port, path)
    url.openConnection() match {
      case conn: HttpURLConnection =>
        setupConnection(conn)
        val stream = conn.getInputStream  // TODO this may fail with java.net.UnknownHostException
        HttpStreamResponse(conn.getResponseCode, conn.getHeaderFields.asScala.toMap, stream)
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
        println("Data to post: " + data)   // TODO buffer the output writer?
        val out = conn.getOutputStream
        val writer = new OutputStreamWriter(out)
        writer.write(data)
        writer.close()

        // TODO check response code?
        val stream = conn.getInputStream
        HttpStreamResponse(conn.getResponseCode, conn.getHeaderFields.asScala.toMap, stream)
    }
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
