// SPDX-License-Identifier: LGPL-2.1-only
/*
 * BackendTuner.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth
package data

import model.{ CaptureCardId, Tuner }

private[myth] case class BackendTuner(
  cardId: CaptureCardId,
  videoDevice: Option[String],
  audioDevice: Option[String],
  vbiDevice: Option[String]
) extends Tuner
