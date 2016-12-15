package mythtv
package connection
package http

import java.net.URI
import java.time.{ Duration, Instant }

import model._
import services.{ MythService, ServiceResult }
import util.MythDateTime
import EnumTypes.{ NotificationPriority, NotificationType, NotificationVisibility }

trait AbstractMythService extends ServiceProtocol with MythService {

  def getBackendInfo: ServiceResult[BackendDetails] = {
    request("GetBackendInfo")("BackendInfo")
  }

  def getConnectionInfo(pin: String): ServiceResult[ConnectionInfo] = {
    val params: Map[String, Any] = Map("Pin" -> pin)
    post("GetConnectionInfo", params)("ConnectionInfo")
  }

  def getFrontends(onlyOnline: Boolean): ServiceResult[List[KnownFrontendInfo]] = {
    var params: Map[String, Any] = Map.empty
    if (onlyOnline) params += "OnLine" -> onlyOnline
    request("GetFrontends", params)("FrontendList", "Frontends")
  }

  def getHostName: ServiceResult[String] = {
    request("GetHostName")()
  }

  def getHosts: ServiceResult[List[String]] = {
    request("GetHosts")()
  }

  def getKeys: ServiceResult[List[String]] = {
    request("GetKeys")()
  }

  def getSettings(hostName: String, key: String): ServiceResult[Settings] = {
    var params: Map[String, Any] = Map.empty
    if (hostName.nonEmpty) params += "HostName" -> hostName
    if (key.nonEmpty)      params += "Key" -> key
    request("GetSetting", params)("SettingList")
  }

  def getSettingList(hostName: String = ""): ServiceResult[Settings] = {
    var params: Map[String, Any] = Map.empty
    if (hostName.nonEmpty) params += "HostName" -> hostName
    request("GetSettingList", params)("SettingList")
  }

  def getStorageGroupDirs(hostName: String, groupName: String): ServiceResult[List[StorageGroupDir]] = {
    var params: Map[String, Any] = Map.empty
    if (hostName.nonEmpty) params += "HostName" -> hostName
    if (groupName.nonEmpty) params += "GroupName" -> groupName
    request("GetStorageGroupDirs", params)("StorageGroupDirList", "StorageGroupDirs")
  }

  def getFormatDate(dateTime: MythDateTime, shortDate: Boolean = false): ServiceResult[String] = {
    var params: Map[String, Any] = Map("Date" -> dateTime.toIsoFormat)
    if (shortDate) params += "ShortDate" -> shortDate
    request("GetFormatDate", params)()
  }

  def getFormatDateTime(dateTime: MythDateTime, shortDate: Boolean = false): ServiceResult[String] = {
    var params: Map[String, Any] = Map("DateTime" -> dateTime.toIsoFormat)
    if (shortDate) params += "ShortDate" -> shortDate
    request("GetFormatDateTime", params)()
  }

  def getFormatTime(dateTime: MythDateTime): ServiceResult[String] = {
    val params: Map[String, Any] = Map("Time" -> dateTime.toIsoFormat)
    request("GetFormatTime", params)()
  }

  def getTimeZone: ServiceResult[TimeZoneInfo] = {
    request("GetTimeZone")("TimeZoneInfo")
  }

  def parseIsoDateString(dateTimeString: String): ServiceResult[MythDateTime] = {
    val params: Map[String, Any] = Map("DateTime" -> dateTimeString)
    request("ParseISODateString", params)("DateTime")
  }

  def getLogHostNames: ServiceResult[List[String]] = {
    request("GetLogs")("LogMessageList", "HostNames")
  }

  def getLogApplications: ServiceResult[List[String]] = {
    request("GetLogs")("LogMessageList", "Applications")
  }

  def getLogs(
    hostName: String,
    application: String,
    pid: Int,
    tid: Int,
    thread: String,
    filename: String,
    lineNumber: Int,
    function: String,
    fromTime: Instant,
    toTime: Instant,
    level: String,
    msgContains: String
  ): ServiceResult[List[LogMessage]] = {
    var params: Map[String, Any] = Map.empty
    if (hostName.nonEmpty)       params += "HostName" -> hostName
    if (application.nonEmpty)    params += "Application" -> application
    if (pid != 0)                params += "PID" -> pid
    if (tid != 0)                params += "TID" -> tid
    if (thread.nonEmpty)         params += "Thread" -> thread
    if (filename.nonEmpty)       params += "Filename" -> filename
    if (lineNumber != 0)         params += "Line" -> lineNumber
    if (function.nonEmpty)       params += "Function" -> function
    if (fromTime != Instant.MAX) params += "FromTime" -> fromTime
    if (toTime != Instant.MIN)   params += "ToTime" -> toTime
    if (level.nonEmpty)          params += "Level" -> level
    if (msgContains.nonEmpty)    params += "MsgContains" -> msgContains
    request("GetLogs", params)("LogMessageList", "LogMessages")
  }

