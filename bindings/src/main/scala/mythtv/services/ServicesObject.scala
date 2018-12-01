// SPDX-License-Identifier: LGPL-2.1-only
/*
 * ServicesObject.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package services

import model._
import util.MythDateTime

trait ServicesObject[+T] {
  def data: T
  def asOf: MythDateTime
  def mythVersion: String
  def mythProtocolVersion: String
}

private[mythtv] trait IsServicesListObject[T] {
  def listFieldName: String
}

private[mythtv] object IsServicesListObject {
  implicit object ChannelIsServicesListObject        extends IsServicesListObject[Channel]        { def listFieldName = "ChannelInfos" }
  implicit object ChannelDetailsIsServicesListObject extends IsServicesListObject[ChannelDetails] { def listFieldName = "ChannelInfos" }
  implicit object ListingSourceIsServicesListObject  extends IsServicesListObject[ListingSource]  { def listFieldName = "VideoSources" }
  implicit object ProgramIsServicesListObject        extends IsServicesListObject[Program]        { def listFieldName = "Programs" }
  implicit object ProgramBriefIsServicesListObject   extends IsServicesListObject[ProgramBrief]   { def listFieldName = "Programs" }
  implicit object RecRuleFilterIsServicesListObject  extends IsServicesListObject[RecRuleFilterItem]  { def listFieldName = "RecRuleFilters" }
  implicit object RecordRuleIsServicesListObject     extends IsServicesListObject[RecordRule]     { def listFieldName = "RecRules"}
  implicit object RecordableIsServicesListObject     extends IsServicesListObject[Recordable]     { def listFieldName = "Programs" }
  implicit object RecordingIsServicesListObject      extends IsServicesListObject[Recording]      { def listFieldName = "Programs" }
  implicit object SongIsServicesListObject           extends IsServicesListObject[Song]           { def listFieldName = "MusicMetadataInfos" }
  implicit object VideoIsServicesListObject          extends IsServicesListObject[Video]          { def listFieldName = "VideoMetadataInfos" }
  implicit object VideoLookupIsServicesListObject    extends IsServicesListObject[VideoLookup]    { def listFieldName = "VideoLookups" }
  implicit object VideoMultiplexIsServicesListObject extends IsServicesListObject[VideoMultiplex] { def listFieldName = "VideoMultiplexes" }
}
