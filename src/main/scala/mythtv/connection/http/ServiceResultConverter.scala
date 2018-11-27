package mythtv
package connection
package http

import java.net.URI

import model._
import util.{ MythDateTime, MythFileHash }
import services.{ PagedList, ServicesObject }

// implement this trait differently for each serialization type (JSON, XML, etc.)
trait ServiceResultConverter {
  def backendDetails(r: ServiceResultNode): BackendDetails
  def blurayInfo(r: ServiceResultNode): BlurayInfo
  def boolean(r: ServiceResultNode): Boolean
  def captureCard(r: ServiceResultNode): CaptureCard
  def captureCardId(r: ServiceResultNode): CaptureCardId
  def channelDetails(r: ServiceResultNode): ChannelDetails
  def connectionInfo(r: ServiceResultNode): ConnectionInfo
  def frontendStatus(r: ServiceResultNode): FrontendStatus
  def guideBrief(r: ServiceResultNode): Guide[Channel, ProgramBrief]
  def guideDetails(r: ServiceResultNode): Guide[ChannelDetails, Program]
  def imageSyncStatus(r: ServiceResultNode): ImageSyncStatus
  def inputId(r: ServiceResultNode): InputId
  def int(r: ServiceResultNode): Int
  def listArtworkInfo(r: ServiceResultNode): List[ArtworkInfo]
  def listCaptureCard(r: ServiceResultNode): List[CaptureCard]
  def listChannelGroup(r: ServiceResultNode): List[ChannelGroup]
  def listInput(r: ServiceResultNode): List[Input]
  def listKnownFrontendInfo(r: ServiceResultNode): List[KnownFrontendInfo]
  def listLineup(r: ServiceResultNode): List[Lineup]
  def listLiveStream(r: ServiceResultNode): List[LiveStream]
  def listLogMessage(r: ServiceResultNode): List[LogMessage]
  def listRecRuleFilter(r: ServiceResultNode): List[RecRuleFilterItem]
  def listRemoteEncoderState(r: ServiceResultNode): List[RemoteEncoderState]
  def listStorageGroupDir(r: ServiceResultNode): List[StorageGroupDir]
  def listString(r: ServiceResultNode): List[String]
  def listTitleInfo(r: ServiceResultNode): List[TitleInfo]
  def listVideoLookup(r: ServiceResultNode): List[VideoLookup]
  def listingSource(r: ServiceResultNode): ListingSource
  def listingSourceId(r: ServiceResultNode): ListingSourceId
  def liveStream(r: ServiceResultNode): LiveStream
  def mythDateTime(r: ServiceResultNode): MythDateTime
  def mythFileHash(r: ServiceResultNode): MythFileHash
  def pagedListChannel(r: ServiceResultNode): PagedList[Channel]
  def pagedListChannelDetails(r: ServiceResultNode): PagedList[ChannelDetails]
  def pagedListProgram(r: ServiceResultNode): PagedList[Program]
  def pagedListProgramBrief(r: ServiceResultNode): PagedList[ProgramBrief]
  def pagedListRecRuleFilter(r: ServiceResultNode): PagedList[RecRuleFilterItem]
  def pagedListRecordRule(r: ServiceResultNode): PagedList[RecordRule]
  def pagedListRecordable(r: ServiceResultNode): PagedList[Recordable]
  def pagedListRecording(r: ServiceResultNode): PagedList[Recording]
  def pagedListVideo(r: ServiceResultNode): PagedList[Video]
  def pagedListVideoMultiplex(r: ServiceResultNode): PagedList[VideoMultiplex]
  def program(r: ServiceResultNode): Program
  def recordedId(r: ServiceResultNode): RecordedId
  def recording(r: ServiceResultNode): Recording
  def recordRule(r: ServiceResultNode): RecordRule
  def recordRuleId(r: ServiceResultNode): RecordRuleId
  def settings(r: ServiceResultNode): Settings
  def soListListingSource(r: ServiceResultNode): ServicesObject[List[ListingSource]]
  def string(r: ServiceResultNode): String
  def stringMap(r: ServiceResultNode): Map[String, String]
  def timeZoneInfo(r: ServiceResultNode): TimeZoneInfo
  def uri(r: ServiceResultNode): URI
  def video(r: ServiceResultNode): Video
  def videoMultiplex(r: ServiceResultNode): VideoMultiplex
}

trait ServiceResultReaderImplicits {
  def converter: ServiceResultConverter

  implicit object ArtworkInfoListReader extends ServiceResultReader[List[ArtworkInfo]] {
    def read(r: ServiceResultNode): List[ArtworkInfo] = converter.listArtworkInfo(r)
  }

  implicit object BackendDetailsReader extends ServiceResultReader[BackendDetails] {
    def read(r: ServiceResultNode): BackendDetails = converter.backendDetails(r)
  }

  implicit object BlurayInfoReader extends ServiceResultReader[BlurayInfo] {
    def read(r: ServiceResultNode): BlurayInfo = converter.blurayInfo(r)
  }

