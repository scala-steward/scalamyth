package mythtv
package services

trait ServiceProvider[A] {
  def instance[B <: A]: B  // TODO this will not work well if I extend multiple service providers in a class!?
}
