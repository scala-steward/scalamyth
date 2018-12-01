// SPDX-License-Identifier: LGPL-2.1-only
/*
 * BackendLiveTvChain.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth
package data

import model.{ ChanId, ChannelNumber, LiveTvChainId, LiveTvChain }
import util.MythDateTime

private[myth] class BackendLiveTvChain(data: Seq[String], fieldOrder: IndexedSeq[String])
    extends GenericBackendObject(data, fieldOrder) with LiveTvChain {

  override def toString: String = s"<BackendLiveTvChain ${chainId.id}>"

  private def isoDateTimeField(f: String): MythDateTime = MythDateTime.fromIso(fields(f))

  /* Convenience accessors with proper type */

  def chainId: LiveTvChainId = LiveTvChainId(fields("chainId"))
  def chanId: ChanId = ChanId(fields("chanId").toInt)
  def startTime: MythDateTime = isoDateTimeField("startTime")
  def endTime: MythDateTime = isoDateTimeField("endTime")
  def discontinuity: Boolean = fields("discontinuity").toInt != 0
  def hostPrefix: String = fields("hostPrefix")
  def cardType: String = fields("cardType")
  def chanNum: ChannelNumber = ChannelNumber(fields("channum"))
  def inputName: String = fields("inputName")
}

private[myth] trait BackendLiveTvChainFactory extends GenericBackendObjectFactory[BackendLiveTvChain]
private[myth] trait LiveTvChainOtherSerializer extends BackendTypeSerializer[LiveTvChain]

private[myth] object BackendLiveTvChain extends BackendLiveTvChainFactory {
  final val FIELD_ORDER = IndexedSeq(
    "chainId", "chanId", "startTime", "endTime", "discontinuity", "hostPrefix",
    "cardType", "channum", "inputName"
  )

  def apply(data: Seq[String]): BackendLiveTvChain = new BackendLiveTvChain(data, FIELD_ORDER)
}
