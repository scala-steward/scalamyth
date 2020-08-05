// SPDX-License-Identifier: LGPL-2.1-only
/*
 * DvrService.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package services

import model._
import EnumTypes.{ DupCheckIn, DupCheckMethod, RecStatus, RecType }
import util.{ MythDateTime, OptionalCount, PagedList }

trait DvrService extends BackendService {
  final def serviceName: String = "Dvr"

  def getRecorded(chanId: ChanId, startTime: MythDateTime): ServiceResult[Recording]
  def getRecorded(recordedId: RecordedId): ServiceResult[Recording]

  // For MythTV 0.28, getRecordedList gains parameters: TitleRegEx, RecGroup, StorageGroup
  def getRecordedList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all,
    descending: Boolean = false,
    titleRegex: String = "",
    recGroup: String = "",
    storageGroup: String = ""
  ): ServiceResult[PagedList[Recording]]

  def getExpiringList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all
  ): ServiceResult[PagedList[Recording]]

  // for 0.28 getUpcomingList gains parameters: recordRuleId, recStatus
  def getUpcomingList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all,
    showAll: Boolean = false,
    recordRuleId: RecordRuleId = RecordRuleId(0),
    recStatus: RecStatus = RecStatus.Unknown
  ): ServiceResult[PagedList[Recordable]]

  // for 0.28 getConflictList gains RecordId parameter
  def getConflictList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all,
    recordRuleId: RecordRuleId = RecordRuleId(0)
  ): ServiceResult[PagedList[Recordable]]

  def getEncoderList: ServiceResult[List[RemoteEncoderState]]

  def getRecordScheduleList(
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all,
    sortBy: String = "",    // Title is the default
    descending: Boolean = false
  ): ServiceResult[PagedList[RecordRule]]

  // for MythTV 0.28 getRecordSchedule adds parameters: Template, RecordedId, ChanId, StartTime, MakeOverride
  def getRecordSchedule(recordId: RecordRuleId): ServiceResult[RecordRule]
  def getRecordSchedule(recordedId: RecordedId): ServiceResult[RecordRule]
  def getRecordSchedule(template: String): ServiceResult[RecordRule]

  // NB getRecordSchedule(ChanId, StartTime) loads from program table, should not be used with recordings
  def getRecordSchedule(chanId: ChanId, startTime: MythDateTime, makeOverride: Boolean = false): ServiceResult[RecordRule]

  def getRecGroupList: ServiceResult[List[String]]

  // for 0.28 getTitleList gains a RecGroup parameter
  def getTitleList: ServiceResult[List[String]] = getTitleList("")
  def getTitleList(recGroup: String): ServiceResult[List[String]]

  def getTitleInfoList: ServiceResult[List[TitleInfo]]

  // getOldRecordedList is new for MythTV 29
  def getOldRecordedList(
    title: String = "",
    seriesId: String = "",
    recordRuleId: RecordRuleId = RecordRuleId(0),
    startTime: MythDateTime = MythDateTime.empty,
    endTime: MythDateTime = MythDateTime.empty,
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all,
    sortBy: String = "",    // starttime is the default (title is other option)
    descending: Boolean = false
  ): ServiceResult[PagedList[Recording]]

  // recordedIdForPathname is new for MythTV 29
  def recordedIdForPathname(pathName: String): ServiceResult[RecordedId]

  // getRecordedSeek* is new for MythTV 29
  def getRecordedSeekBytes(recordedId: RecordedId): ServiceResult[List[RecordedSeekBytes]]
  def getRecordedSeekMs(recordedId: RecordedId): ServiceResult[List[RecordedSeekMilliseconds]]

  // getSavedBookmark* is new for MythTV 29
  def getSavedBookmark(recordedId: RecordedId): ServiceResult[VideoPositionFrame]
  def getSavedBookmark(chanId: ChanId, startTime: MythDateTime): ServiceResult[VideoPositionFrame]

  def getSavedBookmarkMilliseconds(recordedId: RecordedId): ServiceResult[VideoPositionMilliseconds]
  def getSavedBookmarkMilliseconds(chanId: ChanId, startTime: MythDateTime): ServiceResult[VideoPositionMilliseconds]

  def getSavedBookmarkByteOffset(recordedId: RecordedId): ServiceResult[VideoPositionBytes]
  def getSavedBookmarkByteOffset(chanId: ChanId, startTime: MythDateTime): ServiceResult[VideoPositionBytes]

  /* mutating POST methods */

  // for 0.28, removeRecorded gains RecordedId, ForceDelete, AllowRerecord parameters
  def removeRecorded(chanId: ChanId, startTime: MythDateTime): ServiceResult[Boolean] =
    removeRecorded(chanId, startTime, forceDelete = false, allowReRecord = false)

  def removeRecorded(chanId: ChanId, startTime: MythDateTime, forceDelete: Boolean, allowReRecord: Boolean): ServiceResult[Boolean]

  def removeRecorded(recordedId: RecordedId, forceDelete: Boolean = false, allowReRecord: Boolean = false): ServiceResult[Boolean]

  def deleteRecording(chanId: ChanId, startTime: MythDateTime): ServiceResult[Boolean] =
    deleteRecording(chanId, startTime, forceDelete = false, allowReRecord = false)

  def deleteRecording(chanId: ChanId, startTime: MythDateTime, forceDelete: Boolean, allowReRecord: Boolean): ServiceResult[Boolean]

  def deleteRecording(recordedId: RecordedId, forceDelete: Boolean = false, allowReRecord: Boolean = false): ServiceResult[Boolean]

  def undeleteRecording(chanId: ChanId, startTime: MythDateTime): ServiceResult[Boolean]

  def undeleteRecording(recordedId: RecordedId): ServiceResult[Boolean]

  // stopRecording is new for MythTV 29
  def stopRecording(recordedId: RecordedId): ServiceResult[Boolean]

  // reactivateRecording is new for MythTV 29
  def reactivateRecording(recordedId: RecordedId): ServiceResult[Boolean]

  def addRecordSchedule(rule: RecordRule): ServiceResult[RecordRuleId]

  def updateRecordSchedule(rule: RecordRule): ServiceResult[Boolean]

  def removeRecordSchedule(recordId: RecordRuleId): ServiceResult[Boolean]

  def disableRecordSchedule(recordId: RecordRuleId): ServiceResult[Boolean]

  def enableRecordSchedule(recordId: RecordRuleId): ServiceResult[Boolean]

  // addDontRecordSchedule is new in MythTV 0.28
  def addDontRecordSchedule(chanId: ChanId, startTime: MythDateTime, neverRecord: Boolean = false): ServiceResult[Boolean]

  /* Added to API on 6 Apr 2016 */
  def updateRecordedWatchedStatus(chanId: ChanId, startTime: MythDateTime, watched: Boolean): ServiceResult[Boolean]
  def updateRecordedWatchedStatus(recordedId: RecordedId, watched: Boolean): ServiceResult[Boolean]

  // setSavedBookmark is new for MythTV 29
  def setSavedBookmark(recordedId: RecordedId, pos: VideoPosition): ServiceResult[Boolean]
  def setSavedBookmark(chanId: ChanId, startTime: MythDateTime, pos: VideoPosition): ServiceResult[Boolean]

  // getInputList is new for MythTV 0.28
  def getInputList: ServiceResult[List[Input]]

  // getRecStorageGroupList is new for MythTV 0.28
  def getRecStorageGroupList: ServiceResult[List[String]]

  // getPlayGroupList is new for MythTV 0.28
  def getPlayGroupList: ServiceResult[List[String]]

  // rescheduleRecordings is new for MythTV 29
  def rescheduleRecordings(): ServiceResult[Boolean]

  // getRecordedCommBreak* is new for MythTV 0.28
  def getRecordedCommBreak(recordedId: RecordedId): ServiceResult[List[VideoSegmentFrames]]
  def getRecordedCommBreakMs(recordedId: RecordedId): ServiceResult[List[VideoSegmentMilliseconds]]
  def getRecordedCommBreakBytes(recordedId: RecordedId): ServiceResult[List[VideoSegmentBytes]]

  def getRecordedCommBreak(chanId: ChanId, startTime: MythDateTime): ServiceResult[List[VideoSegmentFrames]]
  def getRecordedCommBreakMs(chanId: ChanId, startTime: MythDateTime): ServiceResult[List[VideoSegmentMilliseconds]]
  def getRecordedCommBreakBytes(chanId: ChanId, startTime: MythDateTime): ServiceResult[List[VideoSegmentBytes]]

  // getRecordedCutList* is new for MythTV 0.28
  def getRecordedCutList(recordedId: RecordedId): ServiceResult[List[VideoSegmentFrames]]
  def getRecordedCutListMs(recordedId: RecordedId): ServiceResult[List[VideoSegmentMilliseconds]]
  def getRecordedCutListBytes(recordedId: RecordedId): ServiceResult[List[VideoSegmentBytes]]

  def getRecordedCutList(chanId: ChanId, startTime: MythDateTime): ServiceResult[List[VideoSegmentFrames]]
  def getRecordedCutListMs(chanId: ChanId, startTime: MythDateTime): ServiceResult[List[VideoSegmentMilliseconds]]
  def getRecordedCutListBytes(chanId: ChanId, startTime: MythDateTime): ServiceResult[List[VideoSegmentBytes]]

  /* Enumeration services -- new for MythTV 0.28 */

  def getRecRuleFilterList: ServiceResult[List[RecRuleFilterItem]]

  def recStatusToString(recStatus: RecStatus): ServiceResult[String]
  def recStatusToDescription(recStatus: RecStatus, recType: RecType, recStartTs: MythDateTime): ServiceResult[String]

  def recTypeToString(recType: RecType): ServiceResult[String]
  def recTypeToDescription(recType: RecType): ServiceResult[String]

  def dupMethodToString(dupMethod: DupCheckMethod): ServiceResult[String]
  def dupMethodToDescription(dupMethod: DupCheckMethod): ServiceResult[String]

  def dupInToString(dupIn: DupCheckIn): ServiceResult[String]
  def dupInToDescription(dupIn: DupCheckIn): ServiceResult[String]
}
