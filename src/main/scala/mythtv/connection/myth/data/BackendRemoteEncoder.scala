package mythtv
package connection
package myth
package data

import model.{ CaptureCardId, RemoteEncoder }

private[myth] case class BackendRemoteEncoder(
  cardId: CaptureCardId,
  host: String,
  port: Int
) extends RemoteEncoder
