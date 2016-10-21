package mythtv
package util

// TODO: add in sortedset/ordering?
// TODO check when we have run out of bits!

import java.lang.reflect.{ Field => JField, Method => JMethod }
import scala.collection.{ mutable, immutable, AbstractIterator, AbstractSet, Set }
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
  private def init(): Unit = {
    vset = new MaskImpl(implicitly[BitWise[T]].zero)
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

  protected final def Mask(i: T): Mask = new MaskImpl(i, null, true)
  protected final def Mask(i: T, name: String): Mask = new MaskImpl(i, name, true)
  protected final def Mask(m: Mask, name: String): Mask = new MaskImpl(m.id, name, true)
  protected final def Mask(m: Mask): Mask = {
    assert(!vmap.isDefinedAt(m.id), "Duplicate id: " + m.id)
    vmap(m.id) = m
    m
  }

  /* Use Java reflection to populate the name map */
  private def populateNameMap() {
    val fields: Array[JField] = getClass.getDeclaredFields
    def isValDef(m: JMethod): Boolean =
      fields exists (fd => fd.getName == m.getName && fd.getType == m.getReturnType)

    val methods: Array[JMethod] = getClass.getMethods filter (m =>
        m.getParameterTypes.isEmpty &&
        classOf[Value].isAssignableFrom(m.getReturnType) &&
        m.getDeclaringClass != classOf[BitmaskEnum[_]] &&
        isValDef(m))

    methods foreach { m =>
      val name = m.getName
      val value = m.invoke(this).asInstanceOf[Value]
      if (value.outerEnum eq thisenum) {
        val id = implicitly[BitWise[T]].unbox(classOf[Value] getMethod "id" invoke value)
        nmap += ((id, name))
      }
    }
  }

  private def nameOf(i: T): String =
    synchronized { nmap.getOrElse(i, { populateNameMap() ; nmap(i) }) }

  private def unknown(i: T): Value =
    synchronized { umap.getOrElseUpdate(i, new UnknownVal(i)) }

  import BitWise.BitwiseOps

  trait Value {
    private[BitmaskEnum] val outerEnum = thisenum

    def id: T

    final def + (elem: Value): Mask = | (elem)
    final def - (elem: Value): Mask = new MaskImpl(id & ~elem.id)
    final def | (elem: Value): Mask = new MaskImpl(id | elem.id)
    final def & (elem: Value): Mask = new MaskImpl(id & elem.id)
    final def ^ (elem: Value): Mask = new MaskImpl(id ^ elem.id)
    final def unary_~ : Mask = new MaskImpl(~id)

    // TODO add a toMask function?

    override def equals(other: Any) = other match {
      case that: BitmaskEnum[_]#Value => (outerEnum eq that.outerEnum) && (id == that.id)
      case _                          => false
    }
    override def hashCode: Int = id.##
  }

  // TODO inherit from SetLike[Value, Mask] also? to make Set method return Mask type rather than Set[Value]
  abstract class Mask extends AbstractSet[Value] with Value

  /*private*/ class MaskImpl(m: T, name: String, cached: Boolean) extends Mask {
    def this(i: T) = this(i, null, false)

    // This conflicted with |= with vset Mask on every Val added (we got dup ids)
    //    however that messes up apply.... can we make vset more dynamic? or not
    //    convert to a mask until requested? maybe we need to be able to create
    //    non-cached masks (i.e. ones that are not defined in the enum)
    if (cached) {
      assert(!vmap.isDefinedAt(m), "Duplicate id: " + m)
      vmap(m) = this
    }

    // TODO do I want to override diff, union, intersect? Any other set operators?

    final def id = m
    final def contains(elem: Value) = (id & elem.id) != 0
    def iterator: Iterator[Value] = new MaskIterator(id)
    override def size = implicitly[BitWise[T]].bitCount(id)
    override def stringPrefix = thisenum + ".Mask"

    def union(that: Mask): Mask = | (that)   // TODO should be an override...

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
      else unknown(i)
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

    override def toString =
      if (name ne null) name
      else try thisenum.nameOf(i)
      catch { case _: NoSuchElementException => s"<Invalid enum: no field for #$i>" }
  }

  private class UnknownVal(i: T) extends Value {
    def id = i
    override def toString = s"<unknown ${thisenum}.Value 0x${i.toHexString}>"
  }
}
