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
