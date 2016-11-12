package mythtv
package connection
package myth

final case class FileTransferId(id: Int) extends AnyVal

trait FileTransfer {
  def fileSize: Long
  def fileName: String
  def storageGroup: String
}
