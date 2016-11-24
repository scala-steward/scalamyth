package mythtv
package services

import java.time.{ Duration, Instant }

import model.{ ConnectionInfo, Settings, StorageGroupDir, TimeZoneInfo }

trait LogMessage // TODO temporary

trait MythService extends BackendService {
  def serviceName: String = "Myth"

  def getConnectionInfo(pin: String): ConnectionInfo

  def getHostName: String

  def getHosts: List[String]
  def getKeys: List[String]

  /**
    * Query a global MythTV setting.
    *
    * A global setting is stored in the database with NULL as the hostname.
    *
    * @param key the key name of the global setting to query
    * @return the value of the setting, or None if not found
    */
  def getSetting(key: String): Option[String] = {
    require(key.nonEmpty)
    getSettings("", key).settings.get(key)
  }

  def getSetting(hostname: String, key: String): Option[String] = {
    require(key.nonEmpty)
    getSettings(hostname, key).settings.get(key)
  }

  def getSettings(hostName: String = "", key: String = ""): Settings

  def getTimeZone: TimeZoneInfo

  def getStorageGroupDirs(hostName: String = "", groupName: String = ""): List[StorageGroupDir]

  /**
    * Retrieve log entries from the database.
    *
    * If logging to the database is not configured, then there will be no entries
    * to return.
    *
    * All parameters are optional and allow for filtering the log messages to only
    * those of interest.
    */
  def getLogs(
    hostName: String = "",
    application: String = "",
    pid: Int = 0,
    tid: Int = 0,
    thread: String = "",
    filename: String = "",
    line: Int = 0,
    function: String = "",
    fromTime: Instant = Instant.MAX,
    toTime: Instant = Instant.MIN,
    level: String = "",
    msgContains: String = ""
  ): List[LogMessage]

  /* mutating POST methods */

  def addStorageGroupDir(storageGroup: String, dirName: String, hostName: String): Boolean

  def removeStorageGroupDir(storageGroup: String, dirName: String, hostName: String): Boolean

  def removeStorageGroupDir(dir: StorageGroupDir): Boolean =
    removeStorageGroupDir(dir.groupName, dir.dirName, dir.hostName)

  def putSetting(hostName: String, key: String, value: String): Boolean

  /**
    * Changes password stored at HTTP/Protected/Password, used for some services? URLs,
    * specified by HTTP/Protected/Urls ? See libs/libmythupnp/httprequest.cpp for more info.
    */
  def changePassword(userName: String, oldPassword: String, newPassword: String): Boolean

  def testDbSettings(hostName: String, userName: String, password: String, dbName: String = "", dbPort: Int = 0): Boolean

  // NB the 'address' parameter must be an IP address (not a hostname) if given
  // NB broadcast address doesn't seem to be picked up by frontends? The packet is visible on bigbertha in wireshark.
  //    It turns out the frontends were listening on 192.168.1.255 but not 255.255.255.255.  Wonder why? Investigate.
  //    mythudplistener in libmythui ; uses ServerPool::DefaultBroadcast from libmythbase
  //    The bug seems to be in the backend service; should use subnet broadcast address rather than global?
  def sendMessage(message: String, address: String = "", udpPort: Int = 0, timeout: Duration = Duration.ZERO): Boolean

  //def sendNotification(....): Boolean

  // This causes a database backup to be performed synchronously, so may not return for some time.
  def backupDatabase(): Boolean

  // This causes a database check to be performed synchronously, so may not return for some time.
  def checkDatabase(repair: Boolean = false): Boolean


  def profileSubmit(): Boolean

  def profileDelete(): Boolean

// TODO are the three below GET methods?

  def profileUrl: String

  def profileUpdated: String

  def profileText: String

}
