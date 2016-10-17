package mythtv

package object util {
  // Used to indicate serialization format for certain MythProtocol commands
  private[mythtv] implicit final class MythDateTimeString(mythDateTime: MythDateTime) {
    override def toString: String = mythDateTime.mythformat
    def toMythDateTime: MythDateTime = mythDateTime
  }

  // TODO do I want this implicit here or in MythDateTime companion. Easier to import
  // here using util._ wildcard. May come into scope naturally at other times.
  /*
  import scala.language.implicitConversions
  implicit def javaInstant2MythDt(instant: Instant) = new MythDateTime(instant)
  implicit def javaLocalDt2MythDt(dt: LocalDateTime) = new MythDateTime(dt)
  */
}
