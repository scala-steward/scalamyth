// SPDX-License-Identifier: LGPL-2.1-only
/*
 * MythService.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package services

import java.net.URI
import java.time.{ Duration, Instant }

import model._
import util.MythDateTime
import EnumTypes.{ NotificationPriority, NotificationType, NotificationVisibility }

trait MythService extends BackendService {
  final def serviceName: String = "Myth"

  // getBackendInfo is new with MythTV 0.28
  def getBackendInfo: ServiceResult[BackendDetails]

  def getConnectionInfo(pin: String): ServiceResult[ConnectionInfo]

  // getFrontends is new with MythTV 0.28
  def getFrontends: ServiceResult[List[KnownFrontendInfo]] = getFrontends(false)
  def getFrontends(onlyOnline: Boolean): ServiceResult[List[KnownFrontendInfo]]

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
  def getSetting(key: String): ServiceResult[String] = getSetting("_GLOBAL_", key)

  def getSetting(hostname: String, key: String): ServiceResult[String]

  def getSettingList(hostName: String = ""): ServiceResult[Settings]

  // getFormatDate is new with MythTV 0.28
  def getFormatDate(dateTime: MythDateTime, shortDate: Boolean = false): ServiceResult[String]

  // getFormatDateTime is new with MythTV 0.28
  def getFormatDateTime(dateTime: MythDateTime, shortDate: Boolean = false): ServiceResult[String]

  // getFormatTime is new with MythTV 0.28
  def getFormatTime(dateTime: MythDateTime): ServiceResult[String]

  // parseIsoDateString is new with MythTV 0.28
  def parseIsoDateString(dateTimeString: String): ServiceResult[MythDateTime]

  def getTimeZone: ServiceResult[TimeZoneInfo]

  def getStorageGroupDirs(hostName: String = "", groupName: String = ""): ServiceResult[List[StorageGroupDir]]

  def getLogHostNames: ServiceResult[List[String]]

  def getLogApplications: ServiceResult[List[String]]

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
    hostName: String,
    application: String,
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
    *
    * This API appears to be useless, at least in 0.28, as nothing seems to use this password,
    * and I can't find a way to set the initial value other than editing the database directly.
    */
  def changePassword(userName: String, oldPassword: String, newPassword: String): ServiceResult[Boolean]

  def testDbSettings(hostName: String, userName: String, password: String, dbName: String = "", dbPort: Int = 0): ServiceResult[Boolean]

  // NB the 'address' parameter must be an IP address (not a hostname) if given
  // NB broadcast address doesn't seem to be picked up by frontends? The packet is visible on bigbertha in wireshark.
  //    It turns out the frontends were listening on 192.168.1.255 but not 255.255.255.255.  Wonder why? Investigate.
  //    mythudplistener in libmythui ; uses ServerPool::DefaultBroadcast from libmythbase
  //    UPSTREAM: The bug seems to be in the backend service; should use subnet broadcast address rather than global?
  def sendMessage(message: String, address: String = "", udpPort: Int = 0, timeout: Duration = Duration.ZERO): ServiceResult[Boolean]

  def sendNotification(
    message: String,
    origin: String = "",
    description: String = "",
    extra: String = "",
    progressText: String = "",
    progress: Float = 0f,   /* should be decimal between 0 and 1 */
    fullScreen: Boolean = false,
    timeout: Duration = Duration.ZERO,
    notifyType: NotificationType = NotificationType.New,
    priority: NotificationPriority = NotificationPriority.Default,
    visibility: NotificationVisibility = NotificationVisibility.All,
    address: String = "",
    udpPort: Int = 0
  ): ServiceResult[Boolean]

  // This causes a database backup to be performed synchronously, so may not return for some time.
  def backupDatabase(): ServiceResult[Boolean]

  // This causes a database check to be performed synchronously, so may not return for some time.
  def checkDatabase(repair: Boolean = false): ServiceResult[Boolean]

  // Submit the hardware profile to the MythTV smolt server
  def profileSubmit(): ServiceResult[Boolean]

  // Deletes any submitted profile and disables hardware profiling
  def profileDelete(): ServiceResult[Boolean]

  // Retrieves the URL of a submitted profile on the MythTV servers
  def profileUrl: ServiceResult[URI]

  // This returns a date but in a format which may vary, and which may omit some components,
  // so we don't attempt to parse it. (It uses the DateFormat value in the 'settings' table)
  def profileUpdated: ServiceResult[String]

  // This causes a new hardware profile to be generated, by running an external command
  def profileText(): ServiceResult[String]
}
