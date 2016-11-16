package mythtv
package connection
package myth
package data

import model.{ VideoPositionFrame, VideoSegment }

private[myth] case class BackendVideoSegment(
  start: VideoPositionFrame,
  end: VideoPositionFrame
) extends VideoSegment {

  override def toString: String = productPrefix + "(" + super.toString + ")"
}
