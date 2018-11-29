package examples.protocol

import java.time.LocalDateTime

import mythtv.connection.myth._

class Listener extends EventListener {
  def listenFor(event: Event): Boolean = true
  def handle(event: Event): Unit = {
    log(event)
  }

  private def log(ev: Event): Unit = {
    println(LocalDateTime.now.toString + " " + ev.toString)
  }
}

class EventMonitor(host: String) {
  val conn = EventConnection(host, eventMode = MythProtocolEventMode.Normal)
  conn += new Listener
}

object EventMonitor {
  def main(args: Array[String]): Unit = {
    val host = if (args.length > 0) args(0) else "myth1"
    new EventMonitor(host)
  }
}
