package mythtv
package util

import java.lang.reflect.{ Field => JField, Method => JMethod }
import scala.collection.{ mutable, immutable, AbstractIterator, AbstractSet, GenSet, Set, SortedSetLike }
import scala.reflect.NameTransformer.{ MODULE_SUFFIX_STRING, NAME_JOIN_STRING }
import scala.util.matching.Regex

abstract class LongBitmaskEnum {
  thisenum =>

  private val vmap: mutable.LongMap[Value] = new mutable.LongMap
  private val umap: mutable.LongMap[Value] = new mutable.LongMap
  private val mmap: mutable.LongMap[Mask] = new mutable.LongMap
  private val nmap: mutable.LongMap[String] = new mutable.LongMap
  private var vset: Mask = Mask.empty
  private var nextId: Long = 1L

  override def toString =
    ((getClass.getName stripSuffix MODULE_SUFFIX_STRING split '.').last split
      Regex.quote(NAME_JOIN_STRING)).last

  final def apply(x: Long): Base =
    try vmap(x)
    catch { case _: NoSuchElementException => Mask(x, null, cache = false) }

  def values: Set[Value] = vset

  protected final def Value: Value = Value(nextId)
  protected final def Value(name: String): Value = Value(nextId, name)

  protected final def Value(i: Long): Value = new Val(i)
  protected final def Value(i: Long, name: String): Value = new Val(i, name)

  /* Use Java reflection to populate the name map */
  private def populateNameMap() = {
    val fields: Array[JField] = getClass.getDeclaredFields
    def isValDef(m: JMethod): Boolean =
      fields exists (fd => fd.getName == m.getName && fd.getType == m.getReturnType)

    val methods: Array[JMethod] = getClass.getMethods filter (m =>
        m.getParameterTypes.isEmpty &&
        classOf[Base].isAssignableFrom(m.getReturnType) &&
        m.getDeclaringClass != classOf[LongBitmaskEnum] &&
        isValDef(m))

    methods foreach { m =>
      val name = m.getName
      val value = m.invoke(this).asInstanceOf[Base]
      if (value.outerEnum eq thisenum) {
        val id = Long.unbox(classOf[Base] getMethod "id" invoke value)
        nmap += ((id, name))
      }
    }
  }

  private def nameOf(i: Long): String =
    synchronized { nmap.getOrElse(i, { populateNameMap() ; nmap(i) }) }

  private def undefined(i: Long): Value =
    synchronized { umap.getOrElseUpdate(i, new UndefinedVal(i)) }

  trait Base {
    private[LongBitmaskEnum] val outerEnum = thisenum

    def id: Long
    def contains(elem: Value): Boolean

    final def containsAny(mask: Mask): Boolean = (id & mask.id) != 0

    final def + (elem: Value): Mask = | (elem)
    final def - (elem: Value): Mask = transientMask(id & ~elem.id)
    final def | (elem: Value): Mask = transientMask(id | elem.id)
    final def & (elem: Value): Mask = transientMask(id & elem.id)
    final def ^ (elem: Value): Mask = transientMask(id ^ elem.id)
    final def unary_~ : Mask = transientMask(~id)

    protected def transientMask(id: Long): Mask = Mask(id, null, cache = false)

    override def equals(other: Any) = other match {
      case that: LongBitmaskEnum#Base => (outerEnum eq that.outerEnum) && (id == that.id)
      case _ => false
    }
    override def hashCode: Int = id.##
  }

  trait Value extends Ordered[Value] with Base {
    def isDefined: Boolean
    final def toMask: Mask = transientMask(id)
    final def contains(elem: Value): Boolean = id == elem.id

    override def compare(that: Value): Int =
      if (id < that.id) -1
      else if (id == that.id) 0
      else 1
  }

  object ValueOrdering extends Ordering[Value] {
    def compare(x: Value, y: Value) = x compare y
  }

  object Mask {
    val empty = new Mask(0L, "<empty>", false)

    def apply(i: Long): Mask = Mask(i, null, cache = true)
    def apply(i: Long, name: String): Mask = Mask(i, name, cache = true)

    def apply(v: Value): Mask = Mask(v.id, null, cache = true)
    def apply(v: Value, name: String): Mask = Mask(v.id, name, cache = true)

    def apply(m: Mask): Mask = Mask(m.id, m.name, cache = true)
    def apply(m: Mask, name: String): Mask = new Mask(m.id, name, cache = true) // special case, may be renaming a Mask already cached

    /*private[LongBitmaskEnum]*/ def apply(id: Long, name: String, cache: Boolean): Mask = {
      if (mmap contains id) mmap(id)
      else new Mask(id, name, cache)
    }
  }

  class Mask private(m: Long, private val name: String, cache: Boolean)
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
    override def size = java.lang.Long.bitCount(id)
    override def stringPrefix = thisenum + ".Mask"

    override def foreach[U](f: Value => U): Unit = {
      var bits = id
      var shifts = 0

      while (java.lang.Long.bitCount(bits) != 0) {
        val nz = java.lang.Long.numberOfTrailingZeros(bits)
        if (nz > 0) {
          bits >>= nz
          shifts += nz
        }

        val i = 1L << shifts
        val v =
          if (vmap contains i) vmap(i)
          else undefined(i)
        f(v)

        bits >>= 1
        shifts += 1
      }
    }

    def keysIteratorFrom(start: Value): Iterator[Value] = {
      val nz = java.lang.Long.numberOfTrailingZeros(start.id)
      new MaskIterator(id & (-1L << nz))
    }

    def rangeImpl(from: Option[Value], until: Option[Value]): Mask = {
      var span = id
      if (from.isDefined) {
        val nz = java.lang.Long.numberOfTrailingZeros(from.get.id)
        span &= (-1L << nz)
      }
      if (until.isDefined) {
        val nz = java.lang.Long.numberOfTrailingZeros(until.get.id)
        span &= ~(-1L << nz)
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

  private class MaskIterator(private[this] var bits: Long) extends AbstractIterator[Value] {
    private[this] var shifts: Int = 0

    def hasNext = java.lang.Long.bitCount(bits) != 0
    def next(): Value = {
      val nz = java.lang.Long.numberOfTrailingZeros(bits)
      if (nz > 0) {
        bits >>= nz
        shifts += nz
      }

      val i = 1L << shifts
      bits >>= 1
      shifts += 1

      if (vmap contains i) vmap(i)
      else undefined(i)
    }
  }

  private class Val(i: Long, name: String) extends Value {
    def this(i: Long) = this(i, null)

    assert(!vmap.contains(i), "Duplicate id: " + i)
    assert(java.lang.Long.bitCount(i) == 1, s"value 0x${i.toHexString} contains more than one bit")
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

  private class UndefinedVal(i: Long) extends Value {
    def id = i
    final def isDefined = false
    override def toString = s"<undefined $thisenum.Value 0x${i.toHexString}>"
  }
}
