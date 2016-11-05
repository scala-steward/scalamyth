package mythtv
package connection
package http
package json

import java.time.{ Duration, Instant }

import spray.json.DefaultJsonProtocol

import services.MythService
import model.{ Settings, StorageGroupDir, TimeZoneInfo }
import RichJsonObject._

import services.LogMessage // TODO temporary

class JsonMythService(conn: BackendJsonConnection)
  extends JsonBackendService(conn)
     with MythService {
  def getHostName: String = {
    import DefaultJsonProtocol.StringJsonFormat
    val response = request("GetHostName")
    val root = responseRoot(response, "String")
    root.convertTo[String]
  }

  def getHosts: List[String] = {
    val response = request("GetHosts")
    val root = responseRoot(response)
    root.convertTo[List[String]]
  }

  def getKeys: List[String] = {
    val response = request("GetKeys")
    val root = responseRoot(response)
    root.convertTo[List[String]]
  }

  def getSettings(hostName: String, key: String): Settings = {
    var params: Map[String, Any] = Map.empty
    if (hostName.nonEmpty) params += "HostName" -> hostName
    if (key.nonEmpty)      params += "Key" -> key
    val response = request("GetSetting", params)
    val root = responseRoot(response, "SettingList")
    root.convertTo[Settings]
  }

  def getStorageGroupDirs(hostName: String, groupName: String): List[StorageGroupDir] = {
    var params: Map[String, Any] = Map.empty
    if (hostName.nonEmpty) params += "HostName" -> hostName
    if (groupName.nonEmpty) params += "GroupName" -> groupName
    val response = request("GetStorageGroupDirs", params)
    val root = responseRoot(response, "StorageGroupDirList")
    root.convertTo[List[StorageGroupDir]]
  }

  def getTimeZone: TimeZoneInfo = {
    val response = request("GetTimeZone")
    val root = responseRoot(response, "TimeZoneInfo")
    root.convertTo[TimeZoneInfo]
  }

  def getLogs(
    hostName: String,
    application: String,
    pid: Int,
    tid: Int,
    thread: String,
    filename: String,
    line: Int,
    function: String,
    fromTime: Instant,
    toTime: Instant,
    level: String,
    msgContains: String
  ): List[LogMessage] = {
    var params: Map[String, Any] = Map.empty
    if (hostName.nonEmpty)       params += "HostName" -> hostName
    if (application.nonEmpty)    params += "Application" -> application
    if (pid != 0)                params += "PID" -> pid
    if (tid != 0)                params += "TID" -> tid
    if (thread.nonEmpty)         params += "Thread" -> thread
    if (filename.nonEmpty)       params += "Filename" -> filename
    if (line != 0)               params += "Line" -> line
    if (function.nonEmpty)       params += "Function" -> function
    if (fromTime != Instant.MAX) params += "FromTime" -> fromTime
    if (toTime != Instant.MIN)   params += "ToTime" -> toTime
    if (level.nonEmpty)          params += "Level" -> level
    if (msgContains.nonEmpty)    params += "MsgContains" -> msgContains
    val response = request("GetLogs", params)
    val root = responseRoot(response, "LogMessageList") // TODO ???
    ???
  }

  /* mutating POST methods */

  def addStorageGroupDir(storageGroup: String, dirName: String, hostName: String): Boolean = {
    val params: Map[String, Any] = Map(
      "GroupName" -> storageGroup,
      "DirName"   -> dirName,
      "HostName"  -> hostName
    )
    val response = post("AddStorageGroupDir", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  def removeStorageGroupDir(storageGroup: String, dirName: String, hostName: String): Boolean = {
    val params: Map[String, Any] = Map(
      "GroupName" -> storageGroup,
      "DirName"   -> dirName,
      "HostName"  -> hostName
    )
    val response = post("RemoveStorageGroupDir", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  def putSetting(hostName: String, key: String, value: String): Boolean = {
    val params: Map[String, Any] = Map(
      "HostName" -> hostName,
      "Key"      -> key,
      "Value"    -> value
    )
    val response = post("PutSetting", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  def changePassword(userName: String, oldPassword: String, newPassword: String): Boolean = {
    val params: Map[String, Any] = Map(
      "UserName"    -> userName,
      "OldPassword" -> oldPassword,
      "NewPassword" -> newPassword
    )
    val response = post("ChangePassword", params)
    val root = responseRoot(response)
    root.booleanField("bool")   // TODO test
  }

  def testDbSettings(hostName: String, userName: String, password: String, dbName: String, dbPort: Int): Boolean = {
    var params: Map[String, Any] = Map(
      "HostName" -> hostName,
      "UserName" -> userName,
      "Password" -> password
    )
    if (dbName.nonEmpty) params += "DBName" -> dbName
    if (dbPort != 0)     params += "dbPort" -> dbPort
    val response = post("TestDBSettings", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  def sendMessage(message: String, address: String, udpPort: Int, timeout: Duration): Boolean = {
    var params: Map[String, Any] = Map("Message" -> message)
    if (address.nonEmpty) params += "Address" -> address
    if (udpPort != 0)     params += "udpPort" -> udpPort
    if (!timeout.isZero)  params += "Timeout" -> timeout.getSeconds
    val response = post("SendMessage", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  //def sendNotification(....): Boolean = ???

  def backupDatabase(): Boolean = {
    val response = post("BackupDatabase")
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  def checkDatabase(repair: Boolean): Boolean = {
    var params: Map[String, Any] = Map.empty
    if (repair) params += "Repair" -> repair
    val response = post("CheckDatabase", params)
    val root = responseRoot(response)
    root.booleanField("bool")
  }

  def profileSubmit(): Boolean = {
    val response = post("ProfileSubmit")
    val root = responseRoot(response)
    root.booleanField("bool")   // TODO test
  }

  def profileDelete(): Boolean = {
    val response = post("ProfileDelete")
    val root = responseRoot(response)
    root.booleanField("bool")   // TODO test
  }

// TODO are the three below GET methods?

  def profileUrl: String = ???

  def profileUpdated: String = ???

  def profileText: String = ???
}
