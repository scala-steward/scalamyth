package mythtv
package connection
package myth

private[myth] trait AnnouncingConnection {
  self: BackendConnection =>

  private[this] var announced = false

  def announce(): Unit
  def hasAnnounced: Boolean = announced

  try {
    announce()
    announced = true
  }
  finally if (!announced) disconnect()
}
