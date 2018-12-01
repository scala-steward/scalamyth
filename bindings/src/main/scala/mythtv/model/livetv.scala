// SPDX-License-Identifier: LGPL-2.1-only
/*
 * livetv.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package model

import util.MythDateTime

final case class LiveTvChainId(id: String) extends AnyVal

trait LiveTvChain {
  def chainId: LiveTvChainId
  def chanId: ChanId
  def startTime: MythDateTime
  def endTime: MythDateTime
  def discontinuity: Boolean
  def hostPrefix: String
  def cardType: String
  def chanNum: ChannelNumber
  def inputName: String

  // these are in the DB but not serialized in the event protocol
  //def watching: Boolean  //  is this used by anything? doesn't appear to be
  //def chainPos: Int      // this get translated (sort of) to/from maxpos in serialization
}
