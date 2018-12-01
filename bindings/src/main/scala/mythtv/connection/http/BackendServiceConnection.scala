// SPDX-License-Identifier: LGPL-2.1-only
/*
 * BackendServiceConnection.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package http

abstract class BackendServiceConnection(protocol: String, host: String, port: Int)
    extends AbstractHttpConnection(protocol, host, port)

object BackendServiceConnection {
  final val DefaultPort: Int = 6544
  final val DefaultProtocol: String = "http"
}
