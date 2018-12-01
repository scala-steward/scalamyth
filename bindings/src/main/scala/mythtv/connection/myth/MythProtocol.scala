// SPDX-License-Identifier: LGPL-2.1-only
/*
 * MythProtocol.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth

import scala.util.matching.Regex

import model._
import util.ByteCount
import model.EnumTypes.{ RecStatus, SleepStatus, TvState }

trait MythProtocol extends MythProtocolLike {
  def ProtocolVersion: Int
  def ProtocolToken: String
}

object MythProtocol extends MythProtocolSerializer {
  final val Separator: String = "[]:[]"
  final val SplitPattern: String = Regex.quote(Separator)

  sealed trait MythProtocolFailure
  object MythProtocolFailure {
    case object MythProtocolNoResult extends MythProtocolFailure
    case object MythProtocolFailureUnknown extends MythProtocolFailure
    final case class MythProtocolFailureMessage(message: String) extends MythProtocolFailure
    final case class MythProtocolFailureThrowable(throwable: Throwable) extends MythProtocolFailure
  }

  // Sum type representing return values from ANN
  sealed trait AnnounceResult
  object AnnounceResult {
    case object AnnounceAcknowledgement extends AnnounceResult
    final case class AnnounceFileTransfer(ftID: FileTransferId, size: ByteCount, checkFiles: Seq[String]) extends AnnounceResult
  }

  // Sum type representing return values from IMAGE_SCAN
  sealed trait ImageScanResult
  object ImageScanResult {
    case object ImageScanAcknowledgement extends ImageScanResult
    final case class ImageScanProgress(isBackend: Boolean, progressCount: Int, totalCount: Int) extends ImageScanResult
  }

  // Sum type representing return values from QUERY_FILETRANSFER
  sealed trait QueryFileTransferResult
  object QueryFileTransferResult {
    case object QueryFileTransferAcknowledgement extends QueryFileTransferResult
    final case class QueryFileTransferBoolean(value: Boolean) extends QueryFileTransferResult
    final case class QueryFileTransferPosition(pos: Long) extends QueryFileTransferResult
    final case class QueryFileTransferBytesTransferred(count: Int) extends QueryFileTransferResult
    final case class QueryFileTransferRequestSize(size: Long, readOnly: Boolean) extends QueryFileTransferResult
  }

  // Sum type representing return values from QUERY_RECORDER
  sealed trait QueryRecorderResult
  object QueryRecorderResult {
    case object QueryRecorderAcknowledgement extends QueryRecorderResult
    final case class QueryRecorderBoolean(value: Boolean) extends QueryRecorderResult
    final case class QueryRecorderFrameRate(rate: Double) extends QueryRecorderResult
    final case class QueryRecorderFrameCount(frames: Long) extends QueryRecorderResult
    final case class QueryRecorderPosition(pos: Long) extends QueryRecorderResult
    final case class QueryRecorderBitrate(bitrate: Long) extends QueryRecorderResult
    final case class QueryRecorderPositionMap(map: Map[VideoPositionFrame, Long]) extends QueryRecorderResult
    final case class QueryRecorderRecording(recording: Recording) extends QueryRecorderResult
    final case class QueryRecorderCardList(cards: List[CaptureCardId]) extends QueryRecorderResult
    final case class QueryRecorderCardInputList(inputs: List[CardInput]) extends QueryRecorderResult
    final case class QueryRecorderInput(input: String) extends QueryRecorderResult
    final case class QueryRecorderPictureAttribute(value: Int) extends QueryRecorderResult
    final case class QueryRecorderCheckChannelPrefix(matched: Boolean, cardId: Option[CaptureCardId],
      extraCharUseful: Boolean, spacer: String) extends QueryRecorderResult
    final case class QueryRecorderChannelInfo(channel: Channel) extends QueryRecorderResult
    final case class QueryRecorderNextProgramInfo(program: UpcomingProgram) extends QueryRecorderResult
  }

  // Sum type representing return values from QUERY_REMOTEENCODER
  sealed trait QueryRemoteEncoderResult
  object QueryRemoteEncoderResult {
    case object QueryRemoteEncoderAcknowledgement extends QueryRemoteEncoderResult
    final case class QueryRemoteEncoderBitrate(bitrate: Long) extends QueryRemoteEncoderResult
    final case class QueryRemoteEncoderBoolean(value: Boolean) extends QueryRemoteEncoderResult
    final case class QueryRemoteEncoderCardInputList(inputs: List[CardInput]) extends QueryRemoteEncoderResult
    final case class QueryRemoteEncoderFlags(flags: Int) extends QueryRemoteEncoderResult
    final case class QueryRemoteEncoderRecording(recording: Recording) extends QueryRemoteEncoderResult
    final case class QueryRemoteEncoderRecStatus(status: RecStatus) extends QueryRemoteEncoderResult
    final case class QueryRemoteEncoderSleepStatus(status: SleepStatus) extends QueryRemoteEncoderResult
    final case class QueryRemoteEncoderState(state: TvState) extends QueryRemoteEncoderResult
    final case class QueryRemoteEncoderTunedInputInfo(busy: Boolean, input: Option[CardInput], chanId: Option[ChanId]) extends QueryRemoteEncoderResult
  }
}

//                                                    protocol version 63   // myth 0.24.x
//                                                    protocol version 72   // myth 0.25.x

private trait MythProtocol75 extends MythProtocol with MythProtocolLike75 { // myth 0.26.x
  final val ProtocolVersion = 75
  final val ProtocolToken = "SweetRock"
}

private trait MythProtocol77 extends MythProtocol with MythProtocolLike77 { // myth 0.27.x
  final val ProtocolVersion = 77
  final val ProtocolToken = "WindMark"
}

private trait MythProtocol88 extends MythProtocol with MythProtocolLike88 { // myth 0.28.x
  final val ProtocolVersion = 88
  final val ProtocolToken = "XmasGift"
}

private trait MythProtocol91 extends MythProtocol with MythProtocolLike91 { // myth 29
  final val ProtocolVersion = 91
  final val ProtocolToken = "BuzzOff"
}

/*
 * History of Myth Protocol version changes:
 *
 * Ver   Git Commit   Notes
 * ====  ===========  =========================================================================
 *  75*  1f8c5902107
 *  76   d4dcff374e4  Add syndicatedEpisode, partNumber, partTotal to ProgramInfo
 *  77*  49dbed5be07  Add command QUERY_RECORDER FILL_DURATION_MAP; VideoRate and DurationMs to Markup enum
 *  78   e8bfd99e214  Add totalEpisodes to ProgramInfo
 *  79   3af71b4f758  Add categoryType to ProgramInfo
 *       5f47d6922e8  Add QUERY_FILETRANSFER REQUEST_SIZE command
 *  80   3ee65b3b145  MythMusic changes in prior commits:
 *       8aefe1bcf0f    add SCAN_MUSIC command
 *       e6c8a78685e    add MUSIC_TAG_GETIMAGE command
 *       2f1a5350be3    add MUSIC_TAG_UPDATE_VOLATILE command
 *       d9217461fff    add MUSIC_TAG_UPDATE_METADATA command
 *       d0185093e95    add MUSIC_FIND_ALBUMART command
 *       b257f3c8608    add MUSIC_CALC_TRACK_LENGTH command
 *       e350c8fb09d    add MUSIC_TAG_ADDIMAGE command
 *       b9d40e4075a    add MUSIC_TAG_REMOVEIMAGE command
 *       efafb148fa8    add MUSIC_TAG_CHANGEIMAGE command
 *       41580ebaef2  add optional allowFallback parameter to QUERY_SG_FILEQUERY
 *  81   cd866684857  ??? chanId added to InputInfo ?
 *  82   e8a99d45c3c  add recordedId to ProgramInfo
 *       124a55dcece  SendErrorResponse to socket on bad protocol command
 *  83   1dab1907956  add a QUERY_FINDFILE command to the protocol
 *       fdaf14dd4cf  first part of new image gallery rewrite, adds lots of new IMAGE_ commands
 *  84   b220116f77e  recordId changes; affects events: RECORDING_LIST_CHANGE (ADD|DELETE), UPDATE_FILE_SIZE,
 *                    UPDATE_MASTER_PROG_INFO - renamed to UPDATE_MASTER_REC_INFO
 *  85   a4f65ce15ba  new ANN Frontend
 *  86   bcd7d65ef74  add inputName, bookmarkUpdate to ProgramInfo serialization
 *       e845eae3e84  big mythgallery changes/rewrite using storage groups, etc.
 *       6657e1a4ec5  change vector excluded_card[id]s to scalar excluded_input
 *       d2736963115  add GET_FREE_INPUT_INFO
 *       189a7be2a32  remove GET_FREE_RECORDER, GET_FREE_RECORDER_COUNT, GET_FREE_RECORDER_LIST, GET_NEXT_FREE_RECORDER,
 *                    QUERY_RECORDER/GET_FREE_INPUTS, QUERY_REMOTE_ENCODER/GET_FREE_INPUTS (all replaced by GET_FREE_INPUT_INFO)
 *       a2676c2dd2a  add MUSIC_LYRICS_FIND and MUSIC_LYRICS_GETGRABBERS
 *       -----------
 *  87   c4f8b270f16  due to prior changes; conversion of cardId to inputId (many/all places?)
 *       ca71fcd28f9  <-- changes start with this commit?
 *       -----------
 *       1971440f8df  update MUSIC_LYRICS_FIND
 *       20603add41a  add MUSIC_LYRICS_SAVE
 *       096b94929c7  add MOVE_FILE
 *  88*  6f6a5914c63  add LyricsView to MythMusic; unsure of impact on protocol -- event changes?
 *  89   1f8c275b814  add reclimit to InputInfo
 *  90   3e782baea08  add reccount to InputInfo
 *  91*  0cd367d7a5b  revert reclimit/reccount changes
 */
