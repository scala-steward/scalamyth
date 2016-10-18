package mythtv
package connection
package myth
package data

trait GenericBackendObject {
  def apply(fieldName: String): String
}

trait GenericBackendObjectFactory[+A] {
  def FIELD_ORDER: IndexedSeq[String]
  def apply(data: Seq[String]): A
}
