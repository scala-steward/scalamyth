package mythtv
package connection
package myth

import model.IntegerIdentifier

final case class FileTransferId(id: Int) extends AnyVal with IntegerIdentifier

trait FileTransfer {
  def fileSize: Long
  def fileName: String
  def storageGroup: String
}
