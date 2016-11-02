package mythtv
package connection
package http
package json

import java.time.{ LocalDate, LocalTime }
import java.time.format.DateTimeParseException

import spray.json.{ JsObject, JsString }

import util.MythDateTime

private trait RichJsonObject extends Any {
  def stringField(fieldName: String): String
  def stringFieldOption(fieldName: String): Option[String]
  def stringFieldOption(fieldName: String, default: String): Option[String]
  def stringFieldOrElse(fieldName: String, default: => String): String

  def booleanField(fieldName: String): Boolean
  def booleanFieldOption(fieldName: String): Option[Boolean]
  def booleanFieldOrElse(fieldName: String, default: => Boolean): Boolean

  def charField(fieldName: String): Char
  def charFieldOption(fieldName: String): Option[Char]
  def charFieldOrElse(fieldName: String, default: => Char): Char

  def dateField(fieldName: String): LocalDate
  def dateFieldOption(fieldName: String): Option[LocalDate]
  def dateFieldOrElse(fieldName: String, default: => LocalDate): LocalDate

  def dateTimeField(fieldName: String): MythDateTime
  def dateTimeFieldOption(fieldName: String): Option[MythDateTime]
  def dateTimeFieldOrElse(fieldName: String, default: => MythDateTime): MythDateTime

  def doubleField(fieldName: String): Double
  def doubleFieldOption(fieldName: String): Option[Double]
  def doubleFieldOption(fieldName: String, default: Double): Option[Double]
  def doubleFieldOrElse(fieldName: String, default: => Double): Double

  def intField(fieldName: String): Int
  def intFieldOption(fieldName: String): Option[Int]
  def intFieldOption(fieldName: String, default: Int): Option[Int]
  def intFieldOrElse(fieldName: String, default: => Int): Int

  def longField(fieldName: String): Long
  def longFieldOption(fieldName: String): Option[Long]
  def longFieldOrElse(fieldName: String, default: => Long): Long

  def timeField(fieldName: String): LocalTime
  def timeFieldOption(fieldName: String): Option[LocalTime]
  def timeFieldOption(fieldName: String, default: LocalTime): Option[LocalTime]
  def timeFieldOrElse(fieldName: String, default: => LocalTime): LocalTime
}

// TODO the NoSuchElementException message is misleading,
//      because it is parent object that does not exist
private object EmptyJsonObject extends RichJsonObject {
  def stringField(fieldName: String) = throw new NoSuchElementException(fieldName)
  def stringFieldOption(fieldName: String) = None
  def stringFieldOption(fieldName: String, default: String): Option[String] = None
  def stringFieldOrElse(fieldName: String, default: => String) = default

  def booleanField(fieldName: String) = throw new NoSuchElementException(fieldName)
  def booleanFieldOption(fieldName: String) = None
  def booleanFieldOrElse(fieldName: String, default: => Boolean) = default

  def charField(fieldName: String) = throw new NoSuchElementException(fieldName)
  def charFieldOption(fieldName: String) = None
  def charFieldOrElse(fieldName: String, default: => Char) = default

  def dateField(fieldName: String) = throw new NoSuchElementException(fieldName)
  def dateFieldOption(fieldName: String) = None
  def dateFieldOrElse(fieldName: String, default: => LocalDate) = default

  def dateTimeField(fieldName: String) = throw new NoSuchElementException(fieldName)
  def dateTimeFieldOption(fieldName: String) = None
  def dateTimeFieldOrElse(fieldName: String, default: => MythDateTime) = default

  def doubleField(fieldName: String) = throw new NoSuchElementException(fieldName)
  def doubleFieldOption(fieldName: String) = None
  def doubleFieldOption(fieldName: String, default: Double) = None
  def doubleFieldOrElse(fieldName: String, default: => Double) = default

  def intField(fieldName: String) = throw new NoSuchElementException(fieldName)
  def intFieldOption(fieldName: String) = None
  def intFieldOption(fieldName: String, default: Int) = None
  def intFieldOrElse(fieldName: String, default: => Int) = default

  def longField(fieldName: String) = throw new NoSuchElementException(fieldName)
  def longFieldOption(fieldName: String) = None
  def longFieldOrElse(fieldName: String, default: => Long) = default

  def timeField(fieldName: String) = throw new NoSuchElementException(fieldName)
  def timeFieldOption(fieldName: String) = None
  def timeFieldOption(fieldName: String, default: LocalTime) = None
  def timeFieldOrElse(fieldName: String, default: => LocalTime) = default
}

