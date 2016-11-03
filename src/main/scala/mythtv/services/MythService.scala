package mythtv
package services

import model.{ Settings, StorageGroupDir, TimeZoneInfo }

trait MythService extends BackendService {
  def serviceName: String = "Myth"

  //def getConnectionInfo(pin: String): ConnectionInfo

  def getHostName: String
  def getHosts: List[String]

  def getKeys: List[String]
  def getSetting(hostName: String, key: String = ""): Settings

  def getTimeZone: TimeZoneInfo

  def getStorageGroupDirs(hostName: String = "", groupName: String = ""): List[StorageGroupDir]

  // getLogs(....)

  /* mutating POST methods */

  def addStorageGroupDir(storageGroup: String, dirName: String, hostName: String): Boolean

  def removeStorageGroupDir(storageGroup: String, dirName: String, hostName: String): Boolean

  def putSetting(hostName: String, key: String, value: String): Boolean

  def changePassword(userName: String, oldPassword: String, newPassword: String): Boolean

  def testDbSettings(hostName: String, userName: String, password: String, dbName: String, dbPort: Int): Boolean

  def sendMessage(message: String, address: String, udpPort: Int, timeout: Int): Boolean

  //def sendNotification(....): Boolean

  def backupDatabase(): Boolean

  def checkDatabase(repair: Boolean = false): Boolean


  def profileSubmit(): Boolean

  def profileDelete(): Boolean

// TODO are the three below GET methods?

  def profileUrl: String

  def profileUpdated: String

  def profileText: String

}