  implicit object BooleanReader extends ServiceResultReader[Boolean] {
    def read(r: ServiceResultNode): Boolean = converter.boolean(r)
    override def defaultField = "bool"
  }

  implicit object CaptureCardReader extends ServiceResultReader[CaptureCard] {
    def read(r: ServiceResultNode): CaptureCard = converter.captureCard(r)
  }

  implicit object CaptureCardIdReader extends ServiceResultReader[CaptureCardId] {
    def read(r: ServiceResultNode): CaptureCardId = converter.captureCardId(r)
    override def defaultField = "int"
  }

  implicit object CaptureCardListReader extends ServiceResultReader[List[CaptureCard]] {
    def read(r: ServiceResultNode): List[CaptureCard] = converter.listCaptureCard(r)
  }

  implicit object ChannelPagedListReader extends ServiceResultReader[PagedList[Channel]] {
    def read(r: ServiceResultNode): PagedList[Channel] = converter.pagedListChannel(r)
  }

  implicit object ChannelDetailsReader extends ServiceResultReader[ChannelDetails] {
    def read(r: ServiceResultNode): ChannelDetails = converter.channelDetails(r)
  }

  implicit object ChannelDetailsPagedListReader extends ServiceResultReader[PagedList[ChannelDetails]] {
    def read(r: ServiceResultNode): PagedList[ChannelDetails] = converter.pagedListChannelDetails(r)
  }

  implicit object ChannelGroupListReader extends ServiceResultReader[List[ChannelGroup]] {
    def read(r: ServiceResultNode): List[ChannelGroup] = converter.listChannelGroup(r)
  }

  implicit object ConnectionInfoReader extends ServiceResultReader[ConnectionInfo] {
    def read(r: ServiceResultNode): ConnectionInfo = converter.connectionInfo(r)
  }

  implicit object FrontendStatusReader extends ServiceResultReader[FrontendStatus] {
    def read(r: ServiceResultNode): FrontendStatus = converter.frontendStatus(r)
  }

  implicit object GuideBriefReader extends ServiceResultReader[Guide[Channel, ProgramBrief]] {
    def read(r: ServiceResultNode): Guide[Channel, ProgramBrief] = converter.guideBrief(r)
  }

  implicit object GuideDetailsReader extends ServiceResultReader[Guide[ChannelDetails, Program]] {
    def read(r: ServiceResultNode): Guide[ChannelDetails, Program] = converter.guideDetails(r)
  }

  implicit object ImageSyncStatusReader extends ServiceResultReader[ImageSyncStatus] {
    def read(r: ServiceResultNode): ImageSyncStatus = converter.imageSyncStatus(r)
  }

  implicit object InputListReader extends ServiceResultReader[List[Input]] {
    def read(r: ServiceResultNode): List[Input] = converter.listInput(r)
  }

  implicit object InputIdReader extends ServiceResultReader[InputId] {
    def read(r: ServiceResultNode): InputId = converter.inputId(r)
    override def defaultField = "int"
  }

  implicit object IntReader extends ServiceResultReader[Int] {
    def read(r: ServiceResultNode): Int = converter.int(r)
    override def defaultField = "int"
  }

  implicit object KnownFrontendInfoListReader extends ServiceResultReader[List[KnownFrontendInfo]] {
    def read(r: ServiceResultNode): List[KnownFrontendInfo] = converter.listKnownFrontendInfo(r)
  }

  implicit object LineupListReader extends  ServiceResultReader[List[Lineup]] {
    def read(r: ServiceResultNode): List[Lineup] = converter.listLineup(r)
  }

  implicit object ListingSourceReader extends ServiceResultReader[ListingSource] {
    def read(r: ServiceResultNode): ListingSource = converter.listingSource(r)
  }

  implicit object ListingSourceIdReader extends ServiceResultReader[ListingSourceId] {
    def read(r: ServiceResultNode): ListingSourceId = converter.listingSourceId(r)
    override def defaultField = "int"
  }

  implicit object LiveStreamReader extends ServiceResultReader[LiveStream] {
    def read(r: ServiceResultNode): LiveStream = converter.liveStream(r)
  }

  implicit object LiveStreamListReader extends ServiceResultReader[List[LiveStream]] {
    def read(r: ServiceResultNode): List[LiveStream] = converter.listLiveStream(r)
  }

  implicit object LogMessageListReader extends ServiceResultReader[List[LogMessage]] {
    def read(r: ServiceResultNode): List[LogMessage] = converter.listLogMessage(r)
  }

  implicit object MythDateTimeReader extends ServiceResultReader[MythDateTime] {
    def read(r: ServiceResultNode): MythDateTime = converter.mythDateTime(r)
  }

  implicit object MythFileHashReader extends ServiceResultReader[MythFileHash] {
    def read(r: ServiceResultNode): MythFileHash = converter.mythFileHash(r)
  }

  implicit object ProgramReader extends ServiceResultReader[Program] {
    def read(r: ServiceResultNode): Program = converter.program(r)
  }

  implicit object ProgramPagedListReader extends ServiceResultReader[PagedList[Program]] {
    def read(r: ServiceResultNode): PagedList[Program] = converter.pagedListProgram(r)
  }

