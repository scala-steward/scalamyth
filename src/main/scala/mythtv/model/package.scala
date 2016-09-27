package mythtv

package object model {
  type PropertyMap = Map[String, Any]
  type Guide = Iterable[GuideEntry]
  type BitmaskEnum = Enumeration
}
