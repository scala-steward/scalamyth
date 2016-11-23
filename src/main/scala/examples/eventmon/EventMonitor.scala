package examples.eventmon

import java.time.LocalDateTime

import mythtv._
import connection.myth._

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
    new EventMonitor("myth1")
  }
}
