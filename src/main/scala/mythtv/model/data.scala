package mythtv
package model

import java.time.{ Instant, ZoneOffset }

trait Backend extends BackendOperations
trait Frontend extends FrontendOperations

trait IntegerIdentifier extends Any {
  def id: Int
}

trait Settings {
  def hostName: String
  def settings: Map[String, String]

  override def toString: String = s"<Settings for $hostName (${settings.size})>"
}

trait TimeZoneInfo {
  def tzName: String
  def offset: ZoneOffset
  def currentTime: Instant

  override def toString: String = s"<TimeZoneInfo $tzName $offset>"
}

// TODO make a tuple type for (chanid, starttime) to shorten parameter lists?
//        and to ease switchover to 0.28+ recordedId in places?
trait PlayableMedia {
  // TODO what are subclasses?  Program(?), Recording, Video, music?
  // TODO methods on PlayableMedia
  def playOnFrontend(fe: Frontend): Boolean
}

trait TitleInfo {
  def title: String
  def inetRef: String

  override def toString: String = s"<TitleInfo $title, $inetRef>"
}

trait MythTvVersionInfo {
  def fullVersion: String
  def branch: String
  def protocol: String
  def binary: String
  def schema: String

  override def toString: String = s"$fullVersion $binary $protocol $schema"
}

trait DatabaseConnectionInfo {
  def host: String
  def port: Int
  def ping: Boolean
  def userName: String
  def password: String
  def dbName: String
  def driver: String
  def localEnabled: Boolean
  def localHostName: String

  override def toString: String = s"$userName@$host:$port/$dbName"
}

trait WakeOnLanInfo {
  def enabled: Boolean
  def reconnect: Int
  def retry: Int
  def command: String

  override def toString: String = s"<WakeOnLanInfo $enabled $command>"
}

trait ConnectionInfo {
  def version: MythTvVersionInfo
  def database: DatabaseConnectionInfo
  def wakeOnLan: WakeOnLanInfo
}
