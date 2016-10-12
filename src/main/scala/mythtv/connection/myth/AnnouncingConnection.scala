package mythtv
package connection
package myth

private trait AnnouncingConnection {
  private[this] var announced = false

  def announce(): Unit
  def hasAnnounced: Boolean = announced

  announce()
  announced = true
}
