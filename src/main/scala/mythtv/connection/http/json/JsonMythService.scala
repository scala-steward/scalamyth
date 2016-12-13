package mythtv
package connection
package http
package json

import java.net.URI
import java.time.{ Duration, Instant }

import scala.util.Try

import spray.json.DefaultJsonProtocol.{ listFormat, StringJsonFormat }

import model._
import util.{ MythDateTime, URIFactory }
import services.{ MythService, ServiceResult }
import EnumTypes.{ NotificationPriority, NotificationType, NotificationVisibility }
import RichJsonObject._

private[json] trait LabelValue {
  def label: String
  def value: String
}

class JsonMythService(conn: BackendJsonConnection)
  extends JsonBackendService(conn)
     with MythService {

  def getBackendInfo: ServiceResult[BackendDetails] = {
    for {
      response <- request("GetBackendInfo")
      root     <- responseRoot(response, "BackendInfo")
      result   <- Try(root.convertTo[BackendDetails])
    } yield result
  }

  def getConnectionInfo(pin: String): ServiceResult[ConnectionInfo] = {
    val params: Map[String, Any] = Map("Pin" -> pin)
    for {
      response <- post("GetConnectionInfo", params)
      root     <- responseRoot(response, "ConnectionInfo")
      result   <- Try(root.convertTo[ConnectionInfo])
    } yield result
  }

  def getFrontends(onlyOnline: Boolean): ServiceResult[List[KnownFrontendInfo]] = {
    var params: Map[String, Any] = Map.empty
    if (onlyOnline) params += "OnLine" -> onlyOnline
    for {
      response <- request("GetFrontends", params)
      root     <- responseRoot(response, "FrontendList", "Frontends")
      result   <- Try(root.convertTo[List[KnownFrontendInfo]])
    } yield result
  }

  def getHostName: ServiceResult[String] = {
    for {
      response <- request("GetHostName")
      root     <- responseRoot(response, "String")
      result   <- Try(root.convertTo[String])
    } yield result
  }

  def getHosts: ServiceResult[List[String]] = {
    for {
      response <- request("GetHosts")
      root     <- responseRoot(response, "StringList")
      result   <- Try(root.convertTo[List[String]])
    } yield result
  }

  def getKeys: ServiceResult[List[String]] = {
    for {
      response <- request("GetKeys")
      root     <- responseRoot(response, "StringList")
      result   <- Try(root.convertTo[List[String]])
    } yield result
  }

  def getSettings(hostName: String, key: String): ServiceResult[Settings] = {
    var params: Map[String, Any] = Map.empty
    if (hostName.nonEmpty) params += "HostName" -> hostName
    if (key.nonEmpty)      params += "Key" -> key
    for {
      response <- request("GetSetting", params)
      root     <- responseRoot(response, "SettingList")
      result   <- Try(root.convertTo[Settings])
    } yield result
  }

  def getSettingList(hostName: String = ""): ServiceResult[Settings] = {
    var params: Map[String, Any] = Map.empty
    if (hostName.nonEmpty) params += "HostName" -> hostName
    for {
      response <- request("GetSettingList", params)
      root     <- responseRoot(response, "SettingList")
      result   <- Try(root.convertTo[Settings])
    } yield result
  }

  def getStorageGroupDirs(hostName: String, groupName: String): ServiceResult[List[StorageGroupDir]] = {
    var params: Map[String, Any] = Map.empty
    if (hostName.nonEmpty) params += "HostName" -> hostName
    if (groupName.nonEmpty) params += "GroupName" -> groupName
    for {
      response <- request("GetStorageGroupDirs", params)
      root     <- responseRoot(response, "StorageGroupDirList", "StorageGroupDirs")
      result   <- Try(root.convertTo[List[StorageGroupDir]])
    } yield result
  }

  def getFormatDate(dateTime: MythDateTime, shortDate: Boolean = false): ServiceResult[String] = {
    var params: Map[String, Any] = Map("Date" -> dateTime.toIsoFormat)
    if (shortDate) params += "ShortDate" -> shortDate
    for {
      response <- request("GetFormatDate", params)
      root     <- responseRoot(response, "String")
      result   <- Try(root.convertTo[String])
    } yield result
  }

  def getFormatDateTime(dateTime: MythDateTime, shortDate: Boolean = false): ServiceResult[String] = {
    var params: Map[String, Any] = Map("DateTime" -> dateTime.toIsoFormat)
    if (shortDate) params += "ShortDate" -> shortDate
    for {
      response <- request("GetFormatDateTime", params)
      root     <- responseRoot(response, "String")
      result   <- Try(root.convertTo[String])
    } yield result
  }

  def getFormatTime(dateTime: MythDateTime): ServiceResult[String] = {
    val params: Map[String, Any] = Map("Time" -> dateTime.toIsoFormat)
    for {
      response <- request("GetFormatTime", params)
      root     <- responseRoot(response, "String")
      result   <- Try(root.convertTo[String])
    } yield result
  }

  def getTimeZone: ServiceResult[TimeZoneInfo] = {
    for {
      response <- request("GetTimeZone")
      root     <- responseRoot(response, "TimeZoneInfo")
      result   <- Try(root.convertTo[TimeZoneInfo])
    } yield result
  }

  def parseIsoDateString(dateTimeString: String): ServiceResult[MythDateTime] = {
    val params: Map[String, Any] = Map("DateTime" -> dateTimeString)
    for {
      response <- request("ParseISODateString", params)
      root     <- responseRoot(response, "DateTime")
      result   <- Try(MythDateTime.fromIso(root.convertTo[String]))
    } yield result
  }

  def getLogHostNames: ServiceResult[List[String]] = {
    for {
      response <- request("GetLogs")
      root     <- responseRoot(response, "LogMessageList", "HostNames")
      result   <- Try(root.convertTo[List[LabelValue]] map (_.value))
    } yield result
  }

  def getLogApplications: ServiceResult[List[String]] = {
    for {
      response <- request("GetLogs")
      root     <- responseRoot(response, "LogMessageList", "Applications")
      result   <- Try(root.convertTo[List[LabelValue]] map (_.value))
    } yield result
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
    for {
      response <- request("GetLogs", params)
      root     <- responseRoot(response, "LogMessageList", "LogMessages")
      result   <- Try(root.convertTo[List[LogMessage]])
    } yield result
  }

  /* mutating POST methods */

  def addStorageGroupDir(storageGroup: String, dirName: String, hostName: String): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "GroupName" -> storageGroup,
      "DirName"   -> dirName,
      "HostName"  -> hostName
    )
    for {
      response <- post("AddStorageGroupDir", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def removeStorageGroupDir(storageGroup: String, dirName: String, hostName: String): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "GroupName" -> storageGroup,
      "DirName"   -> dirName,
      "HostName"  -> hostName
    )
    for {
      response <- post("RemoveStorageGroupDir", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def putSetting(hostName: String, key: String, value: String): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "HostName" -> hostName,
      "Key"      -> key,
      "Value"    -> value
    )
    for {
      response <- post("PutSetting", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def changePassword(userName: String, oldPassword: String, newPassword: String): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "UserName"    -> userName,
      "OldPassword" -> oldPassword,
      "NewPassword" -> newPassword
    )
    for {
      response <- post("ChangePassword", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def testDbSettings(hostName: String, userName: String, password: String, dbName: String, dbPort: Int): ServiceResult[Boolean] = {
    var params: Map[String, Any] = Map(
      "HostName" -> hostName,
      "UserName" -> userName,
      "Password" -> password
    )
    if (dbName.nonEmpty) params += "DBName" -> dbName
    if (dbPort != 0)     params += "dbPort" -> dbPort
    for {
      response <- post("TestDBSettings", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def sendMessage(message: String, address: String, udpPort: Int, timeout: Duration): ServiceResult[Boolean] = {
    var params: Map[String, Any] = Map("Message" -> message)
    if (address.nonEmpty) params += "Address" -> address
    if (udpPort != 0)     params += "udpPort" -> udpPort
    if (!timeout.isZero)  params += "Timeout" -> timeout.getSeconds
    for {
      response <- post("SendMessage", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
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
    for {
      response <- post("SendNotification", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def backupDatabase(): ServiceResult[Boolean] = {
    for {
      response <- post("BackupDatabase")
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def checkDatabase(repair: Boolean): ServiceResult[Boolean] = {
    var params: Map[String, Any] = Map.empty
    if (repair) params += "Repair" -> repair
    for {
      response <- post("CheckDatabase", params)
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def profileSubmit(): ServiceResult[Boolean] = {
    for {
      response <- post("ProfileSubmit")
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def profileDelete(): ServiceResult[Boolean] = {
    for {
      response <- post("ProfileDelete")
      root     <- responseRoot(response)
      result   <- Try(root.booleanField("bool"))
    } yield result
  }

  def profileUrl: ServiceResult[URI] = {
    for {
      response <- request("ProfileURL")
      root     <- responseRoot(response, "String")
      result   <- Try(URIFactory(root.convertTo[String]))
    } yield result
  }

  def profileUpdated: ServiceResult[String] = {
    for {
      response <- request("ProfileUpdated")
      root     <- responseRoot(response, "String")
      result   <- Try(root.convertTo[String])
    } yield result
  }

  def profileText(): ServiceResult[String] = {
    for {
      response <- request("ProfileText")
      root     <- responseRoot(response, "String")
      result   <- Try(root.convertTo[String])
    } yield result
  }
}
