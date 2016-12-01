package mythtv
package connection
package http
package json

import services._

trait JsonServiceFactory[S <: Service] extends ServiceFactory[S]
object JsonServiceFactory extends JsonServiceFactoryImplicits

private[json] trait GenericJsonServiceFactory[S <: Service, C <: JsonConnection] extends JsonServiceFactory[S] {
  def newInstance(conn: C): S
  def newConnection(host: String): C
  def newConnection(host: String, port: Int): C
  def apply(host: String): S = newInstance(newConnection(host))
  def apply(host: String, port: Int): S = newInstance(newConnection(host, port))
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

object JsonServiceProvider extends ServiceProvider {
  private val f = JsonServiceFactory

  def captureService(host: String): CaptureService = f.JsonCaptureServiceFactory(host)
  def captureService(host: String, port: Int): CaptureService = f.JsonCaptureServiceFactory(host, port)

  def channelService(host: String): ChannelService = f.JsonChannelServiceFactory(host)
  def channelService(host: String, port: Int): ChannelService = f.JsonChannelServiceFactory(host, port)

  def contentService(host: String): ContentService = f.JsonContentServiceFactory(host)
  def contentService(host: String, port: Int): ContentService = f.JsonContentServiceFactory(host, port)

  def dvrService(host: String): DvrService = f.JsonDvrServiceFactory(host)
  def dvrService(host: String, port: Int): DvrService = f.JsonDvrServiceFactory(host, port)

  def guideService(host: String): GuideService = f.JsonGuideServiceFactory(host)
  def guideService(host: String, port: Int): GuideService = f.JsonGuideServiceFactory(host, port)

  def mythService(host: String): MythService = f.JsonMythServiceFactory(host)
  def mythService(host: String, port: Int): MythService = f.JsonMythServiceFactory(host, port)

  def videoService(host: String): VideoService = f.JsonVideoServiceFactory(host)
  def videoService(host: String, port: Int): VideoService = f.JsonVideoServiceFactory(host, port)

  def mythFrontendService(host: String): MythFrontendService = f.JsonMythFrontendServiceFactory(host)
  def mythFrontendService(host: String, port: Int): MythFrontendService = f.JsonMythFrontendServiceFactory(host, port)
}
