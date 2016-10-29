package mythtv
package connection
package myth
package data

import model.{ FreeSpace, StorageGroupId }
import util.{ ByteCount, DecimalByteCount }

private[myth] class BackendFreeSpace(data: Seq[String], fieldOrder: IndexedSeq[String])
    extends GenericBackendObject(data, fieldOrder) with FreeSpace {

  override def toString: String = s"<FreeSpace $path@$host: $freeSpace>"

  /* Convenience accessors with proper type */
  def host: String = fields("host")
  def path: String = fields("path")
  def isLocal: Boolean = fields("isLocal").toInt != 0
  def diskNumber: Int = fields("diskNumber").toInt
  def sGroupId: StorageGroupId = StorageGroupId(fields("sGroupId").toInt)
  def blockSize: ByteCount = DecimalByteCount(fields("blockSize").toLong)
  def totalSpace: ByteCount = DecimalByteCount(fields("totalSpace").toLong * 1024)
  def usedSpace: ByteCount = DecimalByteCount(fields("usedSpace").toLong * 1024)
  def freeSpace: ByteCount = totalSpace - usedSpace
}

private[myth] trait BackendFreeSpaceFactory extends GenericBackendObjectFactory[BackendFreeSpace]
private[myth] trait FreeSpaceOtherSerializer extends BackendTypeSerializer[FreeSpace]

private[myth] object BackendFreeSpace extends BackendFreeSpaceFactory {
  final val FIELD_ORDER = IndexedSeq(
    "host", "path", "isLocal", "diskNumber", "sGroupId", "blockSize", "totalSpace", "usedSpace"
  )

  def apply(data: Seq[String]): BackendFreeSpace = new BackendFreeSpace(data, FIELD_ORDER)
}
