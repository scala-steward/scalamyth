package mythtv
package connection
package myth
package data

import model.{ FreeSpace, StorageGroupId }
import util.{ ByteCount, DecimalByteCount }

private[myth] class BackendFreeSpace(data: Seq[String]) extends FreeSpace {
  import BackendFreeSpace.FIELD_ORDER

  // assumes data.length >= FIELD_ORDER.length, or else some fields will be missing
  val fields: Map[String, String] = (FIELD_ORDER zip data).toMap

  def apply(fieldName: String): String = fields(fieldName)

  def get(fieldName: String): Option[String] = fields.get(fieldName)

  override def toString: String = s"<FreeSpace $path@$host: $freeSpace>"

  /* Convenience accessors with proper type */
  def host: String = fields("host")
  def path: String = fields("path")
  lazy val isLocal: Boolean = fields("isLocal").toInt != 0
  lazy val diskNumber: Int = fields("diskNumber").toInt
  lazy val sGroupId: StorageGroupId = StorageGroupId(fields("sGroupId").toInt)
  lazy val blockSize: ByteCount = DecimalByteCount(fields("blockSize").toLong)
  lazy val totalSpace: ByteCount = DecimalByteCount(fields("totalSpace").toLong * 1024)
  lazy val usedSpace: ByteCount = DecimalByteCount(fields("usedSpace").toLong * 1024)
  lazy val freeSpace: ByteCount = totalSpace - usedSpace
}

private[myth] object BackendFreeSpace {
  final val FIELD_ORDER = IndexedSeq(
    "host", "path", "isLocal", "diskNumber", "sGroupId", "blockSize", "totalSpace", "usedSpace"
  )

  def apply(data: Seq[String]): BackendFreeSpace = new BackendFreeSpace(data)
}
