// SPDX-License-Identifier: LGPL-2.1-only
/*
 * DynamicEnum.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package util

import scala.language.{ dynamics, implicitConversions }

sealed trait DynamicEnumValueProvider

object DynamicEnumValueProvider {
  case class EnumValuesStrings(values: Iterable[String]) extends DynamicEnumValueProvider
  case class EnumValuesNameMap(nameMap: Map[String, Int]) extends DynamicEnumValueProvider
  case class EnumValuesIntIdMap(idMap: Map[Int, String]) extends DynamicEnumValueProvider

  case class EnumValuesFuncStrings(f: () => Iterable[String]) extends DynamicEnumValueProvider
  case class EnumValuesFuncNameMap(f: () => Map[String, Int]) extends DynamicEnumValueProvider
  case class EnumValuesFuncIntIdMap(f: () => Map[Int, String]) extends DynamicEnumValueProvider

  implicit def iterable2Provider(v: Iterable[String]): DynamicEnumValueProvider = EnumValuesStrings(v)
  implicit def nameMap2Provider(m: Map[String, Int]): DynamicEnumValueProvider = EnumValuesNameMap(m)
  implicit def intMap2Provider(m: Map[Int, String]): DynamicEnumValueProvider = EnumValuesIntIdMap(m)

  implicit def funcIter2Provider(f: () => Iterable[String]): DynamicEnumValueProvider = EnumValuesFuncStrings(f)
  implicit def funcNameMap2Provider(f: () => Map[String, Int]): DynamicEnumValueProvider = EnumValuesFuncNameMap(f)
  implicit def funcIntMap2Provider(f: () => Map[Int, String]): DynamicEnumValueProvider = EnumValuesFuncIntIdMap(f)
}

trait DynamicEnumLike extends Dynamic {
  import DynamicEnumValueProvider._

  type EnumValue

  protected def valueProvider: DynamicEnumValueProvider
  protected def newValue(name: String): EnumValue
  protected def newValue(id: Int, name: String): EnumValue

  private[this] val ourVals: Map[String, EnumValue] = valueProvider match {
    case EnumValuesStrings(strings) => strings.map(n => (identifier(n), newValue(n)))(collection.breakOut)
    case EnumValuesFuncStrings(f)   => f().map(    n => (identifier(n), newValue(n)))(collection.breakOut)
    case EnumValuesNameMap(nmap)    => nmap map { case (n, i) => (identifier(n), newValue(i, n)) }
    case EnumValuesFuncNameMap(f)   => f()  map { case (n, i) => (identifier(n), newValue(i, n)) }
    case EnumValuesIntIdMap(imap)   => imap map { case (i, n) => (identifier(n), newValue(i, n)) }
    case EnumValuesFuncIntIdMap(f)  => f()  map { case (i, n) => (identifier(n), newValue(i, n)) }
  }

  def selectDynamic(name: String): EnumValue = {
    if (ourVals contains name) ourVals(name)
    else {
      val id = identifier(name)
      if (ourVals contains id) ourVals(id)
      else throw new NoSuchElementException(name)
    }
  }

  final def identifiers: Set[String] = ourVals.keySet

  // A function to translate name from provider to a legal scala identifier
  // so that we don't need to escape it in backticks to use it.
  //
  // The default implementation simply squashes space characters and converts
  // the first character to upper case if it is not already.
  //
  // Override if an alternate implementation is required.
  def identifier(name: String): String = name.replace(" ", "").capitalize
}

class DynamicEnum(provider: DynamicEnumValueProvider, initial: Int)
  extends Enumeration(initial)
     with DynamicEnumLike {
  type EnumValue = Value

  def this(provider: DynamicEnumValueProvider) = this(provider, 0)
  def this(names: String*) = this(DynamicEnumValueProvider.EnumValuesStrings(names), 0)
  def this(initial: Int, names: String*) = this(DynamicEnumValueProvider.EnumValuesStrings(names), initial)

  def valueProvider: DynamicEnumValueProvider = provider
  def newValue(name: String) = Value(name)
  def newValue(id: Int, name: String) = Value(id, name)
}


class DynamicLooseEnum(provider: DynamicEnumValueProvider, initial: Int)
  extends LooseEnum(initial)
     with DynamicEnumLike {
  type EnumValue = Value

  def this(provider: DynamicEnumValueProvider) = this(provider, 0)
  def this(names: String*) = this(DynamicEnumValueProvider.EnumValuesStrings(names), 0)
  def this(initial: Int, names: String*) = this(DynamicEnumValueProvider.EnumValuesStrings(names), initial)

  def valueProvider: DynamicEnumValueProvider = provider
  def newValue(name: String) = Value(name)
  def newValue(id: Int, name: String) = Value(id, name)
}


class DynamicBitmaskEnum(provider: DynamicEnumValueProvider)
  extends IntBitmaskEnum
     with DynamicEnumLike {
  type EnumValue = Base

  def this(names: String*) = this(DynamicEnumValueProvider.EnumValuesStrings(names))

  def valueProvider: DynamicEnumValueProvider = provider
  def newValue(name: String) = Value(name)
  def newValue(id: Int, name: String) = {
    if (java.lang.Integer.bitCount(id) == 1) Value(id, name)
    else                                      Mask(id, name)
  }
}
