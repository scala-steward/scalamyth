package mythtv
package util

import scala.collection.AbstractIterator

class ExpectedCountIterator[A](val expectedCount: Int, underlying: Iterator[A])
    extends AbstractIterator[A] {
  override def hasNext: Boolean = underlying.hasNext
  override def next(): A = underlying.next()
}
