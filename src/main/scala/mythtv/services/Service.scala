package mythtv
package services

trait Service {
  def serviceName: String
}

/**
  * A marker trait used to indicate services provided by the MythTV backend.
  */
trait BackendService extends Service

/**
  * A marker trait used to indicate services provided by the MythTV frontend.
  */
trait FrontendService extends Service

object Service {
  sealed trait ServiceFailure
  object ServiceFailure {
    case object ServiceNoResult extends ServiceFailure
    case object ServiceFailureUnknown extends ServiceFailure
    final case class ServiceFailureThrowable(ex: Throwable) extends ServiceFailure
  }
}
