package mythtv
package services

trait Service {
  def serviceName: String
}

trait BackendService extends Service
trait FrontendService extends Service

trait DataBytes  // TODO placeholder

