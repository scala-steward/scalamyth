package mythtv
package connection
package http
package json

import spray.json.DefaultJsonProtocol

import services.MythService
import model.{ Settings, StorageGroupDir, TimeZoneInfo }

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

  def getStorageGroupDirs(hostName: String, groupName: String): List[StorageGroupDir] = ???

  def getTimeZone: TimeZoneInfo = {
    val response = request("GetTimeZone")
    val root = responseRoot(response, "TimeZoneInfo")
    root.convertTo[TimeZoneInfo]
  }


  // getLogs(....)

  /* mutating POST methods */

  def addStorageGroupDir(storageGroup: String, dirName: String, hostName: String): Boolean = ???

  def removeStorageGroupDir(storageGroup: String, dirName: String, hostName: String): Boolean = ???

  def putSetting(hostName: String, key: String, value: String): Boolean = ???

  def changePassword(userName: String, oldPassword: String, newPassword: String): Boolean = ???

  def testDbSettings(hostName: String, userName: String, password: String, dbName: String, dbPort: Int): Boolean = ???

  def sendMessage(message: String, address: String, udpPort: Int, timeout: Int): Boolean = ???

  //def sendNotification(....): Boolean = ???

  def backupDatabase(): Boolean = ???

  def checkDatabase(repair: Boolean = false): Boolean = ???


  def profileSubmit(): Boolean = ???

  def profileDelete(): Boolean = ???

// TODO are the three below GET methods?

  def profileUrl: String = ???

  def profileUpdated: String = ???

  def profileText: String = ???
}
