// SPDX-License-Identifier: LGPL-2.1-only
/*
 * MythTV.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv

import java.net.InetAddress

import services._
import util.ServiceDiscovery
import connection.http.BackendServiceConnection
import connection.myth.FrontendConnection

object MythTV {
  def frontend(host: String): MythFrontend = new MythFrontend(host)
  def backend(host: String): MythBackend = new MythBackend(host)

  @deprecated("", "") def service[A <: Service](host: String)(implicit factory: ServiceFactory[A]): A = factory(host)
  @deprecated("", "") def service[A <: Service](host: String, port: Int)(implicit factory: ServiceFactory[A]): A = factory(host, port)

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

  // If there is more than one master backend advertised via mDNS, this may find
  // either of them; which one is undefined.
  def discoverMasterBackend: BackendInfo = {
    val backends = ServiceDiscovery.discoverBackends
    if (backends.isEmpty) throw new NoSuchElementException("No MythTV backends discovered")

    val trialBackend = backends.head
    val host = trialBackend.addresses.head.getHostAddress
    val port = trialBackend.port
    val myth = ServiceProvider.mythService(host, port)

    val masterIp = myth.getSetting("MasterServerIP").get
    val masterPort = myth.getSetting("MasterServerPort").get.toInt

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
