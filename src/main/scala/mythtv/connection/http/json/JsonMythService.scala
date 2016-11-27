package mythtv
package connection
package http
package json

import java.time.{ Duration, Instant }

import scala.util.Try

import spray.json.DefaultJsonProtocol

import services.{ MythService, ServiceResult }
import model.{ ConnectionInfo, Settings, StorageGroupDir, TimeZoneInfo }
import RichJsonObject._

import services.LogMessage // TODO temporary

class JsonMythService(conn: BackendJsonConnection)
  extends JsonBackendService(conn)
     with MythService {

  def getConnectionInfo(pin: String): ServiceResult[ConnectionInfo] = {
    val params: Map[String, Any] = Map("Pin" -> pin)
    for {
      response <- Try( post("GetConnectionInfo", params) )
      root     <- Try( responseRoot(response, "ConnectionInfo") )
      result   <- Try( root.convertTo[ConnectionInfo] )
    } yield result
  }

  def getHostName: ServiceResult[String] = {
    import DefaultJsonProtocol.StringJsonFormat
    for {
      response <- Try( request("GetHostName") )
      root     <- Try( responseRoot(response, "String") )
      result   <- Try( root.convertTo[String] )
    } yield result
  }

  def getHosts: ServiceResult[List[String]] = {
    for {
      response <- Try( request("GetHosts") )
      root     <- Try( responseRoot(response) )
      result   <- Try( root.convertTo[List[String]] )
    } yield result
  }

  def getKeys: ServiceResult[List[String]] = {
    for {
      response <- Try( request("GetKeys") )
      root     <- Try( responseRoot(response) )
      result   <- Try( root.convertTo[List[String]] )
    } yield result
  }

  def getSettings(hostName: String, key: String): ServiceResult[Settings] = {
    var params: Map[String, Any] = Map.empty
    if (hostName.nonEmpty) params += "HostName" -> hostName
    if (key.nonEmpty)      params += "Key" -> key
    for {
      response <- Try( request("GetSetting", params) )
      root     <- Try( responseRoot(response, "SettingList") )
      result   <- Try( root.convertTo[Settings] )
    } yield result
  }

  def getStorageGroupDirs(hostName: String, groupName: String): ServiceResult[List[StorageGroupDir]] = {
    var params: Map[String, Any] = Map.empty
    if (hostName.nonEmpty) params += "HostName" -> hostName
    if (groupName.nonEmpty) params += "GroupName" -> groupName
    for {
      response <- Try( request("GetStorageGroupDirs", params) )
      root     <- Try( responseRoot(response, "StorageGroupDirList") )
      result   <- Try( root.convertTo[List[StorageGroupDir]] )
    } yield result
  }

  def getTimeZone: ServiceResult[TimeZoneInfo] = {
    for {
      response <- Try( request("GetTimeZone") )
      root     <- Try( responseRoot(response, "TimeZoneInfo") )
      result   <- Try( root.convertTo[TimeZoneInfo] )
    } yield result
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
  ): ServiceResult[List[LogMessage]] = {
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
    for {
      response <- Try( request("GetLogs", params) )
      root     <- Try( responseRoot(response, "LogMessageList") ) // TODO ???
      result   <- Try( ??? )
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
      response <- Try( post("AddStorageGroupDir", params) )
      root     <- Try( responseRoot(response) )
      result   <- Try( root.booleanField("bool") )
    } yield result
  }

  def removeStorageGroupDir(storageGroup: String, dirName: String, hostName: String): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "GroupName" -> storageGroup,
      "DirName"   -> dirName,
      "HostName"  -> hostName
    )
    for {
      response <- Try( post("RemoveStorageGroupDir", params) )
      root     <- Try( responseRoot(response) )
      result   <- Try( root.booleanField("bool") )
    } yield result
  }

  def putSetting(hostName: String, key: String, value: String): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "HostName" -> hostName,
      "Key"      -> key,
      "Value"    -> value
    )
    for {
      response <- Try( post("PutSetting", params) )
      root     <- Try( responseRoot(response) )
      result   <- Try( root.booleanField("bool") )
    } yield result
  }

  def changePassword(userName: String, oldPassword: String, newPassword: String): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "UserName"    -> userName,
      "OldPassword" -> oldPassword,
      "NewPassword" -> newPassword
    )
    for {
      response <- Try( post("ChangePassword", params) )
      root     <- Try( responseRoot(response) )
      result   <- Try( root.booleanField("bool") )  // TODO test
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
      response <- Try( post("TestDBSettings", params) )
      root     <- Try( responseRoot(response) )
      result   <- Try( root.booleanField("bool") )
    } yield result
  }

  def sendMessage(message: String, address: String, udpPort: Int, timeout: Duration): ServiceResult[Boolean] = {
    var params: Map[String, Any] = Map("Message" -> message)
    if (address.nonEmpty) params += "Address" -> address
    if (udpPort != 0)     params += "udpPort" -> udpPort
    if (!timeout.isZero)  params += "Timeout" -> timeout.getSeconds
    for {
      response <- Try( post("SendMessage", params) )
      root     <- Try( responseRoot(response) )
      result   <- Try( root.booleanField("bool") )
    } yield result
  }

  //def sendNotification(....): Boolean = ???

  def backupDatabase(): ServiceResult[Boolean] = {
    for {
      response <- Try( post("BackupDatabase") )
      root     <- Try( responseRoot(response) )
      result   <- Try( root.booleanField("bool") )
    } yield result
  }

  def checkDatabase(repair: Boolean): ServiceResult[Boolean] = {
    var params: Map[String, Any] = Map.empty
    if (repair) params += "Repair" -> repair
    for {
      response <- Try( post("CheckDatabase", params) )
      root     <- Try( responseRoot(response) )
      result   <- Try( root.booleanField("bool") )
    } yield result
  }

  def profileSubmit(): ServiceResult[Boolean] = {
    for {
      response <- Try( post("ProfileSubmit") )
      root     <- Try( responseRoot(response) )
      result   <- Try( root.booleanField("bool") )  // TODO test
    } yield result
  }

  def profileDelete(): ServiceResult[Boolean] = {
    for {
      response <- Try( post("ProfileDelete") )
      root     <- Try( responseRoot(response) )
      result   <- Try( root.booleanField("bool") )  // TODO test
    } yield result
  }

// TODO are the three below GET methods?

  def profileUrl: ServiceResult[String] = ???

  def profileUpdated: ServiceResult[String] = ???

  def profileText: ServiceResult[String] = ???
}
