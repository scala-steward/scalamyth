package mythtv
package model

import util.ByteCount

trait FreeSpace {
  def host: String
  def path: String
  def isLocal: Boolean
  def diskNumber: Int
  def sGroupId: StorageGroupId
  def blockSize: ByteCount
  def totalSpace: ByteCount
  def usedSpace: ByteCount
  def freeSpace: ByteCount
}

final case class StorageGroupId(id: Int) extends AnyVal

trait StorageGroup {
  def id: StorageGroupId
  def groupName: String
  def hostName: String
  def dirName: String

  override def toString: String = s"<StorageGroup $id $groupName@$hostName: $dirName>"
}
