package mythtv
package connection
package myth

import util.ByteCount

case class PixmapInfo(fileSize: ByteCount, crc16: Int, base64data: String)
