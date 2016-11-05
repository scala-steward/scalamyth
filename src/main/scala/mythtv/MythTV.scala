package mythtv

import model.Frontend
import services._

object MythTV {
  def frontend(host: String): MythFrontend = new MythFrontend(host)
  def backend(host: String): MythBackend = new MythBackend(host)
  def service[A <: Service](host: String)(implicit provider: ServiceProvider[A]): A = provider.instance(host)
  def service[A <: Service](host: String, port: Int)(implicit provider: ServiceProvider[A]): A = provider.instance(host, port)
  def discoverFrontends: List[Frontend] = ???
}
