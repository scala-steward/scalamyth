// SPDX-License-Identifier: LGPL-2.1-only
/*
 * Service.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package services

trait Service {
  def serviceName: String
  def endpoints: Map[String, ServiceEndpoint]
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
