// SPDX-License-Identifier: LGPL-2.1-only
/*
 * BackendInput.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth
package data

import scala.util.Try

import model.{ CaptureCardId, ChanId, Input, InputId, ListingSourceId, MultiplexId }

private[myth] class BackendInput(data: Seq[String], fieldOrder: IndexedSeq[String])
    extends GenericBackendObject(data, fieldOrder) with Input {

  private def optionalNonZeroIntField(f: String): Option[Int] =
    { Try(fields(f).toInt) filter (_ != 0) }.toOption

  override def toString: String = s"<BackendInput $inputId $sourceId $name>"

  def inputId       = InputId(fields("inputId").toInt)
  def cardId        = CaptureCardId(fields.getOrElse("cardId", fields("inputId")).toInt)
  def sourceId      = ListingSourceId(fields("sourceId").toInt)
  def chanId        = optionalNonZeroIntField("chanId") map ChanId.apply
  def mplexId       = optionalNonZeroIntField("mplexId") map MultiplexId
  def name          = fields("name")
  def displayName   = fields("displayName")
  def recPriority   = fields("recPriority").toInt
  def scheduleOrder = fields("scheduleOrder").toInt
  def liveTvOrder   = fields("liveTvOrder").toInt
  def quickTune     = fields("quickTune").toInt != 0
}

private[myth] trait BackendInputFactory extends GenericBackendObjectFactory[BackendInput]
private[myth] trait InputOtherSerializer extends BackendTypeSerializer[Input]

private[myth] object BackendInput extends BackendInputFactory {
  // NB field called 'cardId' is populated with same value as inputId
  final val FIELD_ORDER = IndexedSeq(
    "name", "sourceId", "inputId", "cardId", "mplexId", "liveTvOrder",
    "displayName", "recPriority", "scheduleOrder", "quickTune", "chanId"
  )

  def apply(data: Seq[String]): BackendInput = new BackendInput(data, FIELD_ORDER)
}

private[myth] object BackendInput91 extends BackendInputFactory {
  // 'cardId' field has been removed
  final val FIELD_ORDER = IndexedSeq(
    "name", "sourceId", "inputId", "mplexId", "liveTvOrder",
    "displayName", "recPriority", "scheduleOrder", "quickTune", "chanId"
  )

  def apply(data: Seq[String]): BackendInput = new BackendInput(data, FIELD_ORDER)
}
