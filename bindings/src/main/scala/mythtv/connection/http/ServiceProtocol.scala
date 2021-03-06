// SPDX-License-Identifier: LGPL-2.1-only
/*
 * ServiceProtocol.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package http

import scala.util.{ Failure, Success, Try }

import util.{ OptionalCount, OptionalCountSome, PagedList }
import services.{ Service, ServiceEndpoint, ServiceResult, ServicesObject }
import Service.ServiceFailure.ServiceFailureThrowable

private[http] trait LabelValue {
  def label: String
  def value: String
}

trait ServiceResultNode extends Any

trait ServiceResultReader[T] {
  def read(r: ServiceResultNode): T
  def defaultField: String = ""
}

/* Top level object will contain a field for the list,
 *    e.g. RecRuleList or ProgramList, etc.
 *
 *  This List object will then contain fields:
 *
 "AsOf": "2016-10-23T06:06:17Z",
 "Count": "171",
 "StartIndex": "0",
 "TotalAvailable": "171",
 "ProtoVer": "77",
 "Version": "0.27.20140323-1"
 *
 *  plus a field for the objects, e.g.
 *
 "Programs": [ ... ]
 *
 *
 * Not all of the "*List" objects follow, this pattern.
 * Exceptions include:
 *    StringList, StorageGroupDirList, ...
 */
private[http] trait ServicesPagedList[+T] extends PagedList[T] with ServicesObject[List[T]] {
  final def items: List[T] = data
}


/*
 *  NB some requests are POST requests while others are GET
 *     split up somehow into different operation sets (mutable vs immutable) (query vs action?)
 *
 *  See:
 *    https://www.mythtv.org/wiki/Services_API
 *    https://www.mythtv.org/wiki/API_parameters_0.27
 *    https://www.mythtv.org/wiki/API_parameters_0.28
 *
 *   Use WSDL to discover services at run time?
 */

/*
 * Myth/
 *   GetHostName           GET ==> { String }                ()
 *   GetHosts              GET ==> { StringList }            ()
 *   GetKeys               GET ==> { StringList }            ()
 *   GetSetting            GET ==> { SettingList }           (HostName AND/OR Key) [Default]
 *   GetConnectionInfo     GET ==>                           (Pin)
 *   GetStorageGroupDirs   GET ==> { StorageGroupDirList }   [GroupName, HostName]
 *   GetTimeZone           GET ==> { TimeZoneInfo }          ()
 *   SendMessage           POST ==>                          (Address, Message, Timeout, udpPort...)
 *   AddStorageGroupDir    POST
 *   RemoveStorageGroupDir POST
 *   GetLogs                                                 ???
 *   PutSetting
 *   ChangePassword        POST
 *   TestDBSettings
 *   SendNotification
 *   BackupDatabase
 *   CheckDatabase
 *   ProfileSubmit
 *   ProfileDelete
 *   ProfileURL
 *   ProfileUpdated
 *   ProfileText
 * Guide/
 *   GetProgramGuide       GET ==> { ProgramGuide }          (StartTime, EndTime)[StartChanId, NumChannels, Details]
 *   GetProgramDetails     GET ==> { Program }               (ChanId, StartTime)
 *   GetChannelIcon        [ DataStream ]
 * Dvr/
 *   GetRecorded           GET ==> { Program }               (ChanId, StartTime) or (RecordedId) {0.28+}
 *   GetRecordedList       GET ==> { ProgramList }           [Count, StartIndex, Descending]
 *   GetExpiringList       GET ==> { ProgramList }           [Count, StartIndex]
 *   GetUpcomingList       GET ==> { ProgramList }           [Count, StartIndex, ShowAll]
 *   GetConflictList       GET ==> ??                        [Count, StartIndex]
 *   GetEncoderList        GET ==> { EncoderList }           ()
 *   GetRecordScheduleList GET ==> { RecRuleList }           [Count, StartIndex]
 *   GetRecordSchedule     GET ==> { RecRule }               (RecordId) [Template] [ChanId,StartTime] [MakeOverride]
 *   RemoveRecorded        POST
 *   GetRecGroupList       GET ==> { StringList }            ()
 *   GetTitleList          GET ==> { StringList }            ()
 *   GetTitleInfoList      GET ==> { TitleInfoList }         ()
 *   AddRecordSchedule            POST
 *   UpdateRecordSchedule         POST
 *   RemoveRecordSchedule         POST                       (RecordId)
 *   EnableRecordSchedule         POST                       (RecordId)
 *   DisableRecordSchedule        POST                       (RecordId)
 *   UpdateRecordedWatchedStatus  POST                       (ChanId, StartTime, Watched)
 * Video/
 *   GetVideo              GET ==> { VideoMetadataInfo }     (Id)
 *   GetVideoByFileName    GET ==> { VideoMetadataInfo }     (FileName)
 *   GetVideoList          GET ==> { VideoMetadataInfoList } [Count, StartIndex, Descending]
 *   LookupVideo           GET ==> { VideoLookupList }       [Title, Subtitle, Inetref] [(Season, Episode)]
 *   GetBluray
 *   RemoveVideoFromDB
 *   AddVideo
 *   UpdateVideoWatchedStatus
 *      **** LookupVideo retrieves metadata from the Internet rather than the Myth server?
 * Content/
 *   GetPreviewImage       GET ==> <image>                   (ChanId, StartTime)[Width, Height, SecsIn]
 *   GetFileList           GET ==> { StringList }            (StorageGroup)
 *   GetHash               GET ==> { String }                (StorageGroup, FileName)
 *   GetFile               GET ==> <filedata>                (StorageGroup, FileName)
 *   GetImageFile          GET ==> <image>                   (StorageGroup, FileName)[Width, Height]
 *   GetMusic                 GET ==> <music>                (Id)
 *   GetRecordingArtwork      GET ==> <image>                (Type, Inetref, Season)[Width, Height]
 *   GetRecordingArtworkList  GET ==> { ArtworkInfoList }    (ChanId, StartTime)
 *   GetProgramArtworkList    GET ==>                        (Inetref, Season)
 *   GetVideoArtwork          GET ==> <image>                (Type, Id)[Width, Height]
 *   GetAlbumArt              GET ==> <image>                (Id)[Width, Height]
 *   GetRecording             GET ==> <video>                (ChanId, StartTime)
 *   GetVideo                 GET ==> <video>                (Id)
 *   DownloadFile
 *   GetLiveStream
 *   GetLiveStreamList
 *   AddLiveStream
 *   AddRecordingLiveStream
 *   AddVideoLiveStream
 *   StopLiveStream
 *   RemoveLiveStream
 * Capture/
 *   GetCaptureCard        GET ==> { CaptureCard }           (CardId)
 *   GetCaptureCardList    GET ==> { CaptureCardList }       [HostName, CardType]
 *   RemoveCaptureCard     POST
 *   AddCaptureCard        POST
 *   UpdateCaptureCard     POST
 *   RemoveCardInput       POST
 *   AddCardInput          POST
 *   UpdateCardInput       POST
 * Channel/
 *   GetChannelInfo        GET ==> { ChannelInfo }           (ChanID)
 *   GetChannelInfoList    GET ==> { ChannelInfoList }       (SourceID)
 *   GetVideoSource        GET ==> { VideoSource }           (SourceID)
 *   GetVideoSourceList    GET ==> { VideoSourceList }       ()
 *   GetVideoMultiplex     GET ==> { VideoMultiplex }        (MplexID)
 *   GetVideoMultiplexList GET ==> { VideoMultiplexList }    (SourceID)[StartIndex, Count]
 *   GetDDLineupList       POST ?                            (Source, UserId, Password)
 *   FetchChannelsFromSource  POST?                          (SourceId, CardId, WaitForFinish)
 *   GetXMLTVIdList        GET ==> { StringList }            (SourceID)
 *   UpdateDBChannel       POST
 *   AddDBChannel          POST
 *   RemoveDBChannel       POST
 *   UpdateVideoSource     POST
 *   AddVideoSource        POST
 *   RemoveVideoSource     POST
 *
 * Frontend/
 *   GetActionList         GET ==> { FrontendActionList }    [Context]
 *   GetContextList        GET ==> { StringList }            ()
 *   GetStatus             GET ==> { FrontendStatus }        ()
 *
 *   SendMessage           POST ==> Boolean                  (Message)[Timeout]
 *        Timeout only applies during playback? Unit is seconds, valid range (0, 1000) exclusive
 *   SendNotification      POST ==> Boolean                  (Message)[lots of optional params]
 *
 *   SendAction
 *
 *   PlayRecording         POST ==> Boolean                  (ChanId, StartTime)
 *   PlayVideo             POST ==> Boolean                  (Id)[UseBookmark]
 */

