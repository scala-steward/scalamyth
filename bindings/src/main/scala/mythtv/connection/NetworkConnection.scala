// SPDX-License-Identifier: LGPL-2.1-only
/*
 * NetworkConnection.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection

trait NetworkConnection {
  def host: String
  def port: Int
}
