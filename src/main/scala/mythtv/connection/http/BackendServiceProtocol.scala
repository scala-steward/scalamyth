package mythtv
package connection
package http

import services.Service
import util.{ OptionalCount, OptionalCountSome }

trait MythServiceProtocol {
  self: Service =>
  def buildPath(endpoint: String, params: Map[String, Any] = Map.empty): String = {
    val b = new StringBuilder("/") ++= serviceName += '/' ++= endpoint
    if (params.nonEmpty) {
      // TODO URL escaping
      params.iterator.map { case (k, v) => k + "=" + v }.addString(b, "?", "&", "")
    }
    b.toString
  }

  def buildStartCountParams(startIndex: Int, count: OptionalCount[Int]): Map[String, Any] = {
    var params: Map[String, Any] = count match {
      case OptionalCountSome(n) => Map("Count" -> n)
      case _ => Map.empty
    }
    if (startIndex != 0) params += "StartIndex" -> startIndex
    params
  }
}

trait BackendServiceProtocol extends MythServiceProtocol {
  self: Service =>
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
   *   AddRecordSchedule
   *   UpdateRecordSchedule
   *   RemoveRecordSchedule
   *   EnableRecordSchedule
   *   DisableRecordSchedule
   *   UpdateRecordedWatchedStatus
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
   */

  /* How is GetCaptureCardList different from GetEncoderList?
     Answer: CaptureCardList is much more detailed information; aligns with the capturecard database table.
             EncoderList is higher level information (id/host/port + state).
             They are related by common CaptureCardId.
   */
}

trait FrontendServiceProtocol extends MythServiceProtocol {
  self: Service =>
  /*
   * Frontend/
   *   GetActionList         GET ==> { FrontendActionList }    ()
   *   GetContextList        GET ==> { StringList }            ()
   *   GetStatus             GET ==> { FrontendStatus }        ()
   */
}
