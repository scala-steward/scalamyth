package mythtv
package connection

import java.nio.{ ByteBuffer, CharBuffer }
import java.nio.charset.CharsetEncoder

import myth.MythProtocol.MythProtocolFailure

package object myth {
  type MythProtocolResult[T] = Either[MythProtocolFailure, T]

  private[myth] type BackendObjectSerializer[T] = GenericBackendObjectSerializer[T, _, _]

  // Allows for the same encode(String) convenience method that Charset provides
  implicit class CharsetStringEncoder(val enc: CharsetEncoder) extends AnyVal {
    def encode(str: String): ByteBuffer = enc.encode(CharBuffer.wrap(str))
  }

  implicit class MythProtocolResultGetter[T](val res: MythProtocolResult[T]) extends AnyVal {
    import MythProtocolFailure._

    def isSuccess: Boolean = res.isRight
    def isFailure: Boolean = res.isLeft

    def get: T = res match {
      case Right(result) => result
      case Left(fail) => fail match {
        case MythProtocolNoResult => throw new NoSuchElementException                     // TODO handle better
        case MythProtocolFailureUnknown => throw new RuntimeException("unknown failure")  // TODO handle better
        case MythProtocolFailureMessage(msg) => throw new RuntimeException(msg)           // TODO handle better
        case MythProtocolFailureThrowable(ex) => throw ex
      }
    }
  }
}
