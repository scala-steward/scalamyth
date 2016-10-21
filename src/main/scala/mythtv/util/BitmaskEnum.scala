package mythtv
package util

// TODO: add in sortedset/ordering?
// TODO check when we have run out of bits!

import java.lang.reflect.{ Field => JField, Method => JMethod }
import scala.collection.{ mutable, immutable, AbstractIterator, AbstractSet, GenSet, Set, SetLike }
import scala.reflect.NameTransformer._
import scala.util.matching.Regex

abstract class BitmaskEnum[@specialized(Int,Long) T: BitWise] {
  thisenum =>

  private val vmap: mutable.Map[T, Value] = new mutable.HashMap
  private val umap: mutable.Map[T, Value] = new mutable.HashMap
  private val nmap: mutable.Map[T, String] = new mutable.HashMap
  private var vset: Mask = _
  protected var nextId: T = _

  // work around specialization bugs with initializing fields
  private def init() = {
    vset = Mask(implicitly[BitWise[T]].zero)
    nextId = implicitly[BitWise[T]].one
  }
  init()

  override def toString =
    ((getClass.getName stripSuffix MODULE_SUFFIX_STRING split '.').last split
      Regex.quote(NAME_JOIN_STRING)).last

  final def apply(x: T): Value = vmap(x)

  def values: Set[Value] = vset

  protected final def Value: Value = Value(nextId)
  protected final def Value(name: String): Value = Value(nextId, name)

  protected final def Value(i: T): Value = new Val(i)
  protected final def Value(i: T, name: String): Value = new Val(i, name)

  /* Use Java reflection to populate the name map */
  private def populateNameMap() {
    val fields: Array[JField] = getClass.getDeclaredFields
    def isValDef(m: JMethod): Boolean =
      fields exists (fd => fd.getName == m.getName && fd.getType == m.getReturnType)

    val methods: Array[JMethod] = getClass.getMethods filter (m =>
        m.getParameterTypes.isEmpty &&
        classOf[Base].isAssignableFrom(m.getReturnType) &&
        m.getDeclaringClass != classOf[BitmaskEnum[_]] &&
        isValDef(m))

    methods foreach { m =>
      val name = m.getName
      val value = m.invoke(this).asInstanceOf[Base]
      if (value.outerEnum eq thisenum) {
        val id = implicitly[BitWise[T]].unbox(classOf[Base] getMethod "id" invoke value)
        nmap += ((id, name))
      }
    }
  }

  private def nameOf(i: T): String =
    synchronized { nmap.getOrElse(i, { populateNameMap() ; nmap(i) }) }

  private def undefined(i: T): Value =
    synchronized { umap.getOrElseUpdate(i, new UndefinedVal(i)) }

  import BitWise.BitwiseOps

  trait Base {
    private[BitmaskEnum] val outerEnum = thisenum

    def id: T

    final def + (elem: Value): Mask = | (elem)
    final def - (elem: Value): Mask = Mask(id & ~elem.id)
    final def | (elem: Value): Mask = Mask(id | elem.id)
    final def & (elem: Value): Mask = Mask(id & elem.id)
    final def ^ (elem: Value): Mask = Mask(id ^ elem.id)
    final def unary_~ : Mask = Mask(~id)

    override def equals(other: Any) = other match {
      case that: BitmaskEnum[_]#Value => (outerEnum eq that.outerEnum) && (id == that.id)
      case _                          => false
    }
    override def hashCode: Int = id.##
  }

  trait Value extends Base {
    def isDefined: Boolean
    final def toMask: Mask = Mask(id)
  }

  object Mask {
    val empty = new Mask(implicitly[BitWise[T]].zero, "<empty>")

    def apply(i: T): Mask = new Mask(i, null)
    def apply(i: T, name: String) = new Mask(i, name)

    def apply(v: Value) = new Mask(v.id, null)
    def apply(v: Value, name: String) = new Mask(v.id, name)

    def apply(m: Mask): Mask = m
    def apply(m: Mask, name: String): Mask = new Mask(m.id, name)
  }

  class Mask private(m: T, name: String) extends AbstractSet[Value] with SetLike[Value, Mask] with Base {
    private def this(i: T) = this(i, null)

    final def id = m
    final def contains(elem: Value) = (id & elem.id) != 0
    def iterator: Iterator[Value] = new MaskIterator(id)
    override def empty: Mask = Mask.empty
    override def size = implicitly[BitWise[T]].bitCount(id)
    override def stringPrefix = thisenum + ".Mask"

    /* methods & | &~ defined to GenSetLike to forward to intersect, union, diff */

    final override def diff(that: GenSet[Value]): Mask = that match {
      case mask: Mask => new Mask(id & ~mask.id)
      case _ => super.diff(that)
    }

    final override def intersect(that: GenSet[Value]): Mask = that match {
      case mask: Mask => new Mask(id & mask.id)
      case _ => super.intersect(that)
    }

    final override def union(that: GenSet[Value]): Mask = that match {
      case mask: Mask => new Mask(id | mask.id)
      case _ => super.union(that)
    }

    final override def subsetOf(that: GenSet[Value]): Boolean = that match {
      case mask: Mask => (id & mask.id) == id
      case _ => super.subsetOf(that)
    }

    override def toString =
      if (name ne null) name
      else try thisenum.nameOf(m)
      catch {
        case _: NoSuchElementException => mkString(stringPrefix + "(", " | ", ")")
      }
  }

  private class MaskIterator(private[this] var bits: T) extends AbstractIterator[Value] {
    private[this] var shifts: Int = 0

    def hasNext = implicitly[BitWise[T]].bitCount(bits) != 0
    def next(): Value = {
      val nz = implicitly[BitWise[T]].numberOfTrailingZeros(bits)
      if (nz > 0) {
        bits >>= nz
        shifts += nz
      }

      val i = implicitly[BitWise[T]].one << shifts
      bits >>= 1
      shifts += 1

      if (vmap contains i) vmap(i)
      else undefined(i)
    }
  }

  private class Val(i: T, name: String) extends Value {
    def this(i: T) = this(i, null)

    assert(!vmap.isDefinedAt(i), "Duplicate id: " + i)
    assert(implicitly[BitWise[T]].bitCount(i) == 1, s"value 0x${i.toHexString} contains more than one bit")
    vmap(i) = this
    vset |= this
    nextId = i << 1

    def id = i
    final def isDefined = true

    override def toString =
      if (name ne null) name
      else try thisenum.nameOf(i)
      catch { case _: NoSuchElementException => s"<Invalid enum: no field for #$i>" }
  }

  private class UndefinedVal(i: T) extends Value {
    def id = i
    final def isDefined = false
    override def toString = s"<undefined ${thisenum}.Value 0x${i.toHexString}>"
  }
}
