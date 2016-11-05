package mythtv
package connection
package http
package json

import services._

trait JsonServiceProviders {
  // TODO template these using a factory to create the actual service object

  implicit object JsonCaptureServiceProvider extends ServiceProvider[CaptureService] {
    def instance(host: String): CaptureService = {
      val conn = new BackendJsonConnection(host)
      new JsonCaptureService(conn)
    }
    def instance(host: String, port: Int): CaptureService = {
      val conn = new BackendJsonConnection(host, port)
      new JsonCaptureService(conn)
    }
  }

  implicit object JsonChannelServiceProvider extends ServiceProvider[ChannelService] {
    def instance(host: String): ChannelService = {
      val conn = new BackendJsonConnection(host)
      new JsonChannelService(conn)
    }
    def instance(host: String, port: Int): ChannelService = {
      val conn = new BackendJsonConnection(host, port)
      new JsonChannelService(conn)
    }
  }

  implicit object JsonContentServiceProvider extends ServiceProvider[ContentService] {
    def instance(host: String): ContentService = {
      val conn = new BackendJsonConnection(host)
      new JsonContentService(conn)
    }
    def instance(host: String, port: Int): ContentService = {
      val conn = new BackendJsonConnection(host, port)
      new JsonContentService(conn)
    }
  }

  implicit object JsonDvrServiceProvider extends ServiceProvider[DvrService] {
    def instance(host: String): DvrService = {
      val conn = new BackendJsonConnection(host)
      new JsonDvrService(conn)
    }
    def instance(host: String, port: Int): DvrService = {
      val conn = new BackendJsonConnection(host, port)
      new JsonDvrService(conn)
    }
  }

  implicit object JsonGuideServiceProvider extends ServiceProvider[GuideService] {
    def instance(host: String): GuideService = {
      val conn = new BackendJsonConnection(host)
      new JsonGuideService(conn)
    }
    def instance(host: String, port: Int): GuideService = {
      val conn = new BackendJsonConnection(host, port)
      new JsonGuideService(conn)
    }
  }

  implicit object JsonMythServiceProvider extends ServiceProvider[MythService] {
    def instance(host: String): MythService = {
      val conn = new BackendJsonConnection(host)
      new JsonMythService(conn)
    }
    def instance(host: String, port: Int): MythService = {
      val conn = new BackendJsonConnection(host, port)
      new JsonMythService(conn)
    }
  }

  implicit object JsonVideoServiceProvider extends ServiceProvider[VideoService] {
    def instance(host: String): VideoService = {
      val conn = new BackendJsonConnection(host)
      new JsonVideoService(conn)
    }
    def instance(host: String, port: Int): VideoService = {
      val conn = new BackendJsonConnection(host, port)
      new JsonVideoService(conn)
    }
  }
}

object JsonServiceProviders extends JsonServiceProviders
