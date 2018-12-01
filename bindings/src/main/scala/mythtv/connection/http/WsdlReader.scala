// SPDX-License-Identifier: LGPL-2.1-only
/*
 * WsdlReader.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package http

import java.net.URL

import scala.xml.{ Node, XML }

import services.ServiceEndpoint

trait WsdlReader {
  def endpoints: Seq[ServiceEndpoint]
}

object WsdlReader {
  /*
   * WSDL namespace values:
   *   WSDL 1.1:  http://schemas.xmlsoap.org/wsdl/
   *   WSDL 2.0:  http://www.w3.org/ns/wsdl
   */
  final val WsdlNamespace11 = "http://schemas.xmlsoap.org/wsdl/"
  final val WsdlNamespace20 = "http://www.w3.org/ns/wsdl"

  def apply(url: URL): WsdlReader = apply(XML.load(url))

  def apply(xmlRoot: Node): WsdlReader = xmlRoot.namespace match {
    case WsdlNamespace11 => new WsdlReader11(xmlRoot)
    case WsdlNamespace20 => throw new NotImplementedError("parsing WSDL 2.0 is not implemented")
    case x  => throw new RuntimeException("unknown WSDL version/namespace " + x)
  }
}

private class WsdlReader11(root: Node) extends WsdlReader {
  private type OperationDef = (String, String, String, HttpRequestMethod)

  def endpoints: Seq[ServiceEndpoint] = {
    val types = (root \ "types" \ "schema" \ "element").map(extractType).toMap
    val messages = (root \ "message").map(extractMessage).toMap
    val operations = (root \ "portType" \ "operation") map extractOperation

    def operationToEndpoint(op: OperationDef): ServiceEndpoint = op match {
      case (nm, out, in, meth) =>
        val result = types(messages(out)).values.head
        val params = types(messages(in))
        new ServiceEndpoint {
          def name = nm
          def resultType = result
          def parameters = params
          def requestMethod = meth
        }
    }

    operations map operationToEndpoint
  }

  // discards any namespace prefix on a string
  private def dropPrefix(s: String): String = s.substring(s.indexOf(':') + 1)

  private def extractType(n: Node): (String, Map[String, String]) =
    (n \@ "name", extractComplexType(n))

  private def extractMessage(n: Node): (String, String) =
    (n \@ "name", dropPrefix(n \ "part" \@ "element"))

  private def extractOperation(n: Node): OperationDef = (
    n \@ "name",
    dropPrefix(n \ "output" \@ "message"),
    dropPrefix(n \ "input" \@ "message"),
    requestMethod((n \ "documentation").text.trim)
  )

  private def extractComplexType(n: Node): Map[String, String] = {
    for {
      elem <- n \\ "complexType" \ "sequence" \ "element"
    } yield (elem \@ "name", elem \@ "type" )
  }.toMap

  private def requestMethod(meth: String): HttpRequestMethod = meth match {
    case "GET" => HttpRequestMethod.Get
    case "POST" => HttpRequestMethod.Post
    case x => throw new RuntimeException("unsupported HTTP request method: " + x)
  }
}
