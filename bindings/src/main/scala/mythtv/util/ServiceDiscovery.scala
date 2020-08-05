// SPDX-License-Identifier: LGPL-2.1-only
/*
 * ServiceDiscovery.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package util

import java.time.Duration
import java.net.InetAddress

import scala.collection.Set
import scala.jdk.CollectionConverters._

import net.straylightlabs.hola.dns.Domain
import net.straylightlabs.hola.sd.{ Instance, Query, Service }

trait NetworkServiceInstance {
  def name: String
  def port: Int
  def addresses: Set[InetAddress]

  override def toString: String = s"$name port=$port $addresses"
}

object ServiceDiscovery {
  private final val BackendServiceName = "_mythbackend._tcp"
  private final val FrontendServiceName = "_mythfrontend._tcp"

  private def makeResult(in: Instance): NetworkServiceInstance = {
    new NetworkServiceInstance {
      def name      = in.getName
      def port      = in.getPort
      def addresses = in.getAddresses.asScala
    }
  }

  def discover(serviceName: String, timeout: Duration = Duration.ofMillis(750)): Iterable[NetworkServiceInstance] = {
    val service = Service.fromName(serviceName)
    val query = Query.createWithTimeout(service, Domain.LOCAL, timeout.toMillis.toInt)
    val instances = query.runOnceOn(NetworkUtil.myInetAddress).asScala
    instances map makeResult
  }

  def discoverFrontends: Iterable[NetworkServiceInstance] = discover(FrontendServiceName)
  def discoverFrontends(timeout: Duration): Iterable[NetworkServiceInstance] = discover(FrontendServiceName, timeout)

  def discoverBackends: Iterable[NetworkServiceInstance] = discover(BackendServiceName)
  def discoverBackends(timeout: Duration): Iterable[NetworkServiceInstance] = discover(BackendServiceName, timeout)
}
