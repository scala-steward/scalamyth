package mythtv
package connection
package http
package json

import scala.util.Try

import spray.json.JsValue

import services.{ Service, ServiceResult }

abstract class JsonService(conn: JsonConnection)
  extends Service
     with ServiceProtocol
     with ServiceResultReaderImplicits {

  override def connection: AbstractHttpConnection = conn
  override def converter: ServiceResultConverter = JsonResultConverter

  override def request[T: ServiceResultReader](endpoint: String, params: Map[String, Any])(f0: String, f1: String): ServiceResult[T] = {
    for {
      response <- requestJson(endpoint, params)
      root     <- responseRoot(response, f0, f1, implicitly[ServiceResultReader[T]].defaultField)
      result   <- Try(implicitly[ServiceResultReader[T]].read(root))
    } yield result
  }

  override def post[T: ServiceResultReader](endpoint: String, params: Map[String, Any])(f0: String, f1: String): ServiceResult[T] = {
    for {
      response <- postJson(endpoint, params)
      root     <- responseRoot(response, f0, f1, implicitly[ServiceResultReader[T]].defaultField)
      result   <- Try(implicitly[ServiceResultReader[T]].read(root))
    } yield result
  }

  def requestJson(endpoint: String, params: Map[String, Any] = Map.empty): Try[JsonResponse] =
    Try(conn.request(buildPath(endpoint, params)))

  def postJson(endpoint: String, params: Map[String, Any] = Map.empty): Try[JsonResponse] =
    Try(conn.post(buildPath(endpoint), params))

  def responseRoot(response: JsonResponse, f0: String, f1: String, default: String): Try[JsValue] = Try {
    val obj = response.json.asJsObject
    if (f0.isEmpty) {
      if (default.nonEmpty) obj.fields(default)
      else obj
    }
    else if (f1.isEmpty) obj.fields(f0)
    else obj.fields(f0).asJsObject.fields(f1)
  }
}

class JsonCaptureService(conn: BackendJsonConnection) extends JsonService(conn) with AbstractCaptureService
class JsonChannelService(conn: BackendJsonConnection) extends JsonService(conn) with AbstractChannelService
class JsonContentService(conn: BackendJsonConnection) extends JsonService(conn) with AbstractContentService
class JsonDvrService(conn: BackendJsonConnection) extends JsonService(conn) with AbstractDvrService
class JsonGuideService(conn: BackendJsonConnection) extends JsonService(conn) with AbstractGuideService
class JsonImageService(conn: BackendJsonConnection) extends JsonService(conn) with AbstractImageService
class JsonMythService(conn: BackendJsonConnection) extends JsonService(conn) with AbstractMythService
class JsonVideoService(conn: BackendJsonConnection) extends JsonService(conn) with AbstractVideoService

class JsonMythFrontendService(conn: FrontendJsonConnection) extends JsonService(conn) with AbstractMythFrontendService
