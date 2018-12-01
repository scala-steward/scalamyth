// SPDX-License-Identifier: LGPL-2.1-only
/*
 * FileTransfer.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth

import model.IntegerIdentifier

final case class FileTransferId(id: Int) extends AnyVal with IntegerIdentifier

trait FileTransfer {
  def fileSize: Long
  def fileName: String
  def storageGroup: String
}
