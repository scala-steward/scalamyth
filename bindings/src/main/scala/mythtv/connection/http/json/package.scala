// SPDX-License-Identifier: LGPL-2.1-only
/*
 * package.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package http

import spray.json.JsValue

package object json {
  implicit class JsonValueResultNode(val value: JsValue) extends AnyVal with ServiceResultNode
}
