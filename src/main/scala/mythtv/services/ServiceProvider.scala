package mythtv
package services

trait ServiceFactory[A <: Service] {
  def apply(host: String): A
  def apply(host: String, port: Int): A
}

trait ServiceProvider {
  /* Backend services */

  def captureService(host: String): CaptureService
  def captureService(host: String, port: Int): CaptureService

  def channelService(host: String): ChannelService
  def channelService(host: String, port: Int): ChannelService

  def contentService(host: String): ContentService
  def contentService(host: String, port: Int): ContentService

  def dvrService(host: String): DvrService
  def dvrService(host: String, port: Int): DvrService

  def guideService(host: String): GuideService
  def guideService(host: String, port: Int): GuideService

  def imageService(host: String): ImageService
  def imageService(host: String, port: Int): ImageService

  def musicService(host: String): MusicService
  def musicService(host: String, port: Int): MusicService

  def mythService(host: String): MythService
  def mythService(host: String, port: Int): MythService

  def videoService(host: String): VideoService
  def videoService(host: String, port: Int): VideoService

  /* Frontend services */

  def mythFrontendService(host: String): MythFrontendService
  def mythFrontendService(host: String, port: Int): MythFrontendService
}

/* Default service provider */
object ServiceProvider extends ServiceProvider {
  private val provider: ServiceProvider = connection.http.json.JsonServiceProvider

  def captureService(host: String): CaptureService                      = provider.captureService(host)
  def captureService(host: String, port: Int): CaptureService           = provider.captureService(host, port)
  def channelService(host: String): ChannelService                      = provider.channelService(host)
  def channelService(host: String, port: Int): ChannelService           = provider.channelService(host, port)
  def contentService(host: String): ContentService                      = provider.contentService(host)
  def contentService(host: String, port: Int): ContentService           = provider.contentService(host, port)
  def dvrService(host: String): DvrService                              = provider.dvrService(host)
  def dvrService(host: String, port: Int): DvrService                   = provider.dvrService(host, port)
  def guideService(host: String): GuideService                          = provider.guideService(host)
  def guideService(host: String, port: Int): GuideService               = provider.guideService(host, port)
  def imageService(host: String): ImageService                          = provider.imageService(host)
  def imageService(host: String, port: Int): ImageService               = provider.imageService(host, port)
  def musicService(host: String): MusicService                          = provider.musicService(host)
  def musicService(host: String, port: Int): MusicService               = provider.musicService(host, port)
  def mythService(host: String): MythService                            = provider.mythService(host)
  def mythService(host: String, port: Int): MythService                 = provider.mythService(host, port)
  def videoService(host: String): VideoService                          = provider.videoService(host)
  def videoService(host: String, port: Int): VideoService               = provider.videoService(host, port)
  def mythFrontendService(host: String): MythFrontendService            = provider.mythFrontendService(host)
  def mythFrontendService(host: String, port: Int): MythFrontendService = provider.mythFrontendService(host, port)
}