  implicit object ProgramBriefPagedListReader extends ServiceResultReader[PagedList[ProgramBrief]] {
    def read(r: ServiceResultNode): PagedList[ProgramBrief] = converter.pagedListProgramBrief(r)
  }

  implicit object RecRuleFilterListReader extends ServiceResultReader[List[RecRuleFilterItem]] {
    def read(r: ServiceResultNode): List[RecRuleFilterItem] = converter.listRecRuleFilter(r)
  }

  implicit object RecRuleFilterPagedListReader extends ServiceResultReader[PagedList[RecRuleFilterItem]] {
    def read(r: ServiceResultNode): PagedList[RecRuleFilterItem] = converter.pagedListRecRuleFilter(r)
  }

  implicit object RecordRuleReader extends ServiceResultReader[RecordRule] {
    def read(r: ServiceResultNode): RecordRule = converter.recordRule(r)
  }

  implicit object RecordRulePagedListReader extends ServiceResultReader[PagedList[RecordRule]] {
    def read(r: ServiceResultNode): PagedList[RecordRule] = converter.pagedListRecordRule(r)
  }

  implicit object RecordRuleIdReader extends ServiceResultReader[RecordRuleId] {
    def read(r: ServiceResultNode): RecordRuleId = converter.recordRuleId(r)
    override def defaultField = "uint"
  }

  implicit object RecordablePagedListReader extends ServiceResultReader[PagedList[Recordable]] {
    def read(r: ServiceResultNode): PagedList[Recordable] = converter.pagedListRecordable(r)
  }

  implicit object RecordedIdReader extends ServiceResultReader[RecordedId] {
    def read(r: ServiceResultNode): RecordedId = converter.recordedId(r)
    override def defaultField = "int"
  }

  implicit object RecordingReader extends ServiceResultReader[Recording] {
    def read(r: ServiceResultNode): Recording = converter.recording(r)
  }

  implicit object RecordingPagedListReader extends ServiceResultReader[PagedList[Recording]] {
    def read(r: ServiceResultNode): PagedList[Recording] = converter.pagedListRecording(r)
  }

  implicit object RemoteEncoderStateListReader extends ServiceResultReader[List[RemoteEncoderState]] {
    def read(r: ServiceResultNode): List[RemoteEncoderState] = converter.listRemoteEncoderState(r)
  }

  implicit object SettingsReader extends ServiceResultReader[Settings] {
    def read(r: ServiceResultNode): Settings = converter.settings(r)
  }

  implicit object SOListingSourceListReader extends ServiceResultReader[ServicesObject[List[ListingSource]]] {
    def read(r: ServiceResultNode): ServicesObject[List[ListingSource]] = converter.soListListingSource(r)
  }

  implicit object StorageGroupDirListReader extends ServiceResultReader[List[StorageGroupDir]] {
    def read(r: ServiceResultNode): List[StorageGroupDir] = converter.listStorageGroupDir(r)
  }

  implicit object StringReader extends ServiceResultReader[String] {
    def read(r: ServiceResultNode): String = converter.string(r)
    override def defaultField = "String"
  }

  implicit object StringListReader extends ServiceResultReader[List[String]] {
    def read(r: ServiceResultNode): List[String] = converter.listString(r)
    override def defaultField = "StringList"
  }

  implicit object StringMapReader extends ServiceResultReader[Map[String, String]] {
    def read(r: ServiceResultNode): Map[String, String] = converter.stringMap(r)
  }

  implicit object TimeZoneInfoReader extends ServiceResultReader[TimeZoneInfo] {
    def read(r: ServiceResultNode): TimeZoneInfo = converter.timeZoneInfo(r)
  }

  implicit object TitleInfoListReader extends ServiceResultReader[List[TitleInfo]] {
    def read(r: ServiceResultNode): List[TitleInfo] = converter.listTitleInfo(r)
  }

  implicit object UriReader extends ServiceResultReader[URI] {
    def read(r: ServiceResultNode): URI = converter.uri(r)
  }

  implicit object VideoReader extends ServiceResultReader[Video] {
    def read(r: ServiceResultNode): Video = converter.video(r)
  }

  implicit object VideoPagedListReader extends ServiceResultReader[PagedList[Video]] {
    def read(r: ServiceResultNode): PagedList[Video] = converter.pagedListVideo(r)
  }

  implicit object VideoLookupListReader extends ServiceResultReader[List[VideoLookup]] {
    def read(r: ServiceResultNode): List[VideoLookup] = converter.listVideoLookup(r)
  }

  implicit object VideoMultiplexReader extends ServiceResultReader[VideoMultiplex] {
    def read(r: ServiceResultNode): VideoMultiplex = converter.videoMultiplex(r)
  }

  implicit object VideoMultiplexPagedListReader extends ServiceResultReader[PagedList[VideoMultiplex]] {
    def read(r: ServiceResultNode): PagedList[VideoMultiplex] = converter.pagedListVideoMultiplex(r)
  }
}
