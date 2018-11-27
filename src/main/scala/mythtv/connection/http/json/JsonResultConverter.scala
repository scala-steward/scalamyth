package mythtv
package connection
package http
package json

import java.net.URI

import spray.json.{ DefaultJsonProtocol, JsValue }

import model._
import util.{ MythDateTime, MythFileHash, URIFactory }
import services.{ PagedList, ServicesObject }

object JsonResultConverter extends ServiceResultConverter with FrontendJsonProtocol with BackendJsonProtocol {
  import DefaultJsonProtocol.{ listFormat, StringJsonFormat }

  @inline private def value(r: ServiceResultNode): JsValue = r.asInstanceOf[JsonValueResultNode].value

  private type PL[T] = PagedList[T]
  private type RN = ServiceResultNode
  private type SO[T] = ServicesObject[T]
  private type SOL[T] = ServicesObject[List[T]]

  def backendDetails(r: RN): BackendDetails                   = value(r).convertTo[BackendDetails]
  def blurayInfo(r: RN): BlurayInfo                           = value(r).convertTo[BlurayInfo]
  def boolean(r: RN): Boolean                                 = value(r).convertTo[Boolean]
  def captureCard(r: RN): CaptureCard                         = value(r).convertTo[CaptureCard]
  def captureCardId(r: RN): CaptureCardId                     = value(r).convertTo[CaptureCardId]
  def channelDetails(r: RN): ChannelDetails                   = value(r).convertTo[ChannelDetails]
  def connectionInfo(r: RN): ConnectionInfo                   = value(r).convertTo[ConnectionInfo]
  def frontendStatus(r: RN): FrontendStatus                   = value(r).convertTo[FrontendStatus]
  def guideBrief(r: RN): Guide[Channel, ProgramBrief]         = value(r).convertTo[SO[Guide[Channel, ProgramBrief]]].data
  def guideDetails(r: RN): Guide[ChannelDetails, Program]     = value(r).convertTo[SO[Guide[ChannelDetails, Program]]].data
  def imageSyncStatus(r: RN): ImageSyncStatus                 = value(r).convertTo[ImageSyncStatus]
  def inputId(r: RN): InputId                                 = value(r).convertTo[InputId]
  def int(r: RN): Int                                         = value(r).convertTo[Int]
  def listArtworkInfo(r: RN): List[ArtworkInfo]               = value(r).convertTo[List[ArtworkInfo]]
  def listCaptureCard(r: RN): List[CaptureCard]               = value(r).convertTo[List[CaptureCard]]
  def listChannelGroup(r: RN): List[ChannelGroup]             = value(r).convertTo[List[ChannelGroup]]
  def listInput(r: RN): List[Input]                           = value(r).convertTo[List[Input]]
  def listKnownFrontendInfo(r: RN): List[KnownFrontendInfo]   = value(r).convertTo[List[KnownFrontendInfo]]
  def listLineup(r: RN): List[Lineup]                         = value(r).convertTo[List[Lineup]]
  def listLiveStream(r: RN): List[LiveStream]                 = value(r).convertTo[List[LiveStream]]
  def listLogMessage(r: RN): List[LogMessage]                 = value(r).convertTo[List[LogMessage]]
  def listRecRuleFilter(r: RN): List[RecRuleFilterItem]       = value(r).convertTo[List[RecRuleFilterItem]]
  def listRecordedMarkup(r: RN): List[RecordedMarkupFrame]    = value(r).convertTo[List[RecordedMarkupFrame]]
  def listRecordedMarkupBytes(r: RN): List[RecordedMarkupBytes] = value(r).convertTo[List[RecordedMarkupBytes]]
  def listRecordedMarkupMs(r: RN): List[RecordedMarkupMilliseconds] = value(r).convertTo[List[RecordedMarkupMilliseconds]]
  def listRecordedSeekBytes(r: RN): List[RecordedSeekBytes]   = value(r).convertTo[List[RecordedSeekBytes]]
  def listRecordedSeekMs(r: RN): List[RecordedSeekMilliseconds] = value(r).convertTo[List[RecordedSeekMilliseconds]]
  def listRemoteEncoderState(r: RN): List[RemoteEncoderState] = value(r).convertTo[List[RemoteEncoderState]]
  def listStorageGroupDir(r: RN): List[StorageGroupDir]       = value(r).convertTo[List[StorageGroupDir]]
  def listString(r: RN): List[String]                         = value(r).convertTo[List[String]]
  def listTitleInfo(r: RN): List[TitleInfo]                   = value(r).convertTo[List[TitleInfo]]
  def listVideoLookup(r: RN): List[VideoLookup]               = value(r).convertTo[List[VideoLookup]]
  def listingSource(r: RN): ListingSource                     = value(r).convertTo[ListingSource]
  def listingSourceId(r: RN): ListingSourceId                 = value(r).convertTo[ListingSourceId]
  def liveStream(r: RN): LiveStream                           = value(r).convertTo[LiveStream]
  def mythDateTime(r: RN): MythDateTime                       = MythDateTime.fromIso(string(r))
  def mythFileHash(r: RN): MythFileHash                       = { val h = string(r); new MythFileHash(if (h != "") h else "NULL") }
  def pagedListChannel(r: RN): PL[Channel]                    = value(r).convertTo[ServicesPagedList[Channel]]
  def pagedListChannelDetails(r: RN): PL[ChannelDetails]      = value(r).convertTo[ServicesPagedList[ChannelDetails]]
  def pagedListProgram(r: RN): PL[Program]                    = value(r).convertTo[ServicesPagedList[Program]]
  def pagedListProgramBrief(r: RN): PL[ProgramBrief]          = value(r).convertTo[ServicesPagedList[ProgramBrief]]
  def pagedListRecRuleFilter(r: RN): PL[RecRuleFilterItem]    = value(r).convertTo[ServicesPagedList[RecRuleFilterItem]]
  def pagedListRecordRule(r: RN): PL[RecordRule]              = value(r).convertTo[ServicesPagedList[RecordRule]]
  def pagedListRecordable(r: RN): PL[Recordable]              = value(r).convertTo[ServicesPagedList[Recordable]]
  def pagedListRecording(r: RN): PL[Recording]                = value(r).convertTo[ServicesPagedList[Recording]]
  def pagedListVideo(r: RN): PL[Video]                        = value(r).convertTo[ServicesPagedList[Video]]
  def pagedListVideoMultiplex(r: RN): PL[VideoMultiplex]      = value(r).convertTo[ServicesPagedList[VideoMultiplex]]
  def program(r: RN): Program                                 = value(r).convertTo[Program]
  def recordedId(r: RN): RecordedId                           = value(r).convertTo[RecordedId]
  def recording(r: RN): Recording                             = value(r).convertTo[Recording]
  def recordRule(r: RN): RecordRule                           = value(r).convertTo[RecordRule]
  def recordRuleId(r: RN): RecordRuleId                       = value(r).convertTo[RecordRuleId]
  def settings(r: RN): Settings                               = value(r).convertTo[Settings]
  def soListListingSource(r: RN): SOL[ListingSource]          = value(r).convertTo[ServicesObject[List[ListingSource]]]
  def string(r: RN): String                                   = value(r).convertTo[String]
  def stringMap(r: RN): Map[String, String]                   = value(r).convertTo[Map[String, String]]
  def timeZoneInfo(r: RN): TimeZoneInfo                       = value(r).convertTo[TimeZoneInfo]
  def uri(r: RN): URI                                         = URIFactory(string(r))
  def video(r: RN): Video                                     = value(r).convertTo[Video]
  def videoMultiplex(r: RN): VideoMultiplex                   = value(r).convertTo[VideoMultiplex]
  def videoPositionBytes(r: RN): VideoPositionBytes           = value(r).convertTo[VideoPositionBytes]
  def videoPositionFrame(r: RN): VideoPositionFrame           = value(r).convertTo[VideoPositionFrame]
  def videoPositionMilliseconds(r: RN): VideoPositionMilliseconds = value(r).convertTo[VideoPositionMilliseconds]
}
