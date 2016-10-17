package mythtv
package connection
package myth

import java.util.regex.Pattern

import model._
import util.ByteCount

trait MythProtocol extends MythProtocolLike {
  def PROTO_VERSION: Int
  def PROTO_TOKEN: String
}

object MythProtocol extends MythProtocolSerializer {
  final val BACKEND_SEP: String = "[]:[]"
  final val SPLIT_PATTERN: String = Pattern.quote(BACKEND_SEP)

  // Sum type representing return values from ANN
  sealed trait AnnounceResult
  object AnnounceResult {
    final case object AnnounceAcknowledgement extends AnnounceResult
    final case class AnnounceFileTransfer(ftID: Int, size: ByteCount) extends AnnounceResult
  }

  // Sum type representing return values from QUERY_FILETRANSFER
  sealed trait QueryFileTransferResult
  object QueryFileTransferResult {
    final case object QueryFileTransferAcknowledgement extends QueryFileTransferResult
    final case class QueryFileTransferBoolean(value: Boolean) extends QueryFileTransferResult
    final case class QueryFileTransferPosition(pos: Long) extends QueryFileTransferResult
    final case class QueryFileTransferBytesTransferred(count: Int) extends QueryFileTransferResult
    final case class QueryFileTransferRequestSize(size: Long, readOnly: Boolean) extends QueryFileTransferResult
  }

  // Sum type representing return values from QUERY_RECORDER
  sealed trait QueryRecorderResult
  object QueryRecorderResult {
    final case object QueryRecorderAcknowledgement extends QueryRecorderResult
    final case class QueryRecorderBoolean(value: Boolean) extends QueryRecorderResult
    final case class QueryRecorderFrameRate(rate: Double) extends QueryRecorderResult
    final case class QueryRecorderFrameCount(frames: Long) extends QueryRecorderResult
    final case class QueryRecorderPosition(pos: Long) extends QueryRecorderResult
    final case class QueryRecorderBitrate(bitrate: Long) extends QueryRecorderResult
    final case class QueryRecorderPositionMap(map: Map[VideoPosition, Long]) extends QueryRecorderResult
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
}

//                                                          protocol version 63   // myth 0.24.x
//                                                          protocol version 72   // myth 0.25.x

private trait MythProtocol75 extends MythProtocol with MythProtocolLike75 { // myth 0.26.x
  final val PROTO_VERSION = 75
  final val PROTO_TOKEN = "SweetRock"
}

private trait MythProtocol77 extends MythProtocol with MythProtocolLike77 { // myth 0.27.x
  final val PROTO_VERSION = 77
  final val PROTO_TOKEN = "WindMark"
}

private trait MythProtocol88 extends MythProtocol with MythProtocolLike88 { // myth 0.28.x
  final val PROTO_VERSION = 88
  final val PROTO_TOKEN = "XmasGift"
}
