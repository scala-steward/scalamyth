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

final case class StorageGroupId(id: Int) extends AnyVal with IntegerIdentifier

trait StorageGroupDir {
  def id: StorageGroupId
  def groupName: String
  def hostName: String
  def dirName: String

  override def toString: String = s"<StorageGroupDir $id $groupName@$hostName: $dirName>"
}
