// SPDX-License-Identifier: LGPL-2.1-only
/*
 * FileXfer.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package examples.protocol

import mythtv.connection.myth.{ FileTransferChannel, MythProtocolAPIConnection }
import mythtv.util.ByteCount

object FileXfer extends SampleProgram with Xfer {

  def doTransfer(api: MythProtocolAPIConnection, filePath: String, storageGroup: String): ByteCount = {
    val ft = FileTransferChannel(api, filePath, storageGroup)
    val verbose = options contains 'verbose
    doTransfer(ft, verbose)
  }

  var options: OptionMap = Map.empty

  class FileXferArgParser extends ArgParser {
    override def accumulateOptions(opts: OptionMap, args: List[String]): OptionMap = args match {
      case Nil                       => opts
      case arg :: _ if isOption(arg) => super.accumulateOptions(opts, args)
      case url :: cdr                => accumulateOptions(opts ++ urlOpt(url), cdr)
      case _                         => super.accumulateOptions(opts, args)
    }

    def urlOpt(url: String): OptionMap = {
      val (serverHost, storageGroup, filePath) = parseUrl(url)
      Map('host -> serverHost, 'storageGroup -> storageGroup, 'filePath -> filePath)
    }

    def parseUrl(spec: String): (String, String, String) = {
      val Pattern = """myth://([^/]+)/([^/]+)/(.+)""".r  // TODO add optional 'port' as well?
      spec match {
        case Pattern(host, storageGroup, filePath) => (host, storageGroup, filePath)
        case _ => throw new Exception("invalid backend URL specified")
      }
    }
  }

  override def argParser: ArgParser = new FileXferArgParser

  override def usageArguments: String =
    s"""<backendUrl>
      |
      | where <backendUrl> is of the form:  myth://backend-host/storage-group/path/to/file.name
      |
      | example:
      |   $usageName myth://myth-server/Videos/Movies/A/American_Beauty.mp4""".stripMargin

  def main(args: Array[String]): Unit = {
    options = argParser.parseArgs(args)

    val storageGroup = options.get('storageGroup) match {
      case Some(value: String) => value
      case _ => fatal("no storage group given")
    }

    val filePath = options.get('filePath) match {
      case Some(value: String) => value
      case _ => fatal("no file path given")
    }

    val api = apiMonitorConnection(options)
    val totalBytesRead = doTransfer(api, filePath, storageGroup)
    println(s"done, $totalBytesRead bytes transferred.")
  }
}
