package mythtv
package services

import model.{ ChanId, LiveStreamInfo }
import util.{ MythDateTime, MythFileHash }

trait ContentService extends BackendService {
  def serviceName: String = "Content"

  def getAlbumArt(id: Int): DataBytes  // TODO optional width, height
  def getFile(storageGroup: String, fileName: String): DataBytes
  def getFileList(storageGroup: String): List[String]
  def getHash(storageGroup: String, fileName: String): MythFileHash
  def getImageFile(storageGroup: String, fileName: String): DataBytes  // optional width, height
  def getLiveStream(id: String): LiveStreamInfo  // TODO is id really a string or an Int?
  def getLiveStreamList: List[LiveStreamInfo]
  def getMusic(id: String): DataBytes  // TODO is id really a string or an int?
  def getPreviewImage(chanId: ChanId, startTime: MythDateTime): DataBytes // TODO optional params
  def getRecording(chanId: ChanId, startTime: MythDateTime): DataBytes
  def getVideo(id: Int): DataBytes
  // TODO more methods
}
