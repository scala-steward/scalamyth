// SPDX-License-Identifier: LGPL-2.1-only
/*
 * SampleProgram.scala
 *
 * Copyright (c) 2018 Tom Grigg <tom@grigg.io>
 */
package examples.protocol

import mythtv.connection.myth.MythProtocolAPIConnection

trait SampleProgram {
  import SampleProgram._

  /* Command line argument parsing */

  type OptionMap = Map[String, Any]

  def usage(): Unit = {
    val info =
      s"""
         |Usage: $usageName [options] $usageArguments
         |
         |Options:
         |
         |  --backendHost <hostname>    default: localhost
         |  --backendPort <portNumber>  default: 6543
         |  --clientName  <clientName>  default: scalamyth
         |  --help                      display this help message
         |  --verbose                   display verbose output during execution
         |$usageAdditionalOptions""".stripMargin
    println(info)
  }

  def usageName: String = getClass.getSimpleName dropRight 1
  def usageArguments: String = ""
  def usageAdditionalOptions: String = ""

  class ArgParser {
    final def isOption(s: String): Boolean = s.head == '-'
    final def flag(sym: String): OptionMap = Map(sym -> true)
    final def int(sym: String, value: String): OptionMap = Map(sym -> value.toInt)
    final def str(sym: String, value: String): OptionMap = Map(sym -> value)

    def accumulateOptions(opts: OptionMap, args: List[String]): OptionMap = args match {
      case Nil => opts
      case "--backendHost" :: host :: cdr   => accumulateOptions(opts ++ str(OptHost, host), cdr)
      case "--backendPort" :: port :: cdr   => accumulateOptions(opts ++ int(OptPort, port), cdr)
      case "--clientName"  :: client :: cdr => accumulateOptions(opts ++ str(OptClient, client), cdr)
      case "--help" :: cdr                  => accumulateOptions(opts ++ flag(OptHelp), cdr)
      case "--verbose" :: cdr               => accumulateOptions(opts ++ flag(OptVerbose), cdr)
      case arg :: _ if isOption(arg)        => fatal(s"Unknown option $arg")
      case _                                => opts
    }

    def parseArgs(args: Array[String]): OptionMap = {
      val parsedOptions = accumulateOptions(defaultOptions, args.toList)
      if (parsedOptions contains OptHelp) {
        usage()
        sys.exit(1)
      }
      parsedOptions
    }
  }

  /* Convenience methods to get common option arguments */

  def backendHost(opts: OptionMap): String = opts(OptHost).toString
  def backendPort(opts: OptionMap): Option[Int] = opts.get(OptPort) collectFirst { case p: Int => p }
  def clientName(opts: OptionMap): String = opts(OptClient).toString

  /* Convenience methods to get an API Connection to the backend */

  def apiConnection(opts: OptionMap): MythProtocolAPIConnection = backendPort(opts) match {
    case Some(port) => MythProtocolAPIConnection(backendHost(opts), port)
    case None       => MythProtocolAPIConnection(backendHost(opts))
  }

  def apiMonitorConnection(opts: OptionMap): MythProtocolAPIConnection = {
    val api = apiConnection(opts)
    api.announce("Monitor", clientName(opts))  // TODO check result of announce
    api
  }

  /* Abort with message */
  def fatal(msg: String): Nothing = { Console.err.println(msg); sys.exit(1) }

  /* Default argument parser */
  def argParser: ArgParser = new ArgParser
  def defaultOptions: OptionMap = Map(OptHost -> "localhost", OptClient -> "scalamyth")
}

object SampleProgram {
  final val OptHelp = "help"
  final val OptHost = "host"
  final val OptPort = "port"
  final val OptClient = "client"
  final val OptVerbose = "verbose"
}
