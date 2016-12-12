package mythtv
package connection
package http
package json

import java.time.{ Instant, ZoneOffset }

import spray.json.{ DefaultJsonProtocol, JsonFormat, RootJsonFormat, deserializationError, jsonWriter }
import spray.json.{ JsArray, JsObject, JsString, JsValue }

private[http] trait BaseMythJsonListFormat[T] {
  def listFieldName: String

  def convertElement(value: JsValue): T
  def elementToJson(elem: T): JsValue

  def writeItems(list: List[T]): JsValue =
    JsArray(list.map(elementToJson).toVector)

  def readItems(obj: JsObject): List[T] = {
    if (!(obj.fields contains listFieldName))
      deserializationError(s"expected to find field name $listFieldName")

    val itemList: List[T] = obj.fields(listFieldName) match {
      case JsArray(elements) => elements.map(convertElement)(scala.collection.breakOut)
      case x => deserializationError(s"expected array in $listFieldName but got $x")
    }
    itemList
  }
}

private[http] trait CommonJsonProtocol {

  trait BasicListFormat[T] extends RootJsonFormat[List[T]] {  // TODO remove this and replace with DefaultProtocol listFormat? (implicit def)
    def convertElement(value: JsValue): T
    def elementToJson(elem: T): JsValue

    def write(list: List[T]): JsValue =
      JsArray(list.map(elementToJson).toVector)

    def read(value: JsValue): List[T] = value match {
      case JsArray(elements) => elements.map(convertElement)(scala.collection.breakOut)
      case x => deserializationError(s"expected array but got $x")
    }
  }

  implicit object StringListJsonFormat extends BasicListFormat[String] {
    import DefaultJsonProtocol.StringJsonFormat
    //def listFieldName = "StringList"
    def convertElement(value: JsValue): String = value.convertTo[String]
    def elementToJson(elem: String): JsValue = jsonWriter[String].write(elem)
  }

  implicit object StringMapJsonFormat extends JsonFormat[Map[String, String]] {
    def write(m: Map[String, String]): JsValue =
      JsObject(m mapValues (JsString(_)))

    def read(value: JsValue): Map[String, String] = {
      val obj = value.asJsObject
      obj.fields mapValues {
        case JsString(s) => s
        case x => x.toString
      }
    }
  }

  implicit object InstantJsonFormat extends JsonFormat[Instant] {
    def write(x: Instant): JsValue = JsString(x.toString)

    def read(value: JsValue): Instant = {
      val dtString = value match {
        case JsString(s) => s
        case x => x.toString
      }
      Instant.parse(dtString)
    }
  }

  implicit object ZoneOffsetJsonFormat extends JsonFormat[ZoneOffset] {
    def write(z: ZoneOffset): JsValue = JsString(z.getTotalSeconds.toString)

    def read(value: JsValue): ZoneOffset = {
      val secs = value match {
        case JsString(s) => s.toInt
        case x => x.toString.toInt
      }
      ZoneOffset.ofTotalSeconds(secs)
    }
  }

}
