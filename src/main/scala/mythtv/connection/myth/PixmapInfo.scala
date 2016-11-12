package mythtv
package connection
package myth

import util.{ ByteCount, Crc16 }

case class PixmapInfo(fileSize: ByteCount, crc16: Crc16, base64data: String)
