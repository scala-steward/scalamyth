package mythtv
package connection
package myth

import EnumTypes.SeekWhence

object SeekWhence extends Enumeration {
  type SeekWhence = Value
  final val Begin      = Value(0)
  final val Current    = Value(1)
  final val End        = Value(2)
}

trait Seekable {
  def rewind(): Unit
  def seek(offset: Long, whence: SeekWhence): Unit
  def tell: Long
}
