package mythtv
package util

trait BitwiseOperable[@specialized(Int,Long) T] {
  def bitwise_and(x: T, y: T): T
  def bitwise_or(x: T, y: T): T
  def bitwise_xor(x: T, y: T): T
  def bitwise_neg(x: T): T
  def bitwise_shl(x: T, n: Int): T
  def bitwise_shr(x: T, n: Int): T
  def bitwise_rol(x: T, n: Int): T
  def bitwise_ror(x: T, n: Int): T
}

trait BitCountable[@specialized(Int,Long) T] {
  def bitCount(x: T): Int
  def numberOfLeadingZeros(x: T): Int
  def numberOfTrailingZeros(x: T): Int
}

// TODO what to call this?
trait BitWise[@specialized(Int, Long) T] extends BitwiseOperable[T] with BitCountable[T] {
  def toHexString(x: T): String
  def unbox(x: AnyRef): T
  def lt(x: T, y: T): Boolean
  def zero: T
  def one: T
}

object BitWise {
  trait IntIsBitwiseOperable extends BitwiseOperable[Int] {
    def bitwise_and(x: Int, y: Int): Int = x & y
    def bitwise_or(x: Int, y: Int): Int = x | y
    def bitwise_xor(x: Int, y: Int): Int = x ^ y
    def bitwise_neg(x: Int): Int = ~x
    def bitwise_shl(x: Int, n: Int): Int = x << n
    def bitwise_shr(x: Int, n: Int): Int = x >>> n
    def bitwise_rol(x: Int, n: Int): Int = java.lang.Integer.rotateLeft(x, n)
    def bitwise_ror(x: Int, n: Int): Int = java.lang.Integer.rotateRight(x, n)
  }

  trait LongIsBitwiseOperable extends BitwiseOperable[Long] {
    def bitwise_and(x: Long, y: Long): Long = x & y
    def bitwise_or(x: Long, y: Long): Long = x | y
    def bitwise_xor(x: Long, y: Long): Long = x ^ y
    def bitwise_neg(x: Long): Long = ~x
    def bitwise_shl(x: Long, n: Int): Long = x << n
    def bitwise_shr(x: Long, n: Int): Long = x >>> n
    def bitwise_rol(x: Long, n: Int): Long = java.lang.Long.rotateLeft(x, n)
    def bitwise_ror(x: Long, n: Int): Long = java.lang.Long.rotateRight(x, n)
  }

  trait IntIsBitCountable extends BitCountable[Int] {
    def bitCount(x: Int): Int = java.lang.Integer.bitCount(x)
    def numberOfLeadingZeros(x: Int): Int = java.lang.Integer.numberOfLeadingZeros(x)
    def numberOfTrailingZeros(x: Int): Int = java.lang.Integer.numberOfTrailingZeros(x)
  }

  trait LongIsBitCountable extends BitCountable[Long] {
    def bitCount(x: Long): Int = java.lang.Long.bitCount(x)
    def numberOfLeadingZeros(x: Long): Int = java.lang.Long.numberOfLeadingZeros(x)
    def numberOfTrailingZeros(x: Long): Int = java.lang.Long.numberOfTrailingZeros(x)
  }

  trait IntIsBitWise extends BitWise[Int] with IntIsBitCountable with IntIsBitwiseOperable {
    def toHexString(x: Int): String = x.toHexString
    def unbox(x: AnyRef): Int = Int.unbox(x)
    def lt(x: Int, y: Int): Boolean = x < y
    def zero = 0
    def one = 1
  }

  trait LongIsBitWise extends BitWise[Long] with LongIsBitCountable with LongIsBitwiseOperable {
    def toHexString(x: Long): String = x.toHexString
    def unbox(x: AnyRef): Long = Long.unbox(x)
    def lt(x: Long, y: Long): Boolean = x < y
    def zero = 0
    def one = 1
  }

  implicit object IntIsBitWise extends IntIsBitWise
  implicit object LongIsBitWise extends LongIsBitWise

  // implicit class can't be defined at top level
  implicit class BitwiseOps[@specialized(Int,Long) T](lhs: T)(implicit ev: BitWise[T]) {
    def & (rhs: T): T = ev.bitwise_and(lhs, rhs)
    def | (rhs: T): T = ev.bitwise_or(lhs, rhs)
    def ^ (rhs: T): T = ev.bitwise_xor(lhs, rhs)
    def unary_~ : T = ev.bitwise_neg(lhs)
    def << (n: Int): T = ev.bitwise_shl(lhs, n)
    def >> (n: Int): T = ev.bitwise_shr(lhs, n)
    def @<< (n: Int): T = ev.bitwise_rol(lhs, n)
    def @>> (n: Int): T = ev.bitwise_ror(lhs, n)
    def < (rhs: T): Boolean = ev.lt(lhs, rhs)
    def toHexString: String = ev.toHexString(lhs)
  }
}
