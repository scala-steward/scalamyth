// SPDX-License-Identifier: LGPL-2.1-only
/*
 * BackendRecordedMarkup.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth
package data

import model.{ RecordedMarkupFrame, VideoPositionFrame }
import model.EnumTypes.Markup

private[myth] case class BackendRecordedMarkup(
  tag: Markup,
  position: VideoPositionFrame
) extends RecordedMarkupFrame
