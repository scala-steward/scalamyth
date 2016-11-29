package mythtv
package services

import java.time.{ Duration, Instant }

import model.{ ConnectionInfo, LogMessage, Settings, StorageGroupDir, TimeZoneInfo }

trait MythService extends BackendService {
  def serviceName: String = "Myth"

  def getConnectionInfo(pin: String): ServiceResult[ConnectionInfo]

  def getHostName: ServiceResult[String]

  def getHosts: ServiceResult[List[String]]
  def getKeys: ServiceResult[List[String]]

  /**
    * Query a global MythTV setting.
    *
    * A global setting is stored in the database with NULL as the hostname.
    *
    * @param key the key name of the global setting to query
    * @return the value of the setting, or None if not found
    */
  def getSetting(key: String): ServiceResult[Option[String]] = {
    require(key.nonEmpty)
    getSettings("", key) map (_.settings.get(key))
  }

  def getSetting(hostname: String, key: String): ServiceResult[Option[String]] = {
    require(key.nonEmpty)
    getSettings(hostname, key) map (_.settings.get(key))
  }

  def getSettings(hostName: String = "", key: String = ""): ServiceResult[Settings]

  def getTimeZone: ServiceResult[TimeZoneInfo]

  def getStorageGroupDirs(hostName: String = "", groupName: String = ""): ServiceResult[List[StorageGroupDir]]

  // TODO leaving hostName or application blank results in collecting a list (matrix?) of available host/app names
  /**
    * Retrieve log entries from the database.
    *
    * If logging to the database is not configured, then there will be no entries
    * to return. Starting with MythTV 0.27, if database logging is desired it must
    * be enabled using the --enable-dblog argument to mythbackend.
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
    lineNumber: Int = 0,
    function: String = "",
    fromTime: Instant = Instant.MAX,
    toTime: Instant = Instant.MIN,
    level: String = "",
    msgContains: String = ""
  ): ServiceResult[List[LogMessage]]

  /* mutating POST methods */

  def addStorageGroupDir(storageGroup: String, dirName: String, hostName: String): ServiceResult[Boolean]

  def removeStorageGroupDir(storageGroup: String, dirName: String, hostName: String): ServiceResult[Boolean]

  def removeStorageGroupDir(dir: StorageGroupDir): ServiceResult[Boolean] =
    removeStorageGroupDir(dir.groupName, dir.dirName, dir.hostName)

  def putSetting(hostName: String, key: String, value: String): ServiceResult[Boolean]

  /**
    * Changes password stored at HTTP/Protected/Password, used for some services? URLs,
    * specified by HTTP/Protected/Urls ? See libs/libmythupnp/httprequest.cpp for more info.
    */
  def changePassword(userName: String, oldPassword: String, newPassword: String): ServiceResult[Boolean]

  def testDbSettings(hostName: String, userName: String, password: String, dbName: String = "", dbPort: Int = 0): ServiceResult[Boolean]

  // NB the 'address' parameter must be an IP address (not a hostname) if given
  // NB broadcast address doesn't seem to be picked up by frontends? The packet is visible on bigbertha in wireshark.
  //    It turns out the frontends were listening on 192.168.1.255 but not 255.255.255.255.  Wonder why? Investigate.
  //    mythudplistener in libmythui ; uses ServerPool::DefaultBroadcast from libmythbase
  //    The bug seems to be in the backend service; should use subnet broadcast address rather than global?
  def sendMessage(message: String, address: String = "", udpPort: Int = 0, timeout: Duration = Duration.ZERO): ServiceResult[Boolean]

  //def sendNotification(....): Boolean

  // This causes a database backup to be performed synchronously, so may not return for some time.
  def backupDatabase(): ServiceResult[Boolean]

  // This causes a database check to be performed synchronously, so may not return for some time.
  def checkDatabase(repair: Boolean = false): ServiceResult[Boolean]


  def profileSubmit(): ServiceResult[Boolean]

  def profileDelete(): ServiceResult[Boolean]

// TODO are the three below GET methods?

  def profileUrl: ServiceResult[String]

  def profileUpdated: ServiceResult[String]

  def profileText: ServiceResult[String]

}
