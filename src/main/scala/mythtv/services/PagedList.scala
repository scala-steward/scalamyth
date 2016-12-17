package mythtv
package services

trait PagedList[+A] extends Iterable[A] {
  def items: List[A]
  def count: Int
  def totalAvailable: Int
  def startIndex: Int

  def iterator: Iterator[A] = items.iterator
  override def toString: String = s"<PagedList $count/$totalAvailable @ $startIndex>"

  // Override map, filter, filterNot so they return PagedList[_] rather than Iterable[_]

  def map[B](f: (A) => B): PagedList[B] = {
    val newItems = items map f
    val (n, avail, index) = (count, totalAvailable, startIndex)
    new PagedList[B] {   // avoid closing over old instance
      def items          = newItems
      def count          = n
      def totalAvailable = avail
      def startIndex     = index
    }
  }

  // Note that filter does not affect the 'count' and 'totalAvailable' fields

  private def plFilter(p: (A) => Boolean, isNegated: Boolean): PagedList[A] = {
    val newItems = if (isNegated) items filterNot p else items filter p
    val (n, avail, index) = (count, totalAvailable, startIndex)
    new PagedList[A] {   // avoid closing over old instance
      def items          = newItems
      def count          = n
      def totalAvailable = avail
      def startIndex     = index
    }
  }

  override def filter(p: (A) => Boolean): PagedList[A] = plFilter(p, isNegated = false)

  override def filterNot(p: (A) => Boolean): PagedList[A] = plFilter(p, isNegated = true)
}
