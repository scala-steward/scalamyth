package mythtv
package connection
package myth

import java.time.Instant

import model.EnumTypes.MusicImageType
import model.{ AlbumArtImage, MusicImageId, RemoteSong }

trait MusicSongApi {
  def calcTrackLength(): MythProtocolResult[Unit]
  def findAlbumArt(updateDatabase: Boolean): MythProtocolResult[List[AlbumArtImage]]
  def lyricsFind(grabberName: String): MythProtocolResult[Unit]
  def lyricsSave(lyricsLines: Seq[String]): MythProtocolResult[Unit]
  def tagAddImage(fileName: String, imageType: MusicImageType): MythProtocolResult[Unit]
  def tagChangeImage(oldType: MusicImageType, newType: MusicImageType): MythProtocolResult[Unit]
  def tagGetImage(imageType: MusicImageType): MythProtocolResult[Unit]
  def tagRemoveImage(imageId: MusicImageId): MythProtocolResult[Unit]
  def tagUpdateMetadata(): MythProtocolResult[Unit]
  def tagUpdateVolatile(rating: Int, playCount: Int, lastPlayed: Instant): MythProtocolResult[Unit]
}

trait MusicSongApiLike extends RemoteSong with MusicSongApi {
  protected def protoApi: MythProtocolApi88

  def calcTrackLength(): MythProtocolResult[Unit] =
    protoApi.musicCalcTrackLength(hostName, songId)

  def findAlbumArt(updateDatabase: Boolean): MythProtocolResult[List[AlbumArtImage]] =
    protoApi.musicFindAlbumArt(hostName, songId, updateDatabase)

  def lyricsFind(grabberName: String): MythProtocolResult[Unit] =
    protoApi.musicLyricsFind(hostName, songId, grabberName)

  def lyricsSave(lyricsLines: Seq[String]): MythProtocolResult[Unit] =
    protoApi.musicLyricsSave(hostName, songId, lyricsLines)

  def tagAddImage(fileName: String, imageType: MusicImageType): MythProtocolResult[Unit] =
    protoApi.musicTagAddImage(hostName, songId, fileName, imageType)

  def tagChangeImage(oldType: MusicImageType, newType: MusicImageType): MythProtocolResult[Unit] =
    protoApi.musicTagChangeImage(hostName, songId, oldType, newType)

  def tagGetImage(imageType: MusicImageType): MythProtocolResult[Unit] =
    protoApi.musicTagGetImage(hostName, songId, imageType)

  def tagRemoveImage(imageId: MusicImageId): MythProtocolResult[Unit] =
    protoApi.musicTagRemoveImage(hostName, songId, imageId)

  def tagUpdateMetadata(): MythProtocolResult[Unit] =
    protoApi.musicTagUpdateMetadata(hostName, songId)

  def tagUpdateVolatile(rating: Int, playCount: Int, lastPlayed: Instant): MythProtocolResult[Unit] =
    protoApi.musicTagUpdateVolatile(hostName, songId, rating, playCount, lastPlayed)
}
