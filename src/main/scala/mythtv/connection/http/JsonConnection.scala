package mythtv
package connection
package http

import java.io.{ BufferedReader, InputStream, InputStreamReader, StringWriter }
import java.net.HttpURLConnection

import spray.json.{ JsonParser, JsValue }

case class JsonResponse(statusCode: Int, headers: HttpHeaders, json: JsValue) extends HttpResponse

trait JsonConnection extends AbstractHttpConnection {
  override def setupConnection(conn: HttpURLConnection): Unit = {
    conn.setRequestProperty("Accept", "application/json")
  }

  override def request(path: String): JsonResponse = super.request(path) match {
    case StreamHttpResponse(status, headers, stream) =>
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

    JsonParser(writer.toString)
  }
}

class BackendJsonConnection(protocol: String, host: String, port: Int)
    extends BackendServiceConnection(protocol, host, port)
    with JsonConnection {
  def this(host: String, port: Int) = this(BackendServiceConnection.DefaultProtocol, host, port)
  def this(host: String) = this(BackendServiceConnection.DefaultProtocol, host, BackendServiceConnection.DefaultPort)
}
