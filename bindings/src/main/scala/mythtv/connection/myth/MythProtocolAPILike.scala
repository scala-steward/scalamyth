// SPDX-License-Identifier: LGPL-2.1-only
/*
 * MythProtocolAPILike.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth

import java.net.{ InetAddress, URI }
import java.time.{ Duration, Instant }

import scala.annotation.nowarn
import scala.util.{ Try, Success, Failure }

import EnumTypes.{ MythProtocolEventMode, SeekWhence }
import model._
import model.EnumTypes._
import util.{ ByteCount, ExpectedCountIterator, FileStats, MythDateTime, MythFileHash, NetworkUtil }
import MythProtocol.ImageScanResult._
import MythProtocol.QueryRecorderResult._
import MythProtocol.QueryRemoteEncoderResult._
import MythProtocol.QueryFileTransferResult._
import MythProtocol.MythProtocolFailure._

@nowarn("cat=other-match-analysis")
private[myth] trait MythProtocolAPILike {
  self: MythProtocol =>

  def allowShutdown(): MythProtocolResult[Boolean] = {
    val result = sendCommand("ALLOW_SHUTDOWN")
    result map { case r: Boolean => r }
  }

  def announce(mode: String, hostName: String, eventMode: MythProtocolEventMode): MythProtocolResult[Boolean] = {
    import MythProtocol.AnnounceResult._
    val localHost =
      if (hostName.nonEmpty) hostName
      else NetworkUtil.myHostName
    val result = sendCommand("ANN", mode, localHost, eventMode)
    result map { case AnnounceAcknowledgement => true }
  }

  def announceFileTransfer(hostName: String, fileName: String, storageGroup: String,
    writeMode: Boolean, useReadAhead: Boolean, timeout: Duration, checkFiles: Seq[String]
  ): MythProtocolResult[(FileTransferId, ByteCount, Seq[String])] = {
    import MythProtocol.AnnounceResult._
    val args: Seq[Any] =
      if (writeMode) List("FileTransfer", hostName, writeMode, fileName, storageGroup)
      else           List("FileTransfer", hostName, writeMode, useReadAhead, timeout, fileName, storageGroup) ++ checkFiles
    val result = sendCommand("ANN", args: _*)
    result map { case AnnounceFileTransfer(ftID, fileSize, check) => (ftID, fileSize, check) }
  }

  def announceSlaveBackend(slaveHostName: String, slaveIpAddr: InetAddress, currentlyRecording: Seq[Recording]): MythProtocolResult[Boolean] = {
    import MythProtocol.AnnounceResult._
    val result = sendCommand("ANN", "SlaveBackend", slaveHostName, slaveIpAddr, currentlyRecording)
    result map { case AnnounceAcknowledgement => true }
  }

  def backendMessage(message: String, extra: String*): MythProtocolResult[Boolean] = {
    val args = List(message) ++ extra
    val result = sendCommand("BACKEND_MESSAGE", args: _*)
    result map { case r: Boolean => r }
  }

  def blockShutdown(): MythProtocolResult[Boolean] = {
    val result = sendCommand("BLOCK_SHUTDOWN")
    result map { case r: Boolean => r }
  }

  def checkRecording(rec: Recording): MythProtocolResult[Boolean] = {
    val result = sendCommand("CHECK_RECORDING", rec)
    result map { case r: Boolean => r }
  }

  def deleteFile(fileName: String, storageGroup: String): MythProtocolResult[Boolean] = {
    val result = sendCommand("DELETE_FILE", fileName, storageGroup)
    result map { case r: Boolean => r }
  }

  def deleteRecording(rec: Recording): MythProtocolResult[Int] = {
    val result = sendCommand("DELETE_RECORDING", rec)
    result map { case r: Int => r }
  }

  def deleteRecording(chanId: ChanId, startTime: MythDateTime): MythProtocolResult[Int] = {
    val result = sendCommand("DELETE_RECORDING", chanId, startTime)
    result map { case r: Int => r }
  }

  def deleteRecording(chanId: ChanId, startTime: MythDateTime, forceDeleteMetadata: Boolean, forgetHistory: Boolean): MythProtocolResult[Int] = {
    val force = if (forceDeleteMetadata) "FORCE" else "-"
    val forget = if (forgetHistory) "FORGET" else ""
    val result = sendCommand("DELETE_RECORDING", chanId, startTime, force, forget)
    result map { case r: Int => r }
  }

  def downloadFile(sourceUrl: URI, storageGroup: String, fileName: String): MythProtocolResult[URI] = {
    val result = sendCommand("DOWNLOAD_FILE", sourceUrl, storageGroup, fileName)
    result map { case s: URI => s }
  }

  def downloadFileNow(sourceUrl: URI, storageGroup: String, fileName: String): MythProtocolResult[URI] = {
    val result = sendCommand("DOWNLOAD_FILE", sourceUrl, storageGroup, fileName)
    result map { case s: URI => s }
  }

  def fillProgramInfo(playbackHost: String, p: Recording): MythProtocolResult[Recording] = {
    val result = sendCommand("FILL_PROGRAM_INFO", playbackHost, p)
    result map { case r: Recording => r }
  }

  def forceDeleteRecording(rec: Recording): MythProtocolResult[Int] = {
    val result = sendCommand("FORCE_DELETE_RECORDING", rec)
    result map { case r: Int => r }
  }

  def forgetRecording(rec: Recording): MythProtocolResult[Boolean] = {
    val result = sendCommand("FORGET_RECORDING", rec)
    result map { case r: Boolean => r }
  }

  def freeTuner(cardId: CaptureCardId): MythProtocolResult[Boolean] = {
    val result = sendCommand("FREE_TUNER", cardId)
    result map { case r: Boolean => r }
  }

  private def cardInput2Input(cardInput: CardInput): Input = new Input {
    def cardId        = cardInput.cardId
    def inputId       = cardInput.cardInputId
    def sourceId      = cardInput.sourceId
    def chanId        = None
    def mplexId       = Some(cardInput.mplexId)
    def name          = cardInput.name
    def displayName   = name
    def recPriority   = 0
    def scheduleOrder = 0
    def liveTvOrder   = cardInput.liveTvOrder
    def quickTune     = false
  }

  import scala.language.implicitConversions
  private implicit def try2Result[T](t: Try[T]): MythProtocolResult[T] = t match {
    case Success(value) => Right(value)
    case Failure(ex) => Left(MythProtocolFailureThrowable(ex))  // TODO catch more specific exception types
  }

  private def internalFreeInputsForCards(cards: List[CaptureCardId]): MythProtocolResult[List[CardInput]] =
    Try(cards flatMap (queryRecorderGetFreeInputs(_).get))

  private def internalFreeInputsForCards(excluded: InputId)(cards: List[CaptureCardId]): MythProtocolResult[List[CardInput]] =
    Try(cards flatMap (queryRecorderGetFreeInputs(_, CaptureCardId(excluded.id)).get))

  def getFreeInputInfo: MythProtocolResult[List[Input]] = {
    if (ProtocolVersion >= 88) {
      val result = sendCommand("GET_FREE_INPUT_INFO", InputId(0))
      result map { case xs: List[_] => xs.asInstanceOf[List[Input]] }
    } else {
      val cardInputs = getFreeRecorderList flatMap internalFreeInputsForCards
      cardInputs map (_ map cardInput2Input)
    }
  }

  def getFreeInputInfo(excludedInput: InputId): MythProtocolResult[List[Input]] = {
    if (ProtocolVersion >= 88) {
      val result = sendCommand("GET_FREE_INPUT_INFO", excludedInput)
      result map { case xs: List[_] => xs.asInstanceOf[List[Input]] }
    } else {
      val cardInputs = getFreeRecorderList flatMap internalFreeInputsForCards(excludedInput)
      cardInputs map (_ map cardInput2Input)
    }
  }

  private def internalFirstInput(inputs: List[Input]): MythProtocolResult[Input] = inputs match {
    case Nil => Left(MythProtocolNoResult)
    case x :: _ => Right(x)
  }

  def getFreeRecorder: MythProtocolResult[RemoteEncoder] = {
    if (ProtocolVersion < 88) {
      val result = sendCommand("GET_FREE_RECORDER")
      result map { case e: RemoteEncoder => e }
    }
    else for {
      inputs  <- getFreeInputInfo
      first   <- internalFirstInput(inputs)
      encoder <- getRecorderFromNum(CaptureCardId(first.inputId.id))
    } yield encoder
  }

  def getFreeRecorderCount: MythProtocolResult[Int] = {
    if (ProtocolVersion < 88) {
      val result = sendCommand("GET_FREE_RECORDER_COUNT")
      result map { case n: Int => n }
    } else {
      getFreeInputInfo map (_.size)
    }
  }

  def getFreeRecorderList: MythProtocolResult[List[CaptureCardId]] = {
    if (ProtocolVersion < 88) {
      val result = sendCommand("GET_FREE_RECORDER_LIST")
      result map { case xs: List[_] => xs.asInstanceOf[List[CaptureCardId]] }
    } else {
      getFreeInputInfo map (_ map (i => CaptureCardId(i.inputId.id)))
    }
  }

  def getNextFreeRecorder(cardId: CaptureCardId): MythProtocolResult[RemoteEncoder] = {
    if (ProtocolVersion < 88) {
      val result = sendCommand("GET_NEXT_FREE_RECORDER", cardId)
      result map { case e: RemoteEncoder => e }
    }
    else for {
      inputs  <- getFreeInputInfo(InputId(cardId.id))  // FIXME not sure this is right?
      first   <- internalFirstInput(inputs)
      encoder <- getRecorderFromNum(CaptureCardId(first.inputId.id))
    } yield encoder
  }

  def getRecorderFromNum(cardId: CaptureCardId): MythProtocolResult[RemoteEncoder] = {
    val result = sendCommand("GET_RECORDER_FROM_NUM", cardId)
    result map { case e: RemoteEncoder => e }
  }

  def getRecorderNum(rec: Recording): MythProtocolResult[RemoteEncoder] = {
    val result = sendCommand("GET_RECORDER_NUM", rec)
    result map { case e: RemoteEncoder => e }
  }

  def goToSleep(): MythProtocolResult[Boolean] = {
    val result = sendCommand("GO_TO_SLEEP")
    result map { case r: Boolean => r }
  }

  def imageCover(directoryId: ImageDirId, coverId: ImageFileId): MythProtocolResult[Unit] = {
    val result = sendCommand("IMAGE_COVER", directoryId, coverId)
    result map { case () => () }
  }

  def imageCoverReset(directoryId: ImageDirId): MythProtocolResult[Unit] = {
    val result = sendCommand("IMAGE_COVER", directoryId, ImageFileId(0))
    result map { case () => () }
  }

  def imageCreateDirs(directoryId: ImageDirId, rescan: Boolean, newRelativePaths: Seq[String]): MythProtocolResult[Unit] = {
    val result = sendCommand("IMAGE_CREATE_DIRS", directoryId, rescan, newRelativePaths)
    result map { case () => () }
  }

  def imageDelete(imageId: ImageId): MythProtocolResult[Unit] = {
    val result = sendCommand("IMAGE_DELETE", imageId)
    result map { case () => () }
  }

  def imageDelete(imageIds: Seq[ImageId]): MythProtocolResult[Unit] = {
    val result = sendCommand("IMAGE_DELETE", imageIds)
    result map { case () => () }
  }

  def imageHide(hide: Boolean, imageId: ImageId): MythProtocolResult[Unit] = {
    val result = sendCommand("IMAGE_HIDE", hide, imageId)
    result map { case () => () }
  }

  def imageHide(hide: Boolean, imageIds: Seq[ImageId]): MythProtocolResult[Unit] = {
    val result = sendCommand("IMAGE_HIDE", hide, imageIds)
    result map { case () => () }
  }

  def imageIgnore(ignorePattern: String*): MythProtocolResult[Unit] = {
    val result = sendCommand("IMAGE_IGNORE", ignorePattern)
    result map { case () => () }
  }

  def imageMove(imageId: ImageId, oldPath: String, newPath: String): MythProtocolResult[Unit] = {
    val result = sendCommand("IMAGE_MOVE", imageId, oldPath, newPath)
    result map { case () => () }
  }

  def imageMove(imageIds: Seq[ImageId], oldPath: String, newPath: String): MythProtocolResult[Unit] = {
    val result = sendCommand("IMAGE_MOVE", imageIds, oldPath, newPath)
    result map { case () => () }
  }

  def imageRename(imageId: ImageId, newBasename: String): MythProtocolResult[Unit] = {
    val result = sendCommand("IMAGE_RENAME", imageId, newBasename)
    result map { case () => () }
  }

  def imageScanQuery(): MythProtocolResult[(Boolean, Int, Int)] = {
    val result = sendCommand("IMAGE_SCAN", "QUERY")
    result map { case ImageScanProgress(isBackend, progress, total) => (isBackend, progress, total) }
  }

  def imageScanStart(): MythProtocolResult[Unit] = {
    val result = sendCommand("IMAGE_SCAN", "START")
    result map { case ImageScanAcknowledgement => () }
  }

  def imageScanStop(): MythProtocolResult[Unit] = {
    val result = sendCommand("IMAGE_SCAN", "STOP")
    result map { case ImageScanAcknowledgement => () }
  }

  def imageTransform(transform: ImageFileTransform, imageId: ImageFileId): MythProtocolResult[Unit] = {
    val result = sendCommand("IMAGE_TRANSFORM", transform, imageId)
    result map { case () => () }
  }

  def imageTransform(transform: ImageFileTransform, imageIds: Seq[ImageFileId]): MythProtocolResult[Unit] = {
    val result = sendCommand("IMAGE_TRANSFORM", transform, imageIds)
    result map { case () => () }
  }

  def lockTuner(): MythProtocolResult[Tuner] = {
    val result = sendCommand("LOCK_TUNER")
    result map { case t: Tuner => t }
  }

  def lockTuner(cardId: CaptureCardId): MythProtocolResult[Tuner] = {
    val result = sendCommand("LOCK_TUNER", cardId)
    result map { case t: Tuner => t }
  }

  def message(message: String, extra: String*): MythProtocolResult[Boolean] = {
    val args = List(message) ++ extra
    val result = sendCommand("MESSAGE", args: _*)
    result map { case r: Boolean => r }
  }

  def messageSetLogLevel(logLevel: MythLogLevel): MythProtocolResult[Boolean] = {
    val result = sendCommand("MESSAGE", "SET_LOG_LEVEL", logLevel)
    result map { case r: Boolean => r }
  }

  def messageSetVerbose(verboseMask: String): MythProtocolResult[Boolean] = {
    val result = sendCommand("MESSAGE", "SET_VERBOSE", verboseMask)
    result map { case r: Boolean => r }
  }

  def moveFile(storageGroup: String, source: String, dest: String): MythProtocolResult[Boolean] = {
    val result = sendCommand("MOVE_FILE", storageGroup, source, dest)
    result map { case r: Boolean => r }
  }

  def musicCalcTrackLength(hostName: String, songId: SongId): MythProtocolResult[Unit] = {
    val result = sendCommand("MUSIC_CALC_TRACK_LENGTH", hostName, songId)
    result map { case () => () }
  }

  def musicFindAlbumArt(hostName: String, songId: SongId, updateDatabase: Boolean): MythProtocolResult[List[AlbumArtImage]] = {
    val result = sendCommand("MUSIC_FIND_ALBUMART", hostName, songId, updateDatabase)
    result map { case xs: List[_] => xs.asInstanceOf[List[AlbumArtImage]] }
  }

  def musicLyricsFind(hostName: String, songId: SongId, grabberName: String): MythProtocolResult[Unit] = {
    val result = sendCommand("MUSIC_LYRICS_FIND", hostName, songId, grabberName)
    result map { case () => () }
  }

  def musicLyricsGetGrabbers: MythProtocolResult[List[String]] = {
    val result = sendCommand("MUSIC_LYRICS_GETGRABBERS")
    result map { case xs: List[_] => xs.asInstanceOf[List[String]] }
  }

  def musicLyricsSave(hostName: String, songId: SongId, lyricsLines: Seq[String]): MythProtocolResult[Unit] = {
    val result = sendCommand("MUSIC_LYRICS_SAVE", hostName, songId, lyricsLines)
    result map { case () => () }
  }

  def musicTagAddImage(hostName: String, songId: SongId, fileName: String, imageType: MusicImageType): MythProtocolResult[Unit] = {
    val result = sendCommand("MUSIC_TAG_ADDIMAGE", hostName, songId, fileName, imageType)
    result map { case () => () }
  }

  def musicTagChangeImage(hostName: String, songId: SongId, oldType: MusicImageType, newType: MusicImageType): MythProtocolResult[Unit] = {
    val result = sendCommand("MUSIC_TAG_CHANGEIMAGE", hostName, songId, oldType, newType)
    result map { case () => () }
  }

  def musicTagGetImage(hostName: String, songId: SongId, imageType: MusicImageType): MythProtocolResult[Unit] = {
    val result = sendCommand("MUSIC_TAG_GETIMAGE", hostName, songId, imageType)
    result map { case () => () }
  }

  def musicTagRemoveImage(hostName: String, songId: SongId, imageId: MusicImageId): MythProtocolResult[Unit] = {
    val result = sendCommand("MUSIC_TAG_REMOVEIMAGE", hostName, songId, imageId)
    result map { case () => () }
  }

  def musicTagUpdateMetadata(hostName: String, songId: SongId): MythProtocolResult[Unit] = {
    val result = sendCommand("MUSIC_TAG_UPDATE_METADATA", hostName, songId)
    result map { case () => () }
  }

  def musicTagUpdateVolatile(hostName: String, songId: SongId, rating: Int, playCount: Int, lastPlayed: Instant): MythProtocolResult[Unit] = {
    val result = sendCommand("MUSIC_TAG_UPDATE_VOLATILE", hostName, songId, rating, playCount, lastPlayed)
    result map { case () => () }
  }

  def protocolVersion(version: Int, token: String): MythProtocolResult[(Boolean, Int)] = {
    // NOTE that I believe an incorrect protocol version results in socket being closed
    val result = sendCommand("MYTH_PROTO_VERSION", version, token)
    result map { case (accepted: Boolean, acceptVer: Int) => (accepted, acceptVer) }
  }

  def queryActiveBackends: MythProtocolResult[List[String]] = {
    val result = sendCommand("QUERY_ACTIVE_BACKENDS")
    result map { case xs: List[_] => xs.asInstanceOf[List[String]] }
  }

  def queryBookmark(chanId: ChanId, startTime: MythDateTime): MythProtocolResult[VideoPositionFrame] = {
    val result = sendCommand("QUERY_BOOKMARK", chanId, startTime)
    result map { case p: VideoPositionFrame => p }
  }

  def queryCheckFile(rec: Recording, checkSlaves: Boolean): MythProtocolResult[String] = {
    val result = sendCommand("QUERY_CHECKFILE", checkSlaves, rec)
    result map { case s: String => s }
  }

  def queryCommBreak(chanId: ChanId, startTime: MythDateTime): MythProtocolResult[List[VideoSegmentFrames]] = {
    val result = sendCommand("QUERY_COMMBREAK", chanId, startTime)
    result map { case xs: List[_] => xs.asInstanceOf[List[VideoSegmentFrames]] }
  }

  def queryCutList(chanId: ChanId, startTime: MythDateTime): MythProtocolResult[List[VideoSegmentFrames]] = {
    val result = sendCommand("QUERY_CUTLIST", chanId, startTime)
    result map { case xs: List[_] => xs.asInstanceOf[List[VideoSegmentFrames]] }
  }

  def queryFileExists(fileName: String, storageGroup: String): MythProtocolResult[(String, FileStats)] = {
    val result =
      if (storageGroup.isEmpty) sendCommand("QUERY_FILE_EXISTS", fileName)
      else sendCommand("QUERY_FILE_EXISTS", fileName, storageGroup)
    result map { case (fullName: String, stats: FileStats) => (fullName, stats) }
  }

  def queryFileTransferDone(ftId: FileTransferId): Unit = {
    val result = sendCommand("QUERY_FILETRANSFER", ftId, "DONE")
    result map { case QueryFileTransferAcknowledgement => true }
  }

  def queryFileTransferIsOpen(ftId: FileTransferId): MythProtocolResult[Boolean] = {
    val result = sendCommand("QUERY_FILETRANSFER", ftId, "IS_OPEN")
    result map { case QueryFileTransferBoolean(bool) => bool }
  }

  def queryFileTransferReopen(ftId: FileTransferId, newFileName: String): MythProtocolResult[Boolean] = {
    val result = sendCommand("QUERY_FILETRANSFER", ftId, "REOPEN")
    result map { case QueryFileTransferBoolean(bool) => bool }
  }

  def queryFileTransferRequestBlock(ftId: FileTransferId, blockSize: Int): MythProtocolResult[Int] = {
    val result = sendCommand("QUERY_FILETRANSFER", ftId, "REQUEST_BLOCK", blockSize)
    result map { case QueryFileTransferBytesTransferred(count) => count }
  }

  def queryFileTransferRequestSize(ftId: FileTransferId): MythProtocolResult[Long] = {
    val result = sendCommand("QUERY_FILETRANSFER", ftId, "REQUEST_SIZE")
    result map { case QueryFileTransferRequestSize(size, _) => size }
  }

  def queryFileTransferSeek(ftId: FileTransferId, pos: Long, whence: SeekWhence, currentPos: Long): MythProtocolResult[Long] = {
    val result = sendCommand("QUERY_FILETRANSFER", ftId, "SEEK", pos, whence, currentPos)
    result map { case QueryFileTransferPosition(newPos) => newPos}
  }

  def queryFileTransferSetTimeout(ftId: FileTransferId, fast: Boolean): Unit = {
    val result = sendCommand("QUERY_FILETRANSFER", ftId, "SET_TIMEOUT", fast)
    result map { case QueryFileTransferAcknowledgement => true }
  }

  def queryFileTransferWriteBlock(ftId: FileTransferId, blockSize: Int): MythProtocolResult[Int] = {
    val result = sendCommand("QUERY_FILETRANSFER", ftId, "WRITE_BLOCK", blockSize)
    result map { case QueryFileTransferBytesTransferred(count) => count }
  }

  def queryFileHash(fileName: String, storageGroup: String, hostName: String): MythProtocolResult[MythFileHash] = {
    val result =
      if (hostName == "") sendCommand("QUERY_FILE_HASH", fileName, storageGroup)
      else sendCommand("QUERY_FILE_HASH", fileName, storageGroup, hostName)
    result map { case h: MythFileHash => h }
  }

  def queryFindFile(fileName: String, storageGroup: String, hostName: String, useRegex: Boolean, allowFallback: Boolean): MythProtocolResult[List[URI]] = {
    val result = sendCommand("QUERY_FINDFILE", hostName, storageGroup, fileName, useRegex, allowFallback)
    result map { case xs: List[_] => xs.asInstanceOf[List[URI]] }
  }

  def queryFreeSpace: MythProtocolResult[List[FreeSpace]] = {
    val result = sendCommand("QUERY_FREE_SPACE")
    result map { case xs: List[_] => xs.asInstanceOf[List[FreeSpace]] }
  }

  def queryFreeSpaceList: MythProtocolResult[List[FreeSpace]] = {
    val result = sendCommand("QUERY_FREE_SPACE_LIST")
    result map { case xs: List[_] => xs.asInstanceOf[List[FreeSpace]] }
  }

  def queryFreeSpaceSummary: MythProtocolResult[(ByteCount, ByteCount)] = {
    val result = sendCommand("QUERY_FREE_SPACE_SUMMARY")
    result map { case (total: ByteCount, used: ByteCount) => (total, used) }
  }

  def queryGenPixmap(rec: Recording, token: String, time: VideoPosition, outputFile: String, width: Int, height: Int): MythProtocolResult[Boolean] = {
    val sentToken = if (token.isEmpty) "do_not_care" else token
    val fileName = if (outputFile.isEmpty) "<EMPTY>" else outputFile
    val hasExtra = time.pos != -1 || outputFile.nonEmpty || width != 0 || height != 0

    // Convert milliseconds to seconds here as a convenience.
    // This Myth protocol API only accepts time values in seconds and frames.
    val timeIn = time match {
      case VideoPositionMilliseconds(ms) => VideoPositionSeconds(ms / 1000)
      case pos => pos
    }

    val result =
      if (hasExtra) sendCommand("QUERY_GENPIXMAP2", sentToken, rec, timeIn.units, timeIn, fileName, width, height)
      else          sendCommand("QUERY_GENPIXMAP2", sentToken, rec)

    result map { case r: Boolean => r }
  }

  def queryGetAllPending: MythProtocolResult[ExpectedCountIterator[Recordable]] = {
    val result = sendCommand("QUERY_GETALLPENDING")
    result map { case it: ExpectedCountIterator[_] => it.asInstanceOf[ExpectedCountIterator[Recordable]] }
  }

  def queryGetAllScheduled: MythProtocolResult[ExpectedCountIterator[Recordable]] = {
    val result = sendCommand("QUERY_GETALLSCHEDULED")
    result map { case it: ExpectedCountIterator[_] => it.asInstanceOf[ExpectedCountIterator[Recordable]] }
  }

  def queryGetConflicting(rec: Recording): MythProtocolResult[ExpectedCountIterator[Recording]] = {
    val result = sendCommand("QUERY_GETCONFLICTING", rec)
    result map { case it: ExpectedCountIterator[_] => it.asInstanceOf[ExpectedCountIterator[Recording]] }
  }

  def queryGetExpiring: MythProtocolResult[ExpectedCountIterator[Recording]] = {
    val result = sendCommand("QUERY_GETEXPIRING")
    result map { case it: ExpectedCountIterator[_] => it.asInstanceOf[ExpectedCountIterator[Recording]] }
  }

  def queryGuideDataThrough: MythProtocolResult[MythDateTime] = {
    val result = sendCommand("QUERY_GUIDEDATATHROUGH")
    result map { case d: MythDateTime => d }
  }

  def queryHostname: MythProtocolResult[String] = {
    val result = sendCommand("QUERY_HOSTNAME")
    result map { case h: String => h }
  }

  def queryIsActiveBackend(hostName: String): MythProtocolResult[Boolean] = {
    val result = sendCommand("QUERY_IS_ACTIVE_BACKEND", hostName)
    result map { case a: Boolean => a }
  }

  def queryIsRecording: MythProtocolResult[(Int, Int)] = {
    val result = sendCommand("QUERY_ISRECORDING")
    result map { case (rec: Int, live: Int) => (rec, live) }
  }

  def queryLoad: MythProtocolResult[(Double, Double, Double)] = {
    val result = sendCommand("QUERY_LOAD")
    result map { case (one: Double, five: Double, fifteen: Double) => (one, five, fifteen) }
  }

  def queryMemStats: MythProtocolResult[(ByteCount, ByteCount, ByteCount, ByteCount)] = {
    val result = sendCommand("QUERY_MEMSTATS")
    result map {
      case (total: ByteCount, free: ByteCount, totalVM: ByteCount, freeVM: ByteCount) =>
        (total, free, totalVM, freeVM)
    }
  }

  def queryPixmapGetIfModified(maxFileSize: Long, rec: Recording): MythProtocolResult[(MythDateTime, Option[PixmapInfo])] = {
    val result = sendCommand("QUERY_PIXMAP_GET_IF_MODIFIED", maxFileSize, rec)
    result map {
      case (lastModified: MythDateTime, pixmapInfo: Option[_]) =>
        (lastModified, pixmapInfo map { case p: PixmapInfo => p })
    }
  }

  def queryPixmapGetIfModified(modifiedSince: MythDateTime, maxFileSize: Long, rec: Recording): MythProtocolResult[(MythDateTime, Option[PixmapInfo])] = {
    val result = sendCommand("QUERY_PIXMAP_GET_IF_MODIFIED", modifiedSince, maxFileSize, rec)
    result map {
      case (lastModified: MythDateTime, pixmapInfo: Option[_]) =>
        (lastModified, pixmapInfo map { case p: PixmapInfo => p })
    }
  }

  def queryPixmapLastModified(rec: Recording): MythProtocolResult[MythDateTime] = {
    val result = sendCommand("QUERY_PIXMAP_LASTMODIFIED", rec)
    result map { case d: MythDateTime => d }
  }

  def queryRecorderCancelNextRecording(cardId: CaptureCardId, cancel: Boolean): MythProtocolResult[Unit] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CANCEL_NEXT_RECORDING", cancel)
    result map { case QueryRecorderAcknowledgement => () }
  }

  def queryRecorderChangeBrightness(cardId: CaptureCardId, adjType: PictureAdjustType, up: Boolean): MythProtocolResult[Int] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CHANGE_BRIGHTNESS", adjType, up)
    result map { case QueryRecorderPictureAttribute(a) => a }
  }

  def queryRecorderChangeChannel(cardId: CaptureCardId, dir: ChannelChangeDirection): MythProtocolResult[Unit] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CHANGE_CHANNEL", dir)
    result map { case QueryRecorderAcknowledgement => () }
  }

  def queryRecorderChangeColour(cardId: CaptureCardId, adjType: PictureAdjustType, up: Boolean): MythProtocolResult[Int] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CHANGE_COLOUR", adjType, up)
    result map { case QueryRecorderPictureAttribute(a) => a }
  }

  def queryRecorderChangeContrast(cardId: CaptureCardId, adjType: PictureAdjustType, up: Boolean): MythProtocolResult[Int] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CHANGE_CONTRAST", adjType, up)
    result map { case QueryRecorderPictureAttribute(a) => a }
  }

  def queryRecorderChangeHue(cardId: CaptureCardId, adjType: PictureAdjustType, up: Boolean): MythProtocolResult[Int] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CHANGE_HUE", adjType, up)
    result map { case QueryRecorderPictureAttribute(a) => a }
  }

  def queryRecorderCheckChannel(cardId: CaptureCardId, channum: ChannelNumber): MythProtocolResult[Boolean] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CHECK_CHANNEL", channum)
    result map { case QueryRecorderBoolean(b) => b }
  }

  def queryRecorderCheckChannelPrefix(cardId: CaptureCardId, channumPrefix: ChannelNumber):
      MythProtocolResult[(Boolean, Option[CaptureCardId], Boolean, String)] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "CHECK_CHANNEL_PREFIX", channumPrefix)
    result map { case QueryRecorderCheckChannelPrefix(matched, card, extraCharUseful, spacer) =>
      (matched, card, extraCharUseful, spacer) }
  }

  // TODO return type more specific?
  def queryRecorderFillDurationMap(cardId: CaptureCardId, start: VideoPositionFrame, end: VideoPositionFrame): MythProtocolResult[Map[VideoPositionFrame, Long]] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "FILL_DURATION_MAP", start, end)
    result map { case QueryRecorderPositionMap(m) => m }
  }

  // TODO return type more specific?
  def queryRecorderFillPositionMap(cardId: CaptureCardId, start: VideoPositionFrame, end: VideoPositionFrame): MythProtocolResult[Map[VideoPositionFrame, Long]] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "FILL_POSITION_MAP", start, end)
    result map { case QueryRecorderPositionMap(m) => m }
  }

  def queryRecorderFinishRecording(cardId: CaptureCardId): MythProtocolResult[Unit] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "FINISH_RECORDING")
    result map { case QueryRecorderAcknowledgement => () }
  }

  def queryRecorderFrontendReady(cardId: CaptureCardId): MythProtocolResult[Unit] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "FRONTEND_READY")
    result map { case QueryRecorderAcknowledgement => () }
  }

  def queryRecorderGetBrightness(cardId: CaptureCardId): MythProtocolResult[Int] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_BRIGHTNESS")
    result map { case QueryRecorderPictureAttribute(a) => a }
  }

  def queryRecorderGetChannelInfo(cardId: CaptureCardId, chanId: ChanId): MythProtocolResult[Channel] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_CHANNEL_INFO", chanId)
    result map { case QueryRecorderChannelInfo(c) => c }
  }

  def queryRecorderGetColour(cardId: CaptureCardId): MythProtocolResult[Int] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_COLOUR")
    result map { case QueryRecorderPictureAttribute(a) => a }
  }

  def queryRecorderGetContrast(cardId: CaptureCardId): MythProtocolResult[Int] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_CONTRAST")
    result map { case QueryRecorderPictureAttribute(a) => a }
  }

  def queryRecorderGetCurrentRecording(cardId: CaptureCardId): MythProtocolResult[Recording] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_CURRENT_RECORDING")
    result map { case QueryRecorderRecording(r) => r }
  }

  def queryRecorderGetFilePosition(cardId: CaptureCardId): MythProtocolResult[Long] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_FILE_POSITION")
    result map { case QueryRecorderPosition(p) => p }
  }

  def queryRecorderGetFrameRate(cardId: CaptureCardId): MythProtocolResult[Double] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_FRAMERATE")
    result map { case QueryRecorderFrameRate(r) => r }
  }

  def queryRecorderGetFramesWritten(cardId: CaptureCardId): MythProtocolResult[Long] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_FRAMES_WRITTEN")
    result map { case QueryRecorderFrameCount(n) => n }
  }

  def queryRecorderGetFreeInputs(cardId: CaptureCardId, excludedCards: CaptureCardId*): MythProtocolResult[List[CardInput]] = {
    val args = List(cardId, "GET_FREE_INPUTS") ++ excludedCards
    val result = sendCommand("QUERY_RECORDER", args: _*)
    result map { case QueryRecorderCardInputList(i) => i }
  }

  def queryRecorderGetHue(cardId: CaptureCardId): MythProtocolResult[Int] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_HUE")
    result map { case QueryRecorderPictureAttribute(a) => a }
  }

  def queryRecorderGetInput(cardId: CaptureCardId): MythProtocolResult[String] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_INPUT")
    result map { case QueryRecorderInput(input) => input }
  }

  def queryRecorderGetKeyframePos(cardId: CaptureCardId, desiredPos: VideoPositionFrame): MythProtocolResult[Long] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_KEYFRAME_POS", desiredPos)
    result map { case QueryRecorderPosition(p) => p }
  }

  def queryRecorderGetMaxBitrate(cardId: CaptureCardId): MythProtocolResult[Long] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_MAX_BITRATE")
    result map { case QueryRecorderBitrate(b) => b }
  }

  def queryRecorderGetNextProgramInfo(cardId: CaptureCardId, chanId: ChanId, dir: ChannelBrowseDirection,
    startTime: MythDateTime): MythProtocolResult[UpcomingProgram] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_NEXT_PROGRAM_INFO", ChannelNumber(""), chanId, dir, startTime)
    result map { case QueryRecorderNextProgramInfo(p) => p }
  }

  def queryRecorderGetNextProgramInfo(cardId: CaptureCardId, channum: ChannelNumber, dir: ChannelBrowseDirection,
    startTime: MythDateTime): MythProtocolResult[UpcomingProgram] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_NEXT_PROGRAM_INFO", channum, ChanId.empty, dir, startTime)
    result map { case QueryRecorderNextProgramInfo(p) => p }
  }

  def queryRecorderGetRecording(cardId: CaptureCardId): MythProtocolResult[Recording] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "GET_RECORDING")
    result map { case QueryRecorderRecording(r) => r }
  }

  def queryRecorderIsRecording(cardId: CaptureCardId): MythProtocolResult[Boolean] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "IS_RECORDING")
    result map { case QueryRecorderBoolean(b) => b }
  }

  def queryRecorderPause(cardId: CaptureCardId): MythProtocolResult[Unit] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "PAUSE")
    result map { case QueryRecorderAcknowledgement => () }
  }

  def queryRecorderSetChannel(cardId: CaptureCardId, channum: ChannelNumber): MythProtocolResult[Unit] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "SET_CHANNEL", channum)
    result map { case QueryRecorderAcknowledgement => () }
  }

  def queryRecorderSetInput(cardId: CaptureCardId, inputName: String): MythProtocolResult[String] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "SET_INPUT", inputName)
    result map { case QueryRecorderInput(input) => input }
  }

  def queryRecorderSetLiveRecording(cardId: CaptureCardId, recordingState: Int): MythProtocolResult[Unit] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "SET_LIVE_RECORDING", recordingState)
    result map { case QueryRecorderAcknowledgement => () }
  }

  def queryRecorderSetSignalMonitoringRate(cardId: CaptureCardId, rate: Int, notifyFrontend: Boolean): MythProtocolResult[Boolean] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "SET_SIGNAL_MONITORING_RATE", rate, notifyFrontend)
    result map { case QueryRecorderBoolean(b) => b }
  }

  def queryRecorderShouldSwitchCard(cardId: CaptureCardId, chanId: ChanId): MythProtocolResult[Boolean] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "SHOULD_SWITCH_CARD", chanId)
    result map { case QueryRecorderBoolean(b) => b }
  }

  def queryRecorderSpawnLiveTV(cardId: CaptureCardId, usePiP: Boolean, channumStart: ChannelNumber): MythProtocolResult[Unit] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "SPAWN_LIVETV", usePiP, channumStart)
    result map { case QueryRecorderAcknowledgement => () }
  }

  def queryRecorderStopLiveTV(cardId: CaptureCardId): MythProtocolResult[Unit] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "STOP_LIVETV")
    result map { case QueryRecorderAcknowledgement => () }
  }

  def queryRecorderToggleChannelFavorite(cardId: CaptureCardId, channelGroup: String): MythProtocolResult[Unit] = {
    val result = sendCommand("QUERY_RECORDER", cardId, "TOGGLE_CHANNEL_FAVORITE", channelGroup)
    result map { case QueryRecorderAcknowledgement => () }
  }

  def queryRecording(pathName: String): MythProtocolResult[Recording] = {
    val result = sendCommand("QUERY_RECORDING", "BASENAME", pathName)
    result map { case r: Recording => r }
  }

  def queryRecording(chanId: ChanId, startTime: MythDateTime): MythProtocolResult[Recording] = {
    val result = sendCommand("QUERY_RECORDING", "TIMESLOT", chanId, startTime)
    result map { case r: Recording => r }
  }

  def queryRecordings(specifier: String): MythProtocolResult[ExpectedCountIterator[Recording]] = {
    val result = sendCommand("QUERY_RECORDINGS", specifier)
    result map { case it: ExpectedCountIterator[_] => it.asInstanceOf[ExpectedCountIterator[Recording]] }
  }

  def queryRemoteEncoderCancelNextRecording(cardId: CaptureCardId, cancel: Boolean): MythProtocolResult[Unit] = {
    val result = sendCommand("QUERY_REMOTEENCODER", cardId, "CANCEL_NEXT_RECORDING", cancel)
    result map { case QueryRemoteEncoderAcknowledgement => () }
  }

  def queryRemoteEncoderGetCurrentRecording(cardId: CaptureCardId): MythProtocolResult[Recording] = {
    val result = sendCommand("QUERY_REMOTEENCODER", cardId, "GET_CURRENT_RECORDING")
    result map { case QueryRemoteEncoderRecording(r) => r }
  }

  def queryRemoteEncoderGetFreeInputs(cardId: CaptureCardId, excludedCards: CaptureCardId*): MythProtocolResult[List[CardInput]] = {
    val args = List(cardId, "GET_FREE_INPUTS") ++ excludedCards
    val result = sendCommand("QUERY_REMOTEENCODER", args: _*)
    result map { case QueryRemoteEncoderCardInputList(i) => i }
  }

  def queryRemoteEncoderGetMaxBitrate(cardId: CaptureCardId): MythProtocolResult[Long] = {
    val result = sendCommand("QUERY_REMOTEENCODER", cardId, "GET_MAX_BITRATE")
    result map { case QueryRemoteEncoderBitrate(r) => r }
  }

  def queryRemoteEncoderGetRecordingStatus(cardId: CaptureCardId): MythProtocolResult[RecStatus] = {
    val result = sendCommand("QUERY_REMOTEENCODER", cardId, "GET_RECORDING_STATUS")
    result map { case QueryRemoteEncoderRecStatus(s) => s }
  }

  def queryRemoteEncoderGetState(cardId: CaptureCardId): MythProtocolResult[TvState] = {
    val result = sendCommand("QUERY_REMOTEENCODER", cardId, "GET_STATE")
    result map { case QueryRemoteEncoderState(s) => s }
  }

  def queryRemoteEncoderGetSleepStatus(cardId: CaptureCardId): MythProtocolResult[SleepStatus] = {
    val result = sendCommand("QUERY_REMOTEENCODER", cardId, "GET_SLEEPSTATUS")
    result map { case QueryRemoteEncoderSleepStatus(s) => s }
  }

  def queryRemoteEncoderGetFlags(cardId: CaptureCardId): MythProtocolResult[Int] = {
    val result = sendCommand("QUERY_REMOTEENCODER", cardId, "GET_FLAGS")
    result map { case QueryRemoteEncoderFlags(f) => f }
  }

  def queryRemoteEncoderIsBusy(cardId: CaptureCardId): MythProtocolResult[(Boolean, Option[CardInput], Option[ChanId])] = {
    val result = sendCommand("QUERY_REMOTEENCODER", cardId, "IS_BUSY")
    result map { case QueryRemoteEncoderTunedInputInfo(busy, input, chanId) => (busy, input, chanId) }
  }

  def queryRemoteEncoderIsBusy(cardId: CaptureCardId, timeBufferSeconds: Int): MythProtocolResult[(Boolean, Option[CardInput], Option[ChanId])] = {
    val result = sendCommand("QUERY_REMOTEENCODER", cardId, "IS_BUSY", timeBufferSeconds)
    result map { case QueryRemoteEncoderTunedInputInfo(busy, input, chanId) => (busy, input, chanId) }
  }

  def queryRemoteEncoderMatchesRecording(cardId: CaptureCardId, rec: Recording): MythProtocolResult[Boolean] = {
    val result = sendCommand("QUERY_REMOTEENCODER", cardId, "MATCHES_RECORDING", rec)
    result map { case QueryRemoteEncoderBoolean(r) => r }
  }

  def queryRemoteEncoderRecordPending(cardId: CaptureCardId, secondsLeft: Int, hasLaterShowing: Boolean, rec: Recording): MythProtocolResult[Unit] = {
    val result = sendCommand("QUERY_REMOTEENCODER", cardId, "RECORD_PENDING", secondsLeft, hasLaterShowing, rec)
    result map { case QueryRemoteEncoderAcknowledgement => () }
  }

  def queryRemoteEncoderStartRecording(cardId: CaptureCardId, rec: Recording): MythProtocolResult[RecStatus] = {
    val result = sendCommand("QUERY_REMOTEENCODER", cardId, "START_RECORDING", rec)
    result map { case QueryRemoteEncoderRecStatus(s) => s }
  }

  def queryRemoteEncoderStopRecording(cardId: CaptureCardId): MythProtocolResult[Unit] = {
    val result = sendCommand("QUERY_REMOTEENCODER", cardId, "STOP_RECORDING")
    result map { case QueryRemoteEncoderAcknowledgement => () }
  }

  def querySGGetFileList(hostName: String, storageGroup: String, path: String): MythProtocolResult[List[StorageGroupInfo]] = {
    val result = sendCommand("QUERY_SG_GETFILELIST", hostName, storageGroup, path)
    result map { case xs: List[_] => xs.asInstanceOf[List[StorageGroupInfo]] }
  }

  def querySGGetFileList(hostName: String, storageGroup: String, path: String, fileNamesOnly: Boolean): MythProtocolResult[List[StorageGroupInfo]] = {
    val result = sendCommand("QUERY_SG_GETFILELIST", hostName, storageGroup, path, fileNamesOnly)
    result map { case xs: List[_] => xs.asInstanceOf[List[StorageGroupInfo]] }
  }

  def querySGFileQuery(hostName: String, storageGroup: String, fileName: String): MythProtocolResult[(String, MythDateTime, ByteCount)] = {
    val result = sendCommand("QUERY_SG_FILEQUERY", hostName, storageGroup, fileName)
    result map { case (fullPath: String, fileTime: MythDateTime, fileSize: ByteCount) => (fullPath, fileTime, fileSize) }
  }

  def querySetting(hostName: String, settingName: String): MythProtocolResult[String] = {
    val result = sendCommand("QUERY_SETTING", hostName, settingName)
    result map { case s: String => s }
  }

  def queryTimeZone: MythProtocolResult[TimeZoneInfo] = {
    val result = sendCommand("QUERY_TIME_ZONE")
    result map { case tzi: TimeZoneInfo => tzi }
  }

  def queryUptime: MythProtocolResult[Duration] = {
    val result = sendCommand("QUERY_UPTIME")
    result map { case d: Duration => d }
  }

  def refreshBackend: MythProtocolResult[Boolean] = {
    val result = sendCommand("REFRESH_BACKEND")
    result map { case r: Boolean => r }
  }

  def rescheduleRecordingsCheck(recStatus: RecStatus, recordId: RecordRuleId, findId: Int, title: String, subtitle: String,
    description: String, programId: String, reason: String): MythProtocolResult[Boolean] = {
    val result = sendCommand("RESCHEDULE_RECORDINGS", "CHECK", recStatus, recordId, findId, reason,
      title, subtitle, description, programId)
    result map { case r: Boolean => r }
  }

  def rescheduleRecordingsMatch(recordId: RecordRuleId, sourceId: ListingSourceId, mplexId: MultiplexId,
    maxStartTime: Option[MythDateTime], reason: String): MythProtocolResult[Boolean] = {
    val result =
      if (maxStartTime.isEmpty) sendCommand("RESCHEDULE_RECORDINGS", "MATCH", recordId, sourceId, mplexId, reason)
      else sendCommand("RESCHEDULE_RECORDINGS", "MATCH", recordId, sourceId, mplexId, maxStartTime.get, reason)
    result map { case r: Boolean => r }
  }

  def rescheduleRecordingsPlace(reason: String): MythProtocolResult[Boolean] = {
    val result = sendCommand("RESCHEDULE_RECORDINGS", "PLACE", reason)
    result map { case r: Boolean => r }
  }

  def scanMusic(): MythProtocolResult[Unit] = {
    val result = sendCommand("SCAN_MUSIC")
    result map { case () => () }
  }

  def scanVideos(): MythProtocolResult[Boolean] = {
    val result = sendCommand("SCAN_VIDEOS")
    result map { case r: Boolean => r }
  }

  def setBookmark(chanId: ChanId, startTime: MythDateTime, pos: VideoPositionFrame): MythProtocolResult[Boolean] = {
    val result = sendCommand("SET_BOOKMARK", chanId, startTime, pos)
    result map { case r: Boolean => r }
  }

  def setChannelInfo(chanId: ChanId, sourceId: ListingSourceId, oldChanNum: ChannelNumber, callsign: String,
    channum: ChannelNumber, name: String, xmltvId: String): MythProtocolResult[Boolean] = {
    val result = sendCommand("SET_CHANNEL_INFO", chanId, sourceId, oldChanNum, callsign, channum, name, xmltvId)
    result map { case r: Boolean => r }
  }

  def setSetting(hostName: String, settingName: String, value: String): MythProtocolResult[Boolean] = {
    val result = sendCommand("SET_SETTING", hostName, settingName, value)
    result map { case r: Boolean => r }
  }

  def shutdownNow(haltCommand: String): Unit = {
    if (haltCommand == "") sendCommand("SHUTDOWN_NOW")
    else postCommand("SHUTDOWN_NOW", haltCommand)
  }

  def stopRecording(rec: Recording): MythProtocolResult[CaptureCardId] = {
    val result = sendCommand("STOP_RECORDING", rec)
    result map { case c: CaptureCardId => c }
  }

  def undeleteRecording(rec: Recording): MythProtocolResult[Boolean] = {
    val result = sendCommand("UNDELETE_RECORDING", rec)
    result map { case r: Boolean => r }
  }

  def undeleteRecording(chanId: ChanId, startTime: MythDateTime): MythProtocolResult[Boolean] = {
    val result = sendCommand("UNDELETE_RECORDING", chanId, startTime)
    result map { case r: Boolean => r }
  }
}
