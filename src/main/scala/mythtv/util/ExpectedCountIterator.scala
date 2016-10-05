package mythtv
package util

import scala.collection.AbstractIterator

// TODO potentitally add a 'remainingCount' state, which would also be included in toString()
// TODO override hasDefiniteSize ? how about length ?
class ExpectedCountIterator[+A](val expectedCount: Int, underlying: Iterator[A])
    extends AbstractIterator[A] {
  override def hasNext: Boolean = underlying.hasNext
  override def next(): A = underlying.next()
  override def toString: String = {
    val items = if (expectedCount == 1) "item" else "items"
    underlying.toString + s" of $expectedCount $items"
  }
}