private object RichJsonObject {
  implicit class MythJsonObject(val jsObj: JsObject) extends AnyVal with RichJsonObject {

    // Three choices for failure handling:
    //  (a) NoSuchElementException
    //  (b) Option / None
    //  (c) default value

    def stringField(fieldName: String): String = jsObj.fields(fieldName) match {
      case JsString(s) => s
      case x => x.toString
    }

    def stringFieldOption(fieldName: String): Option[String] = jsObj.fields get fieldName map {
      case JsString(s) => s
      case x => x.toString
    }

    def stringFieldOption(fieldName: String, default: String): Option[String] =
      stringFieldOption(fieldName) match {
        case Some(`default`) => None
        case x => x
      }

    def stringFieldOrElse(fieldName: String, default: => String): String =
      stringFieldOption(fieldName).getOrElse(default)

    def booleanField(fieldName: String) = stringField(fieldName).toBoolean
    def booleanFieldOption(fieldName: String) = stringFieldOption(fieldName) map (_.toBoolean)
    def booleanFieldOrElse(fieldName: String, default: => Boolean) = booleanFieldOption(fieldName).getOrElse(default)

    def charField(fieldName: String): Char = stringField(fieldName).charAt(0)
    def charFieldOption(fieldName: String): Option[Char] = stringFieldOption(fieldName, "") map (_.charAt(0))
    def charFieldOrElse(fieldName: String, default: => Char): Char = charFieldOption(fieldName).getOrElse(default)

    def dateField(fieldName: String): LocalDate = LocalDate.parse(stringField(fieldName))
    def dateFieldOption(fieldName: String): Option[LocalDate] =
      try stringFieldOption(fieldName) map LocalDate.parse
      catch { case e: DateTimeParseException => None }
    def dateFieldOrElse(fieldName: String, default: => LocalDate): LocalDate = dateFieldOption(fieldName).getOrElse(default)

    def dateTimeField(fieldName: String): MythDateTime = MythDateTime.fromIso(stringField(fieldName))
    def dateTimeFieldOption(fieldName: String): Option[MythDateTime] =
      try stringFieldOption(fieldName) map MythDateTime.fromIso
      catch { case e: DateTimeParseException => None }
    def dateTimeFieldOrElse(fieldName: String, default: => MythDateTime): MythDateTime =
      dateTimeFieldOption(fieldName).getOrElse(default)

    def doubleField(fieldName: String): Double = stringField(fieldName).toDouble
    def doubleFieldOption(fieldName: String): Option[Double] = stringFieldOption(fieldName) map (_.toDouble)
    def doubleFieldOrElse(fieldName: String, default: => Double): Double = doubleFieldOption(fieldName).getOrElse(default)
    def doubleFieldOption(fieldName: String, default: Double): Option[Double] = doubleFieldOption(fieldName) match {
      case Some(`default`) => None
      case x => x
    }

    def intField(fieldName: String): Int = stringField(fieldName).toInt
    def intFieldOption(fieldName: String): Option[Int] = stringFieldOption(fieldName) map (_.toInt)
    def intFieldOrElse(fieldName: String, default: => Int): Int = intFieldOption(fieldName).getOrElse(default)
    def intFieldOption(fieldName: String, default: Int): Option[Int] = intFieldOption(fieldName) match {
      case Some(`default`) => None
      case x => x
    }

    def longField(fieldName: String): Long = stringField(fieldName).toLong
    def longFieldOption(fieldName: String): Option[Long] = stringFieldOption(fieldName) map (_.toLong)
    def longFieldOrElse(fieldName: String, default: => Long): Long = longFieldOption(fieldName).getOrElse(default)

    def timeField(fieldName: String): LocalTime = LocalTime.parse(stringField(fieldName))
    def timeFieldOption(fieldName: String): Option[LocalTime] =
      try stringFieldOption(fieldName) map LocalTime.parse
      catch { case e: DateTimeParseException => None }
    def timeFieldOrElse(fieldName: String, default: => LocalTime): LocalTime = timeFieldOption(fieldName).getOrElse(default)
    def timeFieldOption(fieldName: String, default: LocalTime): Option[LocalTime] = timeFieldOption(fieldName) match {
      case Some(`default`) => None
      case x => x
    }
  }
}
