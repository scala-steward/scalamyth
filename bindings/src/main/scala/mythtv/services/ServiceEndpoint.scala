// SPDX-License-Identifier: LGPL-2.1-only
/*
 * ServiceEndpoint.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package services

import connection.http.HttpRequestMethod

trait ServiceEndpoint {
  type TypeName = String

  def name: String
  def resultType: TypeName
  def parameters: Map[String, TypeName]
  def requestMethod: HttpRequestMethod

  override def toString: String =
    "[" + requestMethod + "] " + name + ": " + resultType + parameters.mkString(" (", ", ", ")")
}
