// SPDX-License-Identifier: LGPL-2.1-only
/*
 * BackendVideoSegment.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth
package data

import model.{ VideoPositionFrame, VideoSegmentFrames }

private[myth] case class BackendVideoSegment(
  start: VideoPositionFrame,
  end: VideoPositionFrame
) extends VideoSegmentFrames {

  override def toString: String = productPrefix + "(" + super.toString + ")"
}
