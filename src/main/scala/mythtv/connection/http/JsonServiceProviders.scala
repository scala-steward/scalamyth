package mythtv
package connection
package http

import services._

trait JsonServiceProviders {
  implicit object JsonCaptureServiceProvider extends ServiceProvider[CaptureService] {
    def instance(host: String): CaptureService = {
      val conn = new BackendJsonConnection(host)
      new JsonCaptureService(conn)
    }
  }

  implicit object JsonChannelServiceProvider extends ServiceProvider[ChannelService] {
    def instance(host: String): ChannelService = {
      val conn = new BackendJsonConnection(host)
      new JsonChannelService(conn)
    }
  }

  implicit object JsonContentServiceProvider extends ServiceProvider[ContentService] {
    def instance(host: String): ContentService = {
      val conn = new BackendJsonConnection(host)
      new JsonContentService(conn)
    }
  }

  implicit object JsonDvrServiceProvider extends ServiceProvider[DvrService] {
    def instance(host: String): DvrService = {
      val conn = new BackendJsonConnection(host)
      new JsonDvrService(conn)
    }
  }

  implicit object JsonGuideServiceProvider extends ServiceProvider[GuideService] {
    def instance(host: String): GuideService = {
      val conn = new BackendJsonConnection(host)
      new JsonGuideService(conn)
    }
  }

  implicit object JsonMythServiceProvider extends ServiceProvider[MythService] {
    def instance(host: String): MythService = {
      val conn = new BackendJsonConnection(host)
      new JsonMythService(conn)
    }
  }

  implicit object JsonVideoServiceProvider extends ServiceProvider[VideoService] {
    def instance(host: String): VideoService = {
      val conn = new BackendJsonConnection(host)
      new JsonVideoService(conn)
    }
  }
}

object JsonServiceProviders extends JsonServiceProviders
