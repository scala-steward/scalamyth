package mythtv
package model

import java.time.Instant

final case class LiveStreamId(id: Int) extends AnyVal

trait LiveStream {
  def id: LiveStreamId
  def width: Int
  def height: Int
  def bitrate: Int
  def audioBitrate: Int
  def segmentSize: Int
  def maxSegments: Int
  def startSegment: Int
  def currentSegment: Int
  def segmentCount: Int
  def percentComplete: Int
  def created: Instant
  def lastModified: Instant
  def relativeUrl: String
  def fullUrl: String
  def statusText: String
  def statusCode: Int
  def statusMessage: String
  def sourceFile: String
  def sourceHost: String
  def sourceWidth: Int
  def sourceHeight: Int
  def audioOnlyBitrate: Int

  override def toString: String = s"<LiveStream $fullUrl>"
}

object LiveStream {
  final val DefaultWidth: Int = 640
  final val DefaultHeight: Int = 480

  final val DefaultBitrate: Int = 800000
  final val DefaultAudioBitrate: Int = 64000
  final val DefaultAudioOnlyBitrate: Int = 32000

  final val DefaultSegmentSize: Int = 10
  final val DefaultMaxSegments: Int = 0

  final val DefaultSampleRate: Int = -1
}
