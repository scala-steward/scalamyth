package mythtv
package services

import scala.language.higherKinds

trait ServiceProvider[F[T <: Service] <: ServiceFactory[T]] {
  def service[A <: Service](host: String)(implicit factory: F[A]): A = factory(host)
  def service[A <: Service](host: String, port: Int)(implicit factory: F[A]): A = factory(host, port)
}

trait ServiceFactory[A <: Service] {
  def apply(host: String): A
  def apply(host: String, port: Int): A
}
