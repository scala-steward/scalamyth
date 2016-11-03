package mythtv
package connection
package http
package json

import spray.json.DefaultJsonProtocol

import services.MythService
import model.{ Settings, StorageGroup, TimeZoneInfo }
import RichJsonObject._

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

  def getSetting(hostName: String, key: String): Settings = {
    var params: Map[String, Any] = Map("HostName" -> hostName)
    if (key.nonEmpty) params += "Key" -> key
    val response = request("GetSetting", params)
    val root = responseRoot(response, "SettingList")
    root.convertTo[Settings]
  }

  def getStorageGroupDirs(hostName: String, groupName: String): List[StorageGroup] = {
    var params: Map[String, Any] = Map.empty
    if (hostName.nonEmpty) params += "HostName" -> hostName
    if (groupName.nonEmpty) params += "GroupName" -> groupName
    val response = request("GetStorageGroupDirs", params)
    val root = responseRoot(response, "StorageGroupDirList")
    root.convertTo[List[StorageGroup]]
  }

  def getTimeZone: TimeZoneInfo = {
    val response = request("GetTimeZone")
    val root = responseRoot(response, "TimeZoneInfo")
    root.convertTo[TimeZoneInfo]
  }


  // getLogs(....)

  /* mutating POST methods */

  def addStorageGroupDir(storageGroup: String, dirName: String, hostName: String): Boolean = {
    val params: Map[String, Any] = Map(
      "GroupName" -> storageGroup,
      "DirName"   -> dirName,
      "HostName"  -> hostName
    )
    val response = post("AddStorageGroupDir", params)
    val root = responseRoot(response)
    root.booleanField("bool")   // TODO test
  }

  def removeStorageGroupDir(storageGroup: String, dirName: String, hostName: String): Boolean = {
    val params: Map[String, Any] = Map(
      "GroupName" -> storageGroup,
      "DirName"   -> dirName,
      "HostName"  -> hostName
    )
    val response = post("RemoveStorageGroupDir", params)
    val root = responseRoot(response)
    root.booleanField("bool")   // TODO test
  }

  def putSetting(hostName: String, key: String, value: String): Boolean = {
    val params: Map[String, Any] = Map(
      "HostName" -> hostName,
      "Key"      -> key,
      "Value"    -> value
    )
    val response = post("PutSetting", params)
    val root = responseRoot(response)
    root.booleanField("bool")   // TODO test
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

  def testDbSettings(hostName: String, userName: String, password: String, dbName: String, dbPort: Int): Boolean = ???

  def sendMessage(message: String, address: String, udpPort: Int, timeout: Int): Boolean = ???

  //def sendNotification(....): Boolean = ???

  def backupDatabase(): Boolean = {
    val response = post("BackupDatabase")
    val root = responseRoot(response)
    root.booleanField("bool")   // TODO test
  }

  def checkDatabase(repair: Boolean): Boolean = {
    var params: Map[String, Any] = Map.empty
    if (repair) params += "Repair" -> repair
    val response = post("CheckDatabase", params)
    val root = responseRoot(response)
    root.booleanField("bool")   // TODO test
  }

  def profileSubmit(): Boolean = ???

  def profileDelete(): Boolean = ???

// TODO are the three below GET methods?

  def profileUrl: String = ???

  def profileUpdated: String = ???

  def profileText: String = ???
}
