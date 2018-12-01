// SPDX-License-Identifier: LGPL-2.1-only
/*
 * EventMonitor.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package examples.protocol

import java.time.LocalDateTime

import mythtv.connection.myth.{ Event, EventConnection, EventListener, MythProtocolEventMode }

class Listener extends EventListener {
  def listenFor(event: Event): Boolean = true
  def handle(event: Event): Unit = {
    log(event)
  }

  private def log(ev: Event): Unit = {
    println(LocalDateTime.now.toString + " " + ev.toString)
  }
}

class EventMonitor(host: String, optionalPort: Option[Int]) {
  val conn: EventConnection = optionalPort match {
    case Some(port) => EventConnection(host, port, eventMode = MythProtocolEventMode.Normal)
    case None       => EventConnection(host, eventMode = MythProtocolEventMode.Normal)
  }
  conn += new Listener
}

// TODO add support for filtering (arbitrary or canned?)

object EventMonitor extends SampleProgram {
  def main(args: Array[String]): Unit = {
    val options = argParser.parseArgs(args)
    new EventMonitor(backendHost(options), backendPort(options))
  }
}