  /* mutating POST methods */

  def addStorageGroupDir(storageGroup: String, dirName: String, hostName: String): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "GroupName" -> storageGroup,
      "DirName"   -> dirName,
      "HostName"  -> hostName
    )
    post("AddStorageGroupDir", params)()
  }

  def removeStorageGroupDir(storageGroup: String, dirName: String, hostName: String): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "GroupName" -> storageGroup,
      "DirName"   -> dirName,
      "HostName"  -> hostName
    )
    post("RemoveStorageGroupDir", params)()
  }

  def putSetting(hostName: String, key: String, value: String): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "HostName" -> hostName,
      "Key"      -> key,
      "Value"    -> value
    )
    post("PutSetting", params)()
  }

  def changePassword(userName: String, oldPassword: String, newPassword: String): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "UserName"    -> userName,
      "OldPassword" -> oldPassword,
      "NewPassword" -> newPassword
    )
    post("ChangePassword", params)()
  }

  def testDbSettings(hostName: String, userName: String, password: String, dbName: String, dbPort: Int): ServiceResult[Boolean] = {
    var params: Map[String, Any] = Map(
      "HostName" -> hostName,
      "UserName" -> userName,
      "Password" -> password
    )
    if (dbName.nonEmpty) params += "DBName" -> dbName
    if (dbPort != 0)     params += "dbPort" -> dbPort
    post("TestDBSettings", params)()
  }

  def sendMessage(message: String, address: String, udpPort: Int, timeout: Duration): ServiceResult[Boolean] = {
    var params: Map[String, Any] = Map("Message" -> message)
    if (address.nonEmpty) params += "Address" -> address
    if (udpPort != 0)     params += "udpPort" -> udpPort
    if (!timeout.isZero)  params += "Timeout" -> timeout.getSeconds
    post("SendMessage", params)()
  }

  def sendNotification(
    message: String,
    origin: String,
    description: String,
    extra: String,
    progressText: String,
    progress: Float,
    fullScreen: Boolean,
    timeout: Duration,
    notifyType: NotificationType,
    priority: NotificationPriority,
    visibility: NotificationVisibility,
    address: String,
    udpPort: Int
  ): ServiceResult[Boolean] = {
    var params: Map[String, Any] = Map("Message" -> message)
    if (origin.nonEmpty)       params +=       "Origin" -> origin
    if (description.nonEmpty)  params +=  "Description" -> description
    if (extra.nonEmpty)        params +=        "Extra" -> extra
    if (progressText.nonEmpty) params += "ProgressText" -> progressText
    if (progress != 0f)        params +=     "Progress" -> progress
    if (fullScreen)            params +=   "Fullscreen" -> fullScreen
    if (!timeout.isZero)       params +=      "Timeout" -> timeout.getSeconds
    if (notifyType != NotificationType.New)       params +=       "Type" -> notifyType.toString.toLowerCase
    if (priority != NotificationPriority.Default) params +=   "Priority" -> priority.id
    if (visibility != NotificationVisibility.All) params += "Visibility" -> visibility.id
    if (address.nonEmpty)      params +=      "Address" -> address
    if (udpPort != 0)          params +=      "udpPort" -> udpPort
    post("SendNotification", params)()
  }

  def backupDatabase(): ServiceResult[Boolean] = {
    post("BackupDatabase")()
  }

  def checkDatabase(repair: Boolean): ServiceResult[Boolean] = {
    var params: Map[String, Any] = Map.empty
    if (repair) params += "Repair" -> repair
    post("CheckDatabase", params)()
  }

  def profileSubmit(): ServiceResult[Boolean] = {
    post("ProfileSubmit")()
  }

  def profileDelete(): ServiceResult[Boolean] = {
    post("ProfileDelete")()
  }

  def profileUrl: ServiceResult[URI] = {
    request("ProfileURL")("String")
  }

  def profileUpdated: ServiceResult[String] = {
    request("ProfileUpdated")()
  }

  def profileText(): ServiceResult[String] = {
    request("ProfileText")()
  }
}
