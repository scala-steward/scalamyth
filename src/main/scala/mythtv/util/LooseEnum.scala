package mythtv
package util

/**
  * An enumeration class that optionally allows for capturing unknown
  * enumeration values and surfacing them in an `unknowns` member set.
  *
  * The `values` set will never have an unknown enumeration value added to it.
  *
  * To capture possible unknown enumeration values, use the `applyOrUnknown`
  * factory method rather than then usual `apply`.
  *
  * Limitation:
  *  Using the `+` method on `Value` to build a `ValueSet` will throw an
  *  exception if an `UnknownVal` is attempted to be used in this manner.
  */

abstract class LooseEnum(initial: Int) extends Enumeration(initial) {
  thisenum =>

  def this() = this(0)

  @volatile private var unknownVals: Map[Int, Value] = Map.empty

  final def applyOrUnknown(x: Int): Value =
    try apply(x)
    catch { case _: NoSuchElementException => unknown(x) }

  def unknowns: Set[Value] = unknownVals.values.toSet

  private def unknown(x: Int): Value =
    if (unknownVals isDefinedAt x) unknownVals(x)
    else synchronized {
      val unk = new UnknownVal(x)
      unknownVals = unknownVals.updated(x, unk)
      unk
    }

  private class UnknownVal(i: Int) extends super.Value {
    def id = i
    override def toString = s"<unknown ${thisenum}.Value $i>"
  }
}
