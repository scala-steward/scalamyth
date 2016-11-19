package mythtv
package connection
package http
package json

import services._

object JsonServiceProvider extends ServiceProvider[JsonServiceFactory]

trait JsonServiceFactory[S <: Service] extends ServiceFactory[S]
object JsonServiceFactory extends JsonServiceFactoryImplicits

private[json] trait GenericJsonServiceFactory[S <: Service, C <: JsonConnection] extends JsonServiceFactory[S] {
  def newInstance(conn: C): S
  def newConnection(host: String): C
  def newConnection(host: String, port: Int): C
  def apply(host: String): S = newInstance(newConnection(host))
  def apply(host: String, port: Int) = newInstance(newConnection(host, port))
}

private[json] trait BackendJsonServiceFactory[S <: BackendService]
    extends GenericJsonServiceFactory[S, BackendJsonConnection] {
  def newConnection(host: String)= new BackendJsonConnection(host)
  def newConnection(host: String, port: Int) = new BackendJsonConnection(host, port)
}

private[json] trait FrontendJsonServiceFactory[S <: FrontendService]
    extends GenericJsonServiceFactory[S, FrontendJsonConnection] {
  def newConnection(host: String) = new FrontendJsonConnection(host)
  def newConnection(host: String, port: Int) = new FrontendJsonConnection(host, port)
}

private[json] trait JsonServiceFactoryImplicits {
  implicit object JsonCaptureServiceFactory extends BackendJsonServiceFactory[CaptureService] {
    def newInstance(conn: BackendJsonConnection): CaptureService = new JsonCaptureService(conn)
  }

  implicit object JsonChannelServiceFactory extends BackendJsonServiceFactory[ChannelService] {
    def newInstance(conn: BackendJsonConnection): ChannelService = new JsonChannelService(conn)
  }

  implicit object JsonContentServiceFactory extends BackendJsonServiceFactory[ContentService] {
    def newInstance(conn: BackendJsonConnection): ContentService = new JsonContentService(conn)
  }

  implicit object JsonDvrServiceFactory extends BackendJsonServiceFactory[DvrService] {
    def newInstance(conn: BackendJsonConnection): DvrService = new JsonDvrService(conn)
  }

  implicit object JsonGuideServiceFactory extends BackendJsonServiceFactory[GuideService] {
    def newInstance(conn: BackendJsonConnection): GuideService = new JsonGuideService(conn)
  }

  implicit object JsonMythServiceFactory extends BackendJsonServiceFactory[MythService] {
    def newInstance(conn: BackendJsonConnection): MythService = new JsonMythService(conn)
  }

  implicit object JsonVideoServiceFactory extends BackendJsonServiceFactory[VideoService] {
    def newInstance(conn: BackendJsonConnection): VideoService = new JsonVideoService(conn)
  }

  implicit object JsonMythFrontendServiceFactory extends FrontendJsonServiceFactory[MythFrontendService] {
    def newInstance(conn: FrontendJsonConnection): MythFrontendService = new JsonMythFrontendService(conn)
  }
}
