// SPDX-License-Identifier: LGPL-2.1-only
/*
 * JsonConnection.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package http
package json

import java.io.{ BufferedReader, InputStream, InputStreamReader, StringWriter }
import java.net.HttpURLConnection

import spray.json.{ JsonParser, JsValue }

case class JsonResponse(statusCode: Int, headers: HttpHeaders, json: JsValue) extends HttpResponse

trait JsonConnection extends AbstractHttpConnection {
  override def setupConnection(conn: HttpURLConnection): Unit = {
    super.setupConnection(conn)
    conn.setRequestProperty("Accept", "application/json")
  }

  override def request(path: String): JsonResponse = super.request(path) match {
    case HttpStreamResponse(status, headers, stream) =>
      val json = parseJson(stream)
      JsonResponse(status, headers, json)
  }

  override def post(path: String, params: Map[String, Any]): JsonResponse = super.post(path, params) match {
    case HttpStreamResponse(status, headers, stream) =>
      val json = parseJson(stream)
      JsonResponse(status, headers, json)
  }

  private def parseJson(stream: InputStream): JsValue = {
    val reader = new BufferedReader(new InputStreamReader(stream))
    val writer = new StringWriter()

    var line: String = null
    do {
      line = reader.readLine()
      if (line != null) writer.write(line)
    } while (line != null)

    stream.close()

    JsonParser(writer.toString)
  }
}

class BackendJsonConnection(protocol: String, host: String, port: Int)
  extends BackendServiceConnection(protocol, host, port)
     with JsonConnection {
  def this(host: String, port: Int) = this(BackendServiceConnection.DefaultProtocol, host, port)
  def this(host: String) = this(BackendServiceConnection.DefaultProtocol, host, BackendServiceConnection.DefaultPort)
}

class FrontendJsonConnection(protocol: String, host: String, port: Int)
  extends FrontendServiceConnection(protocol, host, port)
     with JsonConnection {
  def this(host: String, port: Int) = this(FrontendServiceConnection.DefaultProtocol, host, port)
  def this(host: String) = this(FrontendServiceConnection.DefaultProtocol, host, FrontendServiceConnection.DefaultPort)
}
