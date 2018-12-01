// SPDX-License-Identifier: LGPL-2.1-only
/*
 * GenericBackendObject.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth
package data

private[myth] abstract class GenericBackendObject(data: Seq[String], fieldOrder: IndexedSeq[String]) {
  // assumes data.length >= fieldOrder.length, or else some fields will be missing
  val fields: Map[String, String] = (fieldOrder zip data).toMap
  def apply(fieldName: String): String = fields(fieldName)
  def get(fieldName: String): Option[String] = fields.get(fieldName)
}

private[myth] trait GenericBackendObjectFactory[+A] {
  def FIELD_ORDER: IndexedSeq[String]
  def apply(data: Seq[String]): A
}
