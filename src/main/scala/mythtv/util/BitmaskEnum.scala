package mythtv
package util

// TODO check when we have run out of bits!
// TODO override newBuilder in Mask object?
// TODO investigate java.lang.Long.lowestOneBit

import java.lang.reflect.{ Field => JField, Method => JMethod }
import scala.collection.{ mutable, immutable, AbstractIterator, AbstractSet, GenSet, Set, SortedSetLike }
import scala.reflect.NameTransformer._
import scala.util.matching.Regex

abstract class BitmaskEnum[@specialized(Int,Long) T: BitWise] {
  thisenum =>

  private val vmap: mutable.Map[T, Value] = new mutable.HashMap
  private val umap: mutable.Map[T, Value] = new mutable.HashMap
  private val mmap: mutable.Map[T, Mask] = new mutable.HashMap
  private val nmap: mutable.Map[T, String] = new mutable.HashMap
  private var vset: Mask = _
  private var nextId: T = _

  // work around specialization bugs with initializing fields
  private def init() = {
    vset = Mask.empty
    nextId = implicitly[BitWise[T]].one
  }
  init()

  override def toString =
    ((getClass.getName stripSuffix MODULE_SUFFIX_STRING split '.').last split
      Regex.quote(NAME_JOIN_STRING)).last

  final def apply(x: T): Base =
    try vmap(x)
    catch { case _: NoSuchElementException => mmap(x) }

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

  import BitWise.BitwiseOps   // TODO can we eliminate this import?

  trait Base {
    private[BitmaskEnum] val outerEnum = thisenum

    def id: T

    final def + (elem: Value): Mask = | (elem)
    final def - (elem: Value): Mask = transientMask(id & ~elem.id)
    final def | (elem: Value): Mask = transientMask(id | elem.id)
    final def & (elem: Value): Mask = transientMask(id & elem.id)
    final def ^ (elem: Value): Mask = transientMask(id ^ elem.id)
    final def unary_~ : Mask = transientMask(~id)

    protected def transientMask(id: T): Mask = Mask(id, null, false)

    override def equals(other: Any) = other match {
      case that: BitmaskEnum[_]#Value => (outerEnum eq that.outerEnum) && (id == that.id)
      case _                          => false
    }
    override def hashCode: Int = id.##
  }

  trait Value extends Ordered[Value] with Base {
    def isDefined: Boolean
    final def toMask: Mask = transientMask(id)

    override def compare(that: Value): Int =
      if (id < that.id) -1
      else if (id == that.id) 0
      else 1
  }

  object ValueOrdering extends Ordering[Value] {
    def compare(x: Value, y: Value) = x compare y
  }

  object Mask {
    val empty = new Mask(implicitly[BitWise[T]].zero, "<empty>", false)

    def apply(i: T): Mask = Mask(i, null, true)
    def apply(i: T, name: String): Mask = Mask(i, name, true)

    def apply(v: Value): Mask = Mask(v.id, null, true)
    def apply(v: Value, name: String): Mask = Mask(v.id, name, true)

    def apply(m: Mask): Mask = Mask(m.id, m.name, true)
    def apply(m: Mask, name: String): Mask = new Mask(m.id, name, true) // special case, may be renaming a Mask already cached

    private[BitmaskEnum] def apply(id: T, name: String, cache: Boolean): Mask = {
      if (mmap isDefinedAt id) mmap(id)
      else new Mask(id, name, cache)
    }
  }

  class Mask private(m: T, private val name: String, cache: Boolean)
    extends AbstractSet[Value]
       with immutable.SortedSet[Value]
       with SortedSetLike[Value, Mask]
       with Base {
    final def id = m
    final def contains(elem: Value) = (id & elem.id) != 0
    implicit final def ordering: Ordering[Value] = ValueOrdering
    if (cache) mmap(id) = this

    def iterator: Iterator[Value] = new MaskIterator(id)
    override def empty: Mask = Mask.empty
    override def size = implicitly[BitWise[T]].bitCount(id)
    override def stringPrefix = thisenum + ".Mask"

    // TODO override foreach?

    def keysIteratorFrom(start: Value): Iterator[Value] = {
      val nz = implicitly[BitWise[T]].numberOfTrailingZeros(start.id)
      new MaskIterator(id & (implicitly[BitWise[T]].minusone << nz))
    }

    def rangeImpl(from: Option[Value], until: Option[Value]): Mask = {
      var span = id
      if (from.isDefined) {
        val nz = implicitly[BitWise[T]].numberOfTrailingZeros(from.get.id)
        span &= (implicitly[BitWise[T]].minusone << nz)
      }
      if (until.isDefined) {
        val nz = implicitly[BitWise[T]].numberOfTrailingZeros(until.get.id)
        span &= ~(implicitly[BitWise[T]].minusone << nz)
      }
      transientMask(span)
    }

    /* methods & | &~ defined in GenSetLike to forward to intersect, union, diff
       so we only override the latter methods here*/

    final override def diff(that: GenSet[Value]): Mask = that match {
      case mask: Mask => transientMask(id & ~mask.id)
      case _ => super.diff(that)
    }

    final override def intersect(that: GenSet[Value]): Mask = that match {
      case mask: Mask => transientMask(id & mask.id)
      case _ => super.intersect(that)
    }

    final override def union(that: GenSet[Value]): Mask = that match {
      case mask: Mask => transientMask(id | mask.id)
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
