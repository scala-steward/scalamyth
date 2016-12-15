package mythtv
package connection
package http

import spray.json.JsValue

package object json {
  implicit class JsonValueResultNode(val value: JsValue) extends AnyVal with ServiceResultNode
}
