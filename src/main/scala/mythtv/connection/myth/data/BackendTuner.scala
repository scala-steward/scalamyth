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
