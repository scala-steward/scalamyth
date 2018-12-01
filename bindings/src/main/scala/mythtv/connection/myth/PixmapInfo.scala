// SPDX-License-Identifier: LGPL-2.1-only
/*
 * PixmapInfo.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth

import util.{ Base64String, ByteCount, Crc16 }

case class PixmapInfo(fileSize: ByteCount, crc16: Crc16, base64data: Base64String)
