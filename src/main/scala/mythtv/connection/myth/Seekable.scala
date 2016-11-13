package mythtv
package connection
package myth

import EnumTypes.SeekWhence

object SeekWhence extends Enumeration {
  type SeekWhence = Value
  val Begin      = Value(0)
  val Current    = Value(1)
  val End        = Value(2)
}

trait Seekable {
  def rewind(): Unit
  def seek(offset: Long, whence: SeekWhence): Unit
  def tell: Long
}
