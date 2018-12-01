// SPDX-License-Identifier: LGPL-2.1-only
/*
 * FrontendNetworkControlConnection.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth

trait FrontendNetworkControlConnection extends FrontendConnection with FrontendNetworkControl

object FrontendNetworkControlConnection {
  def apply(
    host: String,
    port: Int = FrontendConnection.DefaultPort,
    timeout: Int = FrontendConnection.DefaultTimeout
  ): FrontendNetworkControlConnection = {
    new FrontendNetworkControlConnectionImpl(host, port, timeout)
  }
}

private class FrontendNetworkControlConnectionImpl(host: String, port: Int, timeout: Int)
  extends FrontendConnectionImpl(host, port, timeout)
     with FrontendNetworkControlConnection
     with FrontendNetworkControlLike
