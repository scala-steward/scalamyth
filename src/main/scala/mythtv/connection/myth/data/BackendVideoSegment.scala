package mythtv
package connection
package myth
package data

import model.{ VideoPosition, VideoSegment }

private[myth] case class BackendVideoSegment(
  start: VideoPosition,
  end: VideoPosition
) extends VideoSegment {

  override def toString: String = productPrefix + "(" + super.toString + ")"
}
