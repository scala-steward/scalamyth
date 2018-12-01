// SPDX-License-Identifier: LGPL-2.1-only
/*
 * AnnouncingConnection.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth

private[myth] trait AnnouncingConnection {
  self: BackendConnection =>

  private[this] var announced = false

  def announce(): Unit
  def hasAnnounced: Boolean = announced

  try {
    announce()
    announced = true
  }
  finally if (!announced) disconnect()
}
