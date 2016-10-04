package mythtv
package util

// TODO can't extend value classes, to provide defaults for units,
//       with e.g. StorageByteCount vs MemoryByteCount. Other options?
//       (or better, BinaryByteCount vs DecimalByteCount, w/above as aliases ?)
//     "true" is a sort of unsatisfactory discrimiator for behavior
//   use implicit conversions between classes

// TODO split into Trait, then new classes BinaryByteCount, DecimalByteCount, with some
//      implicit or other conversion methods

case class ByteCount(bytes: Long) extends AnyVal {
  def b: Long = bytes
  def kb: Long = kilobytes(false)
  def mb: Long = megabytes(false)
  def gb: Long = gigabytes(false)
  def tb: Long = terabytes(false)

  def kb(si: Boolean): Long = kilobytes(si)
  def mb(si: Boolean): Long = megabytes(si)
  def gb(si: Boolean): Long = gigabytes(si)
  def tb(si: Boolean): Long = terabytes(si)

  def kilobytes: Long = kilobytes(false)
  def megabytes: Long = megabytes(false)
  def gigabytes: Long = gigabytes(false)
  def terabytes: Long = terabytes(false)

  def kilobytes(si: Boolean): Long = if (si) bytes / 1000 else bytes >> 10
  def megabytes(si: Boolean): Long = if (si) bytes / (1000 * 1000) else bytes >> 20
  def gigabytes(si: Boolean): Long = if (si) bytes / (1000 * 1000 * 1000) else bytes >> 30
  def terabytes(si: Boolean): Long = if (si) bytes / (1000 * 1000 * 1000 * 1000) else bytes >> 40

  def + (that: ByteCount): ByteCount = ByteCount(bytes + that.bytes)
  def - (that: ByteCount): ByteCount = ByteCount(bytes - that.bytes)
  def * (that: ByteCount): ByteCount = ByteCount(bytes * that.bytes)
  def / (that: ByteCount): ByteCount = ByteCount(bytes / that.bytes)

  // TODO maybe move to a formatter class to allow for static fields?
  def toString(si: Boolean): String = {
    val binUnits = Array("B", "KiB", "MiB", "GiB", "TiB")
    val decUnits = Array("B", "kB", "MB", "GB", "TB")

    val binThresholds = Array(0, 1024, 1048576, 1073741824, 1099511627776L)
    val decThresholds = Array(0, 1000, 1000000, 1000000000, 1000000000000L)

    val units = if (si) decUnits else binUnits
    val thresholds = if (si) decThresholds else binThresholds

    val pairs = (units zip thresholds).reverse
    val chosen = pairs find { case (u, t) => t <= bytes }
    val r = chosen map {
      case (u, t) if t == 0 => bytes + " " + u
      case (u, t) => (bytes / t.toDouble) + " " + u  // TODO control precision of output
    }
    r getOrElse bytes.toString
  }

  override def toString(): String = toString(false)
}
