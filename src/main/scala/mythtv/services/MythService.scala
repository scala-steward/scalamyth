package mythtv
package services

import model.{ Settings ,StorageGroupDir, TimeZoneInfo }

trait MythService extends BackendService {
  def serviceName: String = "Myth"

  def getHostName: String
  def getHosts: List[String]
  def getKeys: List[String]
  def getSetting(hostName: String, key: String = ""): Settings

  def getTimeZone: TimeZoneInfo
  def getStorageGroupDirs(hostName: String = "", groupName: String = ""): List[StorageGroupDir]
}
