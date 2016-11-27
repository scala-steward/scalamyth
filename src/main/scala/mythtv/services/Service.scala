package mythtv
package services

trait Service {
  def serviceName: String
}

trait BackendService extends Service
trait FrontendService extends Service

object Service {
  sealed trait ServiceFailure
  object ServiceFailure {
    case object ServiceNoResult extends ServiceFailure
    case object ServiceFailureUnknown extends ServiceFailure
    final case class ServiceFailureThrowable(ex: Throwable) extends ServiceFailure
  }
}
