package mythtv

import services.Service.ServiceFailure

package object services {
  type ServiceResult[T] = Either[ServiceFailure, T]
}
