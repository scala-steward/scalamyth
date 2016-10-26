package mythtv
package connection
package http

import java.net.HttpURLConnection

import scala.xml.XML

case class XMLResponse(statusCode: Int, headers: HttpHeaders, root: xml.Elem) extends HttpResponse

/*abstract*/ class XMLConnection(protocol: String, host: String, port: Int)
    extends AbstractHttpConnection(protocol, host, port) {

  override def setupConnection(conn: HttpURLConnection): Unit = {
    conn.setRequestProperty("Accept", "text/xml, application/xml")
  }

  override def request(path: String): XMLResponse = super.request(path) match {
    case StreamHttpResponse(status, headers, stream) =>
      XMLResponse(status, headers, XML.load(stream))
  }
}

abstract class BackendXMLConnection(host: String, port: Int) extends XMLConnection("http", host, port)
    with BackendServiceOperations {
  def hosts: List[String] = ???
  def keys: List[String] = ???
  def setting(key: String, hostname: Option[String] = None, default: Option[String] = None) = ???
}
