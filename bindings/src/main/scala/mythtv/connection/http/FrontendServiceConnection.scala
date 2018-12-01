// SPDX-License-Identifier: LGPL-2.1-only
/*
 * FrontendServiceConnection.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package http

abstract class FrontendServiceConnection(protocol: String, host: String, port: Int)
    extends AbstractHttpConnection(protocol, host, port)

object FrontendServiceConnection {
  final val DefaultPort: Int = 6547
  final val DefaultProtocol: String = "http"
}
