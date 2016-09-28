package mythtv

import model.Frontend
import services.Service

object MythTV {
  def frontend(host: String): MythFrontend = new MythFrontend(host)
  def backend(host: String): MythBackend = new MythBackend(host)
  def service[A <: Service]: Service = ???
  def discoverFrontends: List[Frontend] = ???
}
