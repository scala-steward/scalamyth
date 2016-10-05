package mythtv
package connection
package myth
package data

import model.{ RecordedMarkup, VideoPosition }
import model.EnumTypes.Markup

private[myth] case class BackendRecordedMarkup(
  tag: Markup,
  position: VideoPosition
) extends RecordedMarkup
