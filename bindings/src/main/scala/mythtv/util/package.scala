// SPDX-License-Identifier: LGPL-2.1-only
/*
 * package.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv

package object util {
  // Used to indicate serialization format for certain MythProtocol commands
  private[mythtv] implicit final class MythDateTimeString(mythDateTime: MythDateTime) {
    override def toString: String = mythDateTime.mythformat
    def toMythDateTime: MythDateTime = mythDateTime
  }
}
