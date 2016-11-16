package mythtv
package connection
package myth
package data

import model.{ RecordedMarkup, VideoPositionFrame }
import model.EnumTypes.Markup

private[myth] case class BackendRecordedMarkup(
  tag: Markup,
  position: VideoPositionFrame
) extends RecordedMarkup
