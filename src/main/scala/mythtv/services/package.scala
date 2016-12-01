package mythtv

import services.Service.ServiceFailure

package object services {
  type ServiceResult[T] = Either[ServiceFailure, T]

  implicit class ServiceResultGetter[T](val res: ServiceResult[T]) extends AnyVal {
    import ServiceFailure._

    def isSuccess: Boolean = res.isRight
    def isFailure: Boolean = res.isLeft

    def get: T = res match {
      case Right(result) => result
      case Left(fail) => fail match {
        case ServiceNoResult => throw new NoSuchElementException                    // TODO handle better
        case ServiceFailureUnknown => throw new RuntimeException("unknown failure") // TODO handle better
        case ServiceFailureThrowable(ex) => throw ex
      }
    }
  }
}
