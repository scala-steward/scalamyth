package mythtv
package connection
package myth

import scala.concurrent.duration.Duration

trait EventLock {
  def event: Option[BackendEvent]
  // TODO timeout doesn't do what you might think, and is of questionable use here
  def waitFor(timeout: Duration = Duration.Inf): Unit
}

object EventLock {
  def apply(eventConn: EventConnection, eventFilter: (BackendEvent) => Boolean): EventLock =
    new Lock(eventConn, eventFilter)

  /**
    * Returns an event lock object without an event; waitFor always returns
    * immediately and event is always None.
    */
  def empty: EventLock = Empty

  private final class Lock(eventConn: EventConnection, eventFilter: (BackendEvent) => Boolean)
      extends EventListener with EventLock {
    private[this] var locked = true
    @volatile private[this] var unlockEvent: Option[BackendEvent] = None

    eventConn.addListener(this)

    def listenFor(event: BackendEvent): Boolean = eventFilter(event)

    def handle(event: BackendEvent): Unit = {
      synchronized {
        locked = false
        unlockEvent = Some(event)
        notifyAll()
      }
      eventConn.removeListener(this)
    }

    def event: Option[BackendEvent] = unlockEvent

    def waitFor(timeout: Duration = Duration.Inf): Unit = {
      val millis = if (timeout.isFinite()) timeout.toMillis else 0
      synchronized { while (locked) wait(millis) }
    }
  }

  private object Empty extends EventLock {
    def event: Option[BackendEvent] = None
    def waitFor(timeout: Duration = Duration.Inf): Unit = ()
  }
}