/* How is GetCaptureCardList different from GetEncoderList?
   Answer: CaptureCardList is much more detailed information; aligns with the capturecard database table.
           EncoderList is higher level information (id/host/port + state).
           They are related by common CaptureCardId.
 */

trait ServiceProtocol extends ServiceResultReaderImplicits {
  self: Service =>

  def connection: AbstractHttpConnection

  def endpoints: Map[String, ServiceEndpoint] = {
    val wsdlUrl = connection.url(buildPath("wsdl"))
    (WsdlReader(wsdlUrl).endpoints map (p => (p.name, p))).toMap
  }

  def request[T: ServiceResultReader](endpoint: String, params: Map[String, Any] = Map.empty)
    (level0Field: String = "", level1Field: String = ""): ServiceResult[T]

  def post[T: ServiceResultReader](endpoint: String, params: Map[String, Any] = Map.empty)
    (level0Field: String = "", level1Field: String = ""): ServiceResult[T]

  @annotation.nowarn("cat=other-match-analysis")
  def requestStream(endpoint: String, params: Map[String, Any] = Map.empty): HttpStreamResponse =
    connection.request(buildPath(endpoint, params)) match { case r: HttpStreamResponse => r }

  def streamResponse[U](response: HttpStreamResponse, f: HttpStreamResponse => U): Unit = {
    try f(response)
    finally response.stream.close()
  }

  def buildPath(endpoint: String, params: Map[String, Any] = Map.empty): String =
    buildPath(serviceName, endpoint, params)

  def buildPath(service: String, endpoint: String, params: Map[String, Any]): String = {
    val builder = new StringBuilder("/") ++= service += '/' ++= endpoint
    if (params.nonEmpty) {
      builder += '?'
      AbstractHttpConnection.encodeParameters(params, builder)
    }
    builder.toString
  }

  def buildStartCountParams(startIndex: Int, count: OptionalCount[Int]): Map[String, Any] = {
    var params: Map[String, Any] = count match {
      case OptionalCountSome(n) => Map("Count" -> n)
      case _ => Map.empty
    }
    if (startIndex != 0) params += "StartIndex" -> startIndex
    params
  }

  import scala.language.implicitConversions
  implicit def try2Result[T](t: Try[T]): ServiceResult[T] = t match {
    case Success(value) => Right(value)
    case Failure(ex) => Left(ServiceFailureThrowable(ex))
  }
}
