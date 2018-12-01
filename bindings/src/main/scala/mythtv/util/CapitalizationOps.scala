// SPDX-License-Identifier: LGPL-2.1-only
/*
 * CapitalizationOps.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package util

import scala.language.implicitConversions

final class CapitalizationOps(val repr: String) extends AnyVal {
  def toCamelCase: String = uncapitalize(toPascalCase)

  def toPascalCase: String = {
    if (repr eq null) null
    else if (repr.length == 0) ""
    else repr.split(' ').map(_.capitalize).mkString
  }

  // capitalize is already provided by scala.collection.immutable.StringOps
  // def capitalize: String

  def uncapitalize: String = uncapitalize(repr)

  private def uncapitalize(s: String): String = {
    if (s eq null) null
    else if (s.length == 0) ""
    else if (s.charAt(0).isLower) s
    else {
      val chars = s.toCharArray
      chars(0) = chars(0).toLower
      new String(chars)
    }
  }
}

object CapitalizationOps {
  implicit def str2CapitalizationOps(s: String): CapitalizationOps = new CapitalizationOps(s)
}
