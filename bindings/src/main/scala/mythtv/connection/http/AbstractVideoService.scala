// SPDX-License-Identifier: LGPL-2.1-only
/*
 * AbstractVideoService.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package http

import java.time.{ Duration, Instant, LocalDate, Year }

import model._
import model.EnumTypes.{ MetadataGrabberType, ParentalLevel, VideoContentType }
import services.{ ServiceResult, VideoService }
import services.Service.ServiceFailure.ServiceNoResult
import util.{ MythDateTime, MythFileHash, OptionalCount, PagedList }

trait AbstractVideoService extends ServiceProtocol with VideoService {

  def getVideo(videoId: VideoId): ServiceResult[Video] = {
    val params: Map[String, Any] = Map("Id" -> videoId.id)
    val vidTry = request[Video]("GetVideo", params)("VideoMetadataInfo")
    if (vidTry.isSuccess && vidTry.get.id.id == 0) Left(ServiceNoResult)
    else vidTry
  }

  def getVideoByFileName(fileName: String): ServiceResult[Video] = {
    val params: Map[String, Any] = Map("FileName" -> fileName)
    request("GetVideoByFileName", params)("VideoMetadataInfo")
  }

  def getVideoList(
    folder: String,
    sortBy: String,
    startIndex: Int,
    count: OptionalCount[Int],
    descending: Boolean
  ): ServiceResult[PagedList[Video]] = {
    var params = buildStartCountParams(startIndex, count)
    if (folder.nonEmpty) params += "Folder"     -> folder
    if (sortBy.nonEmpty) params += "Sort"       -> sortBy
    if (descending)      params += "Descending" -> descending
    request("GetVideoList", params)("VideoMetadataInfoList")
  }

  def getBluray(path: String): ServiceResult[BlurayInfo] = {
    val params: Map[String, Any] = Map("Path" -> path)
    val bdTry = request[BlurayInfo]("GetBluray", params)("BlurayInfo")
    if (bdTry.isSuccess && bdTry.get.title.isEmpty) Left(ServiceNoResult)
    else bdTry
  }

  def lookupVideo(
    title: String,
    subtitle: String,
    inetRef: String,
    season: Int,
    episode: Int,
    grabberType: MetadataGrabberType,
    allowGeneric: Boolean
  ): ServiceResult[List[VideoLookup]] = {
    var params: Map[String, Any] = Map.empty
    if (title.nonEmpty)    params += "Title"    -> title
    if (subtitle.nonEmpty) params += "Subtitle" -> subtitle
    if (inetRef.nonEmpty)  params += "Inetref"  -> inetRef
    if (season != 0)       params += "Season"   -> season
    if (episode != 0)      params += "Episode"  -> episode
    if (grabberType != MetadataGrabberType.Unknown) params += "GrabberType" -> grabberType.toString
    if (allowGeneric)      params += "AllowGeneric" -> allowGeneric
    request("LookupVideo", params)("VideoLookupList")
  }

  /* mutating POST methods */

  def addVideo(fileName: String, hostName: String): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "FileName" -> fileName,
      "HostName" -> hostName
    )
    post("AddVideo", params)()
  }

  def removeVideoFromDb(videoId: VideoId): ServiceResult[Boolean] = {
    post("RemoveVideoFromDB", Map("Id" -> videoId.id))()
  }

  /* Added to API on 6 Apr 2016 */
  def updateVideoWatchedStatus(videoId: VideoId, watched: Boolean): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map("Id" -> videoId.id, "Watched" -> watched)
    post("UpdateVideoWatchedStatus", params)()
  }

  def updateVideoMetadata(video: Video): ServiceResult[Boolean] = {
    val params: Map[String, Any] = Map(
      "Id"            -> video.id.id,
      "Title"         -> video.title,
      "SubTitle"      -> video.subtitle,
      "TagLine"       -> video.tagline.getOrElse(""),
      "Director"      -> video.director,
      "Studio"        -> video.studio.getOrElse(""),
      "Plot"          -> video.description,
      "Season"        -> video.season,
      "Episode"       -> video.episode,
      "Rating"        -> video.rating,
      "Inetref"       -> video.inetRef,
      "HomePage"      -> video.homePage.getOrElse(""),
      "Year"          -> video.year.map(_.getValue).getOrElse(0),
      "ReleaseDate"   -> video.releasedDate.toString,
      "UserRating"    -> video.userRating,
      "Length"        -> video.length.map(_.toMinutes).getOrElse(0),
      "PlayCount"     -> video.playCount,
      "FileName"      -> video.fileName,
      "Host"          -> video.hostName,
      "Hash"          -> video.hash.toString,
      "ShowLevel"     -> video.parentalLevel.id,
      "Watched"       -> video.watched,
      "Processed"     -> video.processed,
      "CollectionRef" -> video.collectionRef.getOrElse(0),
      "Trailer"       -> video.trailer,
      "CoverFile"     -> video.coverFile,
      "Screenshot"    -> video.screenshot,
      "Banner"        -> video.banner,
      "Fanart"        -> video.fanart,
      "InsertDate"    -> video.addedDate.map(MythDateTime(_).toIsoFormat),
      "ContentType"   -> video.contentType.toString,
      "Genres"        -> video.genres.mkString(","),
    )
    post("UpdateVideoMetadata", params)()
  }

  def updateVideoMetadata(videoId: VideoId, lookup: VideoLookup): ServiceResult[Boolean] = {
    var params: Map[String, Any] = Map(
      "Id"          -> videoId.id,
      "Title"       -> lookup.title,
      "SubTitle"    -> lookup.subtitle,
      "TagLine"     -> lookup.tagline,
      "Plot"        -> lookup.description,
      "Season"      -> lookup.season,
      "Episode"     -> lookup.episode.getOrElse(0),
      "Rating"      -> lookup.certification.getOrElse(""),
      "Inetref"     -> lookup.inetRef,
      "HomePage"    -> lookup.homePage,
      "Year"        -> lookup.year,
      "ReleaseDate" -> MythDateTime(lookup.releasedDate).toIsoFormat,
      "UserRating"  -> lookup.userRating.getOrElse(0),
      "Length"      -> lookup.length.getOrElse(0),
      "Countries"   -> lookup.countries.mkString(",")
    )
    val coverart = lookup.artwork.find(_.artworkType.toLowerCase == "coverart")
    val screenshot = lookup.artwork.find(_.artworkType.toLowerCase == "screenshot")
    val banner = lookup.artwork.find(_.artworkType.toLowerCase == "banner")
    val fanart = lookup.artwork.find(_.artworkType.toLowerCase == "fanart")
    if (coverart.nonEmpty)   params += "CoverFile" -> coverart.get
    if (screenshot.nonEmpty) params += "Screenshot" -> screenshot.get
    if (banner.nonEmpty)     params += "Banner" -> banner.get
    if (fanart.nonEmpty)     params += "Fanart" -> fanart.get

    post("UpdateVideoMetadata", params)()
  }

  def updateVideoMetadata(
    videoId: VideoId,
    title: Option[String],
    subTitle: Option[String],
    tagLine: Option[String],
    director: Option[String],
    studio: Option[String],
    plot: Option[String],
    season: Option[Int],
    episode: Option[Int],
    rating: Option[String],
    inetRef: Option[String],
    homePage: Option[String],
    year: Option[Year],
    releasedDate: Option[LocalDate],
    userRating: Option[Double],
    length: Option[Duration],
    playCount: Option[Int],
    fileName: Option[String],
    hostName: Option[String],
    hash: Option[MythFileHash],
    parentalLevel: Option[ParentalLevel],
    browse: Option[Boolean],
    watched: Option[Boolean],
    processed: Option[Boolean],
    playCommand: Option[String],
    collectionRef: Option[Int],
    childId: Option[Int],
    categoryId: Option[Int],
    trailer: Option[String],
    coverFile: Option[String],
    screenshot: Option[String],
    banner: Option[String],
    fanart: Option[String],
    addedDate: Option[Instant],
    contentType: Option[VideoContentType],
    cast: List[String],
    genres: List[String],
    countries: List[String]
  ): ServiceResult[Boolean] = {
    var params: Map[String, Any] = Map("Id" -> videoId.id)
    if (title.nonEmpty)         params += "Title" -> title.get
    if (subTitle.nonEmpty)      params += "SubTitle" -> subTitle.get
    if (tagLine.nonEmpty)       params += "TagLine" -> tagLine.get
    if (director.nonEmpty)      params += "Director" -> director.get
    if (studio.nonEmpty)        params += "Studio" -> studio.get
    if (plot.nonEmpty)          params += "Plot" -> plot.get
    if (season.nonEmpty)        params += "Season" -> season.get
    if (episode.nonEmpty)       params += "Episode" -> episode.get
    if (rating.nonEmpty)        params += "Rating" -> rating.get
    if (inetRef.nonEmpty)       params += "Inetref" -> inetRef.get
    if (homePage.nonEmpty)      params += "HomePage" -> homePage.get
    if (year.nonEmpty)          params += "Year" -> year.get.getValue
    if (releasedDate.nonEmpty)  params += "ReleaseDate" -> releasedDate.get.toString
    if (userRating.nonEmpty)    params += "UserRating" -> userRating.get
    if (length.nonEmpty)        params += "Length" -> length.get.toMinutes
    if (playCount.nonEmpty)     params += "PlayCount" -> playCount.get
    if (fileName.nonEmpty)      params += "FileName" -> fileName.get
    if (hostName.nonEmpty)      params += "Host" -> hostName.get
    if (hash.nonEmpty)          params += "Hash" -> hash.get.toString
    if (parentalLevel.nonEmpty) params += "ShowLevel" -> parentalLevel.get.id
    if (browse.nonEmpty)        params += "Browse" -> browse.get
    if (watched.nonEmpty)       params += "Watched" -> watched.get
    if (processed.nonEmpty)     params += "Processed" -> processed.get
    if (playCommand.nonEmpty)   params += "PlayCommand" -> playCommand.get
    if (collectionRef.nonEmpty) params += "CollectionRef" -> collectionRef.get
    if (childId.nonEmpty)       params += "ChildID" -> childId.get
    if (categoryId.nonEmpty)    params += "Category" -> categoryId.get
    if (trailer.nonEmpty)       params += "Trailer" -> trailer.get
    if (coverFile.nonEmpty)     params += "CoverFile" -> coverFile.get
    if (screenshot.nonEmpty)    params += "Screenshot" -> screenshot.get
    if (banner.nonEmpty)        params += "Banner" -> banner.get
    if (fanart.nonEmpty)        params += "Fanart" -> fanart.get
    if (addedDate.nonEmpty)     params += "InsertDate" -> MythDateTime(addedDate.get).toIsoFormat
    if (contentType.nonEmpty)   params += "ContentType" -> contentType.get.toString
    if (cast.nonEmpty)          params += "Cast" -> cast.mkString(",")
    if (genres.nonEmpty)        params += "Genres" -> genres.mkString(",")
    if (countries.nonEmpty)     params += "Countries" -> countries.mkString(",")
    post("UpdateVideoMetadata", params)()
  }
}
