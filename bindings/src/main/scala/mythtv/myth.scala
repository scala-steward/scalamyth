// SPDX-License-Identifier: LGPL-2.1-only
/*
 * myth.scala: top level interfaces for interacting with MythTV
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv

import java.io.InputStream
import java.time.{ Duration, Instant }
import java.net.InetAddress

import model._
import util.{ ByteCount, MythDateTime, MythFileHash }

trait NetworkHostInfo {
  def hostName: String
  def addresses: Iterable[InetAddress]

  def host: String = {
    if (addresses.nonEmpty) addresses.head.getHostAddress
    else if (hostName.nonEmpty) hostName
    else ""
  }

  override def toString: String = s"host=$hostName addr=$addresses"
}

trait BackendInfo extends NetworkHostInfo {
  def mythProtocolPort: Int
  def servicesPort: Int

  override def toString: String =
    s"<BackendInfo ${super.toString} ports=$mythProtocolPort/$servicesPort>"
}

trait FrontendInfo extends NetworkHostInfo {
  def remoteControlPort: Int
  def servicesPort: Int

  override def toString: String =
    s"<FrontendInfo ${super.toString} ports=$remoteControlPort/$servicesPort>"
}

trait BackendOperations {
  def recordings: Iterable[Recording]
  def expiringRecordings: Iterable[Recording]

  def pendingRecordings: Iterable[Recordable]
  def upcomingRecordings: Iterable[Recordable]
  def conflictingRecordings: Iterable[Recordable]

  def availableRecorders: Iterable[CaptureCardId]

  def freeSpace: List[FreeSpace]
  def freeSpaceCombined: List[FreeSpace]
  def freeSpaceSummary: (ByteCount, ByteCount)

  def uptime: Duration
  def loadAverages: (Double, Double, Double)

  def isActiveBackend(hostname: String): Boolean

  def guideDataThrough: MythDateTime

  def isRecording(cardId: CaptureCardId): Boolean

  // These are FileOps methods in the Python bindings ...
  def recording(chanId: ChanId, startTime: MythDateTime): Recording
  def deleteRecording(rec: Recording, force: Boolean = false): Boolean
  def forgetRecording(rec: Recording): Boolean
  def stopRecording(rec: Recording): Option[CaptureCardId]

  /*
   * Storage group file operations
   */
  def fileHash(fileName: String, storageGroup: String, hostName: String = ""): MythFileHash
  def fileExists(fileName: String, storageGroup: String): Boolean

  def reschedule(): Unit
  def reschedule(wait: Boolean): Unit
  def reschedule(recordId: RecordRuleId, wait: Boolean = false): Unit

  // These are MythXML methods in the Python bindings ...
/*
  def programGuide(startTime: MythDateTime, endTime: MythDateTime, startChanId: ChanId, numChannels: Option[Int]): Guide[Channel, Program]
  def programDetails(chanId: ChanId, startTime: MythDateTime)
  def previewImage(chanId: ChanId, startTime: MythDateTime, width: Option[Int], height: Option[Int], secsIn: Option[Int]): Array[Byte]
 */

  // These are MythDB methods in the Python bindings ...
  def scanVideos(): Map[String, Set[VideoId]]
  //def recorders: Iterable[CaptureCard]
}

trait FrontendOperations {
  def play(media: PlayableMedia): Boolean
  def screenshot[U](format: ScreenshotFormat, width: Int = 0, height: Int = 0)(f: InputStream => U): Unit

  def uptime: Duration
  def loadAverages: List[Double]
  def memoryStats: Map[String, ByteCount]  // memory type -> bytes available
  def currentTime: Instant

  // remote control methods
  def key: PartialFunction[MythFrontend.KeyName, Boolean]
  def jump: PartialFunction[MythFrontend.JumpPoint, Boolean]
}
