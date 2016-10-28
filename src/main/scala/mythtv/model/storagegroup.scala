package mythtv
package model

import util.ByteCount

final case class StorageGroupId(id: Int) extends AnyVal

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

// TODO storage group stuff ...

trait StorageGroupDir  // TODO move
