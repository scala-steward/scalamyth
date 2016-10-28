package mythtv
package services

trait PagedList[+A] extends Iterable[A] {
  def items: List[A]
  def count: Int
  def totalAvailable: Int
  def startIndex: Int

  def iterator: Iterator[A] = items.iterator
  override def toString: String = s"<PagedList $count/$totalAvailable @ $startIndex>"
}
