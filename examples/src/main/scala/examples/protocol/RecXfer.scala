// SPDX-License-Identifier: LGPL-2.1-only
/*
 * RecXfer.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package examples.protocol

import mythtv.connection.myth._
import mythtv.model.{ CaptureCardId, ChanId, RecordedId, Recording }
import mythtv.util.{ ByteCount, MythDateTime }

import MythProtocol.MythProtocolFailure
import RecordedId.{ RecordedIdChanTime, RecordedIdInt }

object RecXfer extends SampleProgram with Xfer {
  import SampleProgram._

  def doTransfer(api: MythProtocolAPIConnection, rec: Recording): ByteCount = {
    val ft = RecordingTransferChannel(api, rec)
    val verbose = options contains OptVerbose
    doTransfer(ft, verbose)
  }

  sealed trait RecordingSource
  final case class RecordingSourceId(id: RecordedId) extends RecordingSource
  final case class RecordingSourceInput(id: CaptureCardId) extends RecordingSource

  final val OptRecording = "recording"
  var options: OptionMap = Map.empty

  class RecXferArgParser extends ArgParser {
    override def accumulateOptions(opts: OptionMap, args: List[String]): OptionMap = args match {
      case Nil                       => opts
      case arg :: _ if isOption(arg) => super.accumulateOptions(opts, args)
      case spec :: cdr               => accumulateOptions(opts ++ recording(spec), cdr)
      case _                         => super.accumulateOptions(opts, args)
    }

    def recording(spec: String): OptionMap = Map(OptRecording -> parseRecordingSpec(spec))

    final val InputPattern = """input:(\d+)""".r
    final val RecIdPattern = """recid:(\d+)""".r
    final val RecChanTimePattern = """rec:(\d{4})_(\d{14})""".r

    @annotation.nowarn("cat=other-match-analysis")
    def parseRecordingSpec(spec: String): RecordingSource = spec match {
      case InputPattern(captureCardId) => RecordingSourceInput(CaptureCardId(captureCardId.toInt))
      case RecIdPattern(recordedId) => RecordingSourceId(RecordedIdInt(recordedId.toInt))
      case RecChanTimePattern(chanId, recStartTs) => RecordingSourceId(
        RecordedIdChanTime(ChanId(chanId.toInt), MythDateTime.fromMythFormat(recStartTs)))
    }
  }

  def getRecordingBySource(api: MythProtocolAPI, recSource: RecordingSource): MythProtocolResult[Recording] = {
    recSource match {
      case RecordingSourceInput(captureCardId) => api.queryRecorderGetCurrentRecording(captureCardId)
      case RecordingSourceId(id) => id match {
        case RecordedIdChanTime(chanId, startTime) => api.queryRecording(chanId, startTime)
        case recordedIdInt: RecordedIdInt => api match {
          case _: MythProtocolApi88 =>
            // There is no API call to query a recording by recordedId, so enumerate all recordings and search.
            for {
              recs <- api.queryRecordings()
              rec = recs.find(r => r.recordedId == recordedIdInt)
            } yield rec.get
          case _ => throw new Exception("integer recordedId not supported by this backend protocol version")
        }
      }
    }
  }

  // TODO progress meter (optional, default)

  override def argParser: ArgParser = new RecXferArgParser

  override def usageArguments: String =
    s"""<recording>
      |
      | where <recording> is one of the following:
      |     input:<card-input-id>
      |     recid:<recorded-id>        (for MythTV 0.28+ backends)
      |     rec:<chanid>_<recstartts>
      |
      | examples:
      |   $usageName input:2
      |   $usageName recid:1068
      |   $usageName rec:1081_20180901083000""".stripMargin

  def main(args: Array[String]): Unit = {
    options = argParser.parseArgs(args)

    val recordingSource = options.get(OptRecording) match {
      case Some(source: RecordingSource) => source
      case _ => throw new Exception("no recording source specified")
    }

    val api = apiMonitorConnection(options)
    val result = for {
      rec <- getRecordingBySource(api, recordingSource)
      totalRead = doTransfer(api, rec)
    } yield totalRead

    import MythProtocolFailure._
    result match {
      case Right(totalBytesRead) => println(s"done, $totalBytesRead bytes transferred.")
      case Left(fail: MythProtocolFailure) => fail match {
        // TODO improve error messages
        case MythProtocolNoResult => println("- No result - ")
        case MythProtocolFailureUnknown => println("- Unknown Failure -")
        case MythProtocolFailureMessage(message) => println(s"Received error response: $message")
        case MythProtocolFailureThrowable(throwable) => throwable.printStackTrace()
      }
    }
  }
}
