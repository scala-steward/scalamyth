// SPDX-License-Identifier: LGPL-2.1-only
/*
 * package.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection

import java.util.{ List => JList }

package object http {
  type HttpHeaders = () => Map[String, JList[String]]
}
