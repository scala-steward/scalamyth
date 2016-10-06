package mythtv
package connection

trait NetworkConnection {
  def host: String
  def port: Int
}
