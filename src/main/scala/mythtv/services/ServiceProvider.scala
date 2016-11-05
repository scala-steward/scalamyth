package mythtv
package services

trait ServiceProvider[A] {
  def instance(host: String): A
  def instance(host: String, port: Int): A
}
