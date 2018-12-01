// SPDX-License-Identifier: LGPL-2.1-only
/*
 * VideoService.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package services

import java.time.{ Duration, Instant, LocalDate, Year }

import util.{ MythFileHash, OptionalCount }
import model.{ BlurayInfo, MetadataGrabberType, Video, VideoId, VideoLookup }
import model.EnumTypes.{ MetadataGrabberType, ParentalLevel, VideoContentType }

trait VideoService extends BackendService {
  final def serviceName: String = "Video"

  def getVideo(id: VideoId): ServiceResult[Video]
  def getVideoByFileName(fileName: String): ServiceResult[Video]

  // For MythTV 0.28, getVideoList gains two parameters: Folder and Sort
  def getVideoList(
    folder: String = "",
    sortBy: String = "",
    startIndex: Int = 0,
    count: OptionalCount[Int] = OptionalCount.all,
    descending: Boolean = false
  ): ServiceResult[PagedList[Video]]

  def getBluray(path: String): ServiceResult[BlurayInfo]

  /*
     TODO UPSTREAM LookupVideo seems to fail or return unexpected results often on my 0.27 setup,
     epescially for movies. Try looking for "Juno" or "Gravity" and see if we get any results.
     Is there a problem with the TMDB grabber or processing its output? A query for Title=Gravity
     with the grabber directly fails with KeyError=25, while Juno returns results with the grabber
     but they are not reflected in the results of this service call.

     This seems to be working better on my test 0.28 installation, as the movies "Juno" and "Gravity"
     appeart first in their respective result set.
   */

  /*
   * The lookupVideo calls may take significant time to complete, which may result in a timeout
   * if the timeout threshold is set too low.
   */

  // perform metadata lookup for a video
  def lookupVideo(
    title: String,
    subtitle: String,
    inetRef: String,
    season: Int = 0,
    episode: Int = 0,
    grabberType: MetadataGrabberType = MetadataGrabberType.Unknown,
    allowGeneric: Boolean = false
  ): ServiceResult[List[VideoLookup]]

  def lookupVideoTitle(
    title: String,
    subtitle: String = "",
    season: Int = 0,
    episode: Int = 0,
    inetRef: String = "",
    grabberType: MetadataGrabberType = MetadataGrabberType.Unknown,
    allowGeneric: Boolean = false
  ): ServiceResult[List[VideoLookup]] =
    lookupVideo(title, subtitle, inetRef, season, episode, grabberType, allowGeneric)

  def lookupVideoInetref(
    inetRef: String,
    season: Int = 0,
    episode: Int = 0,
    title: String = "",
    subtitle: String = "",
    grabberType: MetadataGrabberType = MetadataGrabberType.Unknown,
    allowGeneric: Boolean = false
  ): ServiceResult[List[VideoLookup]] =
    lookupVideo(title, subtitle, inetRef, season, episode, grabberType, allowGeneric)

  /* mutating POST methods */

  def addVideo(fileName: String, hostName: String): ServiceResult[Boolean]

  def removeVideoFromDb(videoId: VideoId): ServiceResult[Boolean]

  /* Added to API on 6 Apr 2016 */
  def updateVideoWatchedStatus(videoId: VideoId, watched: Boolean): ServiceResult[Boolean]

  // updateVideoMetadata is new for MythTV 29
  def updateVideoMetadata(video: Video): ServiceResult[Boolean]
  def updateVideoMetadata(videoId: VideoId, lookup: VideoLookup): ServiceResult[Boolean]

  def updateVideoMetadata(
    videoId: VideoId,
    title: Option[String] = None,
    subTitle: Option[String] = None,
    tagLine: Option[String] = None,
    director: Option[String] = None,
    studio: Option[String] = None,
    plot: Option[String] = None,
    season: Option[Int] = None,
    episode: Option[Int] = None,
    rating: Option[String] = None,
    inetRef: Option[String] = None,
    homePage: Option[String] = None,
    year: Option[Year] = None,
    releasedDate: Option[LocalDate] = None,
    userRating: Option[Double] = None,
    length: Option[Duration] = None,
    playCount: Option[Int] = None,
    fileName: Option[String] = None,
    hostName: Option[String] = None,
    hash: Option[MythFileHash] = None,
    parentalLevel: Option[ParentalLevel] = None,
    browse: Option[Boolean] = None,
    watched: Option[Boolean] = None,
    processed: Option[Boolean] = None,
    playCommand: Option[String] = None,
    collectionRef: Option[Int] = None,
    childId: Option[Int] = None,
    categoryId: Option[Int] = None,
    trailer: Option[String] = None,
    coverFile: Option[String] = None,
    screenshot: Option[String] = None,
    banner: Option[String] = None,
    fanart: Option[String] = None,
    addedDate: Option[Instant] = None,
    contentType: Option[VideoContentType] = None,
    cast: List[String] = Nil,
    genres: List[String] = Nil,
    countries: List[String] = Nil
  ): ServiceResult[Boolean]
}
