package mythtv
package connection
package http

trait BackendServiceProtocol {
  /*
   *  NB some requests are POST requests while others are GET
   *     split up somehow into different operation sets (mutable vs immutable) (query vs action?)
   *
   *  See:
   *    https://www.mythtv.org/wiki/Services_API
   *    https://www.mythtv.org/wiki/API_parameters_0.27
   *    https://www.mythtv.org/wiki/API_parameters_0.28
   *
   *   Use WDSL to discover services at run time?
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
   * Guide/
   *   GetProgramGuide       GET ==> { ProgramGuide }
   *   GetProgramDetails     ???
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
   * Video/
   *   GetVideo              GET ==> { VideoMetadataInfo }     (Id)
   *   GetVideoByFileName    GET ==> { VideoMetadataInfo }     (FileName)
   *   GetVideoList          GET ==> { VideoMetadataInfoList } [Count, StartIndex, Descending]
   *   LookupVideo           GET ==> { VideoLookupList }       [Title, Subtitle, Inetref] [(Season, Episode)]
   *      **** LookupVideo retrieves metadata from the Internet rather than the Myth server?
   * Content/
   *   GetPreviewImage       [ DataStream ]
   *   GetFileList           GET ==> { StringList }            (StorageGroup)
   *   GetHash               GET ==> { String }                (StorageGroup, FileName)
   * Capture/
   *   GetCaptureCard        GET ==> { CaptureCard }           (CardId)
   *   GetCaptureCardList    GET ==> { CaptureCardList }       [HostName, CardType]
   * Channel/
   *   GetChannelInfo        GET ==> { ChannelInfo }           (ChanID)
   *   GetChannelInfoList    GET ==> { ChannelInfoList }       (SourceID)
   *   GetVideoSource        GET ==> { VideoSource }           (SourceID)
   *   GetVideoSourceList    GET ==> { VideoSourceList }       ()
   *   GetXMLTVIdList        GET ==> { StringList }            (SourceID)
   */

  /* How is GetCaptureCardList different from GetEncoderList?
     Answer: CaptureCardList is much more detailed information; aligns with the capturecard database table.
             EncoderList is higher level information (id/host/port + state).
             They are related by common CaptureCardId.
   */
}

trait FrontendServiceProtocol {
  /*
   * Frontend/
   *   GetActionList         GET ==> { FrontendActionList }    ()
   *   GetContextList        GET ==> { StringList }            ()
   *   GetStatus             GET ==> { FrontendStatus }        ()
   */
}
