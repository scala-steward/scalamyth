package mythtv
package connection
package http

import java.net.HttpURLConnection

import scala.xml.XML

case class XmlResponse(statusCode: Int, headers: HttpHeaders, root: xml.Elem) extends HttpResponse

/*abstract*/ class XmlConnection(protocol: String, host: String, port: Int)
    extends AbstractHttpConnection(protocol, host, port) {

  override def setupConnection(conn: HttpURLConnection): Unit = {
    conn.setRequestProperty("Accept", "text/xml, application/xml")
  }

  override def request(path: String): XmlResponse = super.request(path) match {
    case StreamHttpResponse(status, headers, stream) =>
      XmlResponse(status, headers, XML.load(stream))
  }
}

abstract class BackendXmlConnection(host: String, port: Int) extends XmlConnection("http", host, port)
