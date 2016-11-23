package mythtv
package connection
package myth

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

private[myth] trait WaitableFileTransferChannel extends FileTransferChannel {
  self: FileTransferChannelImpl =>

  private[this] val lock = new ReentrantLock
  private[this] val sizeChanged = lock.newCondition

  def isInProgress: Boolean

  protected abstract override def waitForMoreData(oldSize: Long): Boolean = {
    if (isInProgress) doWaitForMoreData(oldSize)
    else false
  }

  private def doWaitForMoreData(oldSize: Long): Boolean = {
    println("waiting for more data " + oldSize)
    lock.lock()
    try {
      // TODO also have an absolute deadline in the invariant so that this
      //      won't wait forever if we never get any event updates
      while (isInProgress && currentSize <= oldSize)
        sizeChanged.await(10, TimeUnit.MINUTES)  // TODO how long to wait?
    }
    finally lock.unlock()
    true
  }

  private[myth] def signalSizeChanged(): Unit = {
    lock.lock()
    try sizeChanged.signalAll()
    finally lock.unlock()
  }
}
