// SPDX-License-Identifier: LGPL-2.1-only
/*
 * livestream.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package model

import java.time.Instant

import util.LooseEnum
import EnumTypes.LiveStreamStatus

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
  def status: LiveStreamStatus
  def statusText: String
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


object LiveStreamStatus extends LooseEnum {
  type LiveStreamStatus = Value
  final val Undefined  = Value(-1)
  final val Queued     = Value(0)
  final val Starting   = Value(1)
  final val Running    = Value(2)
  final val Completed  = Value(3)
  final val Errored    = Value(4)
  final val Stopping   = Value(5)
  final val Stopped    = Value(6)
}
