package mythtv
package connection
package http

import java.io.{ BufferedReader, InputStream, InputStreamReader, StringWriter }
import java.net.HttpURLConnection

import spray.json.{ JsonParser, JsValue }

case class JSONResponse(statusCode: Int, headers: HttpHeaders, json: JsValue) extends HttpResponse

/* abstract */ class JSONConnection(protocol: String, host: String, port: Int)
    extends AbstractHttpConnection(protocol, host, port) {

  override def setupConnection(conn: HttpURLConnection): Unit = {
    conn.setRequestProperty("Accept", "application/json")
  }

  override def request(path: String): JSONResponse = super.request(path) match {
    case StreamHttpResponse(status, headers, stream) =>
      val json = parseJson(stream)
      JSONResponse(status, headers, json)
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

/*abstract*/ class BackendJSONConnection(host: String, port: Int) extends JSONConnection("http", host, port)
    with BackendServiceOperations {
  def hosts: List[String] = ???
  def keys: List[String] = ???
  def setting(key: String, hostname: Option[String] = None, default: Option[String] = None) = ???
}
