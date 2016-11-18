package mythtv
package connection
package myth

import java.util.regex.Pattern

import model._
import util.ByteCount
import model.EnumTypes.{ RecStatus, SleepStatus, TvState }

trait MythProtocol extends MythProtocolLike {
  def ProtocolVersion: Int
  def ProtocolToken: String
}

object MythProtocol extends MythProtocolSerializer {
  final val BackendSeparator: String = "[]:[]"
  final val SplitPattern: String = Pattern.quote(BackendSeparator)

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
    final case class AnnounceFileTransfer(ftID: FileTransferId, size: ByteCount) extends AnnounceResult
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

//                                                          protocol version 63   // myth 0.24.x
//                                                          protocol version 72   // myth 0.25.x

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
