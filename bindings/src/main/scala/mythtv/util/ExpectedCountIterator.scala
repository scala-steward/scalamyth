// SPDX-License-Identifier: LGPL-2.1-only
/*
 * ExpectedCountIterator.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package util

import scala.collection.AbstractIterator

class ExpectedCountIterator[+A] private[mythtv](val expectedCount: Int, underlying: Iterator[A])
    extends AbstractIterator[A] {
  private[this] var remainingCount: Int = expectedCount

  def remaining: Int = remainingCount
  override def hasNext: Boolean = underlying.hasNext
  override def next(): A = { if (remainingCount > 0) remainingCount -= 1; underlying.next() }

  override def toString: String = {
    val items = if (expectedCount == 1) "item" else "items"
    val remain = if (remainingCount > 0) s" ($remainingCount remain)" else ""
    underlying.toString + s" of $expectedCount $items" + remain
  }
}

object ExpectedCountIterator {
  val empty: ExpectedCountIterator[Nothing] = new ExpectedCountIterator[Nothing](0, Iterator.empty)
}
