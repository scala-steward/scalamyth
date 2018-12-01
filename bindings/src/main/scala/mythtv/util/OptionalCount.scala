// SPDX-License-Identifier: LGPL-2.1-only
/*
 * OptionalCount.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package util

sealed abstract class OptionalCount[+A] {
  def isEmpty: Boolean
  def isFinite: Boolean
  def isDefined: Boolean = !isEmpty
  final def nonEmpty: Boolean = isDefined
  def get: A
}

final case class OptionalCountSome[+A](x: A) extends OptionalCount[A] {
  def isEmpty = false
  def isFinite = true
  def get: A = x
}

case object OptionalCountAll extends OptionalCount[Nothing] {
  def isEmpty = false
  def isFinite = false
  def get = throw new NoSuchElementException("OptionalCountAll.get")
}

case object OptionalCountNone extends OptionalCount[Nothing] {
  def isEmpty = true
  def isFinite = true
  def get = throw new NoSuchElementException("OptionalCountNone.get")
}

object OptionalCount {
  import scala.language.implicitConversions
  implicit def anyToOptionalCount[A](x: A): OptionalCount[A] = OptionalCountSome(x)

  def apply[A](x: A): OptionalCount[A] = if (x == null) OptionalCountNone else OptionalCountSome(x)
  def empty[A]: OptionalCount[A] = OptionalCountNone
  def none[A]: OptionalCount[A] = OptionalCountNone
  def all[A]: OptionalCount[A] = OptionalCountAll
}
