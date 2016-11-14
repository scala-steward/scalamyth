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

  // TODO these are not serialized in the event protocol?
  //def watching: Boolean  //  is this used by anything? doesn't appear to be
  //def chainPos: Int      // this get translated (sort of) to/from maxpos in serialization
}
