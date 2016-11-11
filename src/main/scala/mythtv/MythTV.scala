package mythtv

import java.net.InetAddress

import services._
import util.ServiceDiscovery
import connection.http.BackendServiceConnection
import connection.myth.FrontendConnection

object MythTV {
  def frontend(host: String): MythFrontend = new MythFrontend(host)
  def backend(host: String): MythBackend = new MythBackend(host)

  def service[A <: Service](host: String)(implicit provider: ServiceProvider[A]): A = provider.instance(host)
  def service[A <: Service](host: String, port: Int)(implicit provider: ServiceProvider[A]): A = provider.instance(host, port)

  private def parseHostNameFromServiceName(serviceName: String): String = {
    val pattern = """.+ on (.+)""".r
    serviceName match {
      case pattern(hostName) => hostName
      case _ => ""
    }
  }

  def discoverFrontends: Iterable[FrontendInfo] = {
    val frontends = ServiceDiscovery.discoverFrontends
    frontends map (f => new FrontendInfo {
      def hostName     = parseHostNameFromServiceName(f.name)
      def addresses    = f.addresses
      def servicesPort = f.port
      def remoteControlPort = FrontendConnection.DefaultPort
    })
  }

  def discoverMasterBackend: BackendInfo = {
    import connection.http.json.JsonServiceProviders._

    val backends = ServiceDiscovery.discoverBackends
    val trialBackend = backends.head // TODO check that we got some results

    val host = trialBackend.addresses.head.getHostAddress
    val port = trialBackend.port
    val myth = service[MythService](host, port)

    // TODO will slave backends process services requests properly?
    val masterIp = myth.getSetting("MasterServerIP").get
    val masterPort = myth.getSetting("MasterServerPort").map(_.toInt).get

    val masterAddr = InetAddress.getByName(masterIp)
    val discoveredMaster = backends find (_.addresses contains masterAddr)

    if (discoveredMaster.isEmpty) new BackendInfo {
      def hostName         = ""
      def addresses        = List(masterAddr)
      def mythProtocolPort = masterPort
      def servicesPort     = BackendServiceConnection.DefaultPort
    }
    else new BackendInfo {
      def hostName         = parseHostNameFromServiceName(discoveredMaster.get.name)
      def addresses        = discoveredMaster.get.addresses
      def mythProtocolPort = masterPort
      def servicesPort     = discoveredMaster.get.port
    }
  }
}
