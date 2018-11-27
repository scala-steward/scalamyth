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
