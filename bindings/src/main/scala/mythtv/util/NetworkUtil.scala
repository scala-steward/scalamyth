package mythtv
package util

import java.net.{ InetAddress, NetworkInterface }

import scala.collection.JavaConverters._

object NetworkUtil {
  // TODO cache the results of these lookups to prevent them from being expensive later

  // TODO support querying only IPv4 address, or only IPv6 addresses (parameter: AddressType: IPv4, IPv6, Any)
  def myInetAddress: InetAddress = {
    val interfaces = NetworkInterface.getNetworkInterfaces.asScala
    val multicastable = interfaces filter (_.supportsMulticast)
    if (multicastable.isEmpty) throw new RuntimeException("no multicastable network interfaces found")

    val interface = multicastable.next()
    val addresses = interface.getInetAddresses.asScala
    if (addresses.isEmpty) throw new RuntimeException("no addresses found for network interface" + interface)
    addresses.next()
  }

  def myHostName: String = {
    // TODO this is less than ideal, may fail in certain circumstances...
    InetAddress.getLocalHost.getHostName
  }
}
