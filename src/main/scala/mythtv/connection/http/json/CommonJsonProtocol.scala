package mythtv
package connection
package http
package json

import java.time.{ DayOfWeek, Instant, ZoneOffset }

import spray.json.{ JsonFormat, deserializationError }
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

  implicit object OptionalDayOfWeekFormat extends JsonFormat[Option[DayOfWeek]] {
    // This maps None <-> -1
    //   - consistent with default template rule (has -1)
    //   - inconsistent with RecordingRule::IsValid (expects 0..6 only...sigh)
    //  If we map None -> 0 then it clashes with SATURDAY

    def write(dow: Option[DayOfWeek]): JsValue = dow match {
      case None    => JsString("-1")
      case Some(d) => JsString((d.getValue + 1 % 7).toString)
    }

    def read(value: JsValue): Option[DayOfWeek] = {
      val d = value match {
        case JsString(s) => s.toInt
        case x => x.toString.toInt
      }
      if (d < 0) None
      else       Some(DayOfWeek.of((d + 5) % 7 + 1))
    }
  }

}
