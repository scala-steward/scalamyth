package mythtv
package util

import java.util.Base64

class Base64String(val str: String) extends AnyVal {
  def decode: Array[Byte] = Base64.getDecoder.decode(str)
  override def toString: String = str
}

object Base64String {
  def apply(data: Array[Byte]): Base64String =
    new Base64String(Base64.getEncoder.encodeToString(data))
}
