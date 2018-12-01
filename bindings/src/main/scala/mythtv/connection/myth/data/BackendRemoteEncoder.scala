// SPDX-License-Identifier: LGPL-2.1-only
/*
 * BackendRemoteEncoder.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth
package data

import model.{ CaptureCardId, RemoteEncoder }

private[myth] case class BackendRemoteEncoder(
  cardId: CaptureCardId,
  host: String,
  port: Int
) extends RemoteEncoder
