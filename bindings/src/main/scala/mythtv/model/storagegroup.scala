// SPDX-License-Identifier: LGPL-2.1-only
/*
 * storagegroup.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package model

import util.ByteCount

trait FreeSpace {
  def host: String
  def path: String
  def isLocal: Boolean
  def diskNumber: Int
  def sGroupId: StorageGroupId
  def blockSize: ByteCount
  def totalSpace: ByteCount
  def usedSpace: ByteCount
  def freeSpace: ByteCount
}

/*
 * Built-in storage groups
 * =======================
 * These storage groups are always available, whether or not they have been
 * configured by the end user. If not otherwise configured, they are default
 * to subdirectories underneath the MythTV configuration directory, which in
 * many cases defaults to ~mythtv/.mythtv and is noted in the log at startup.
 *
 * Storage Group Name      Default directory
 * ------------------      --------------------
 * ChannelIcons            ${confdir}/channels
 * Themes                  ${confdir}/themes
 * Temp                    ${confdir}/tmp
 * Streaming               ${confdir}/tmp/hls
 * 3rdParty                ${confdir}/3rdParty
 *
 *
 * Special storage groups
 * =======================
 * 0.27: LiveTV, DB Backups, Videos, Trailers, Coverart, Fanart, Screenshots, Banners
 * 0.28: Photographs, Music, MusicArt
 *
 *
 * See libs/libmythbase/storagegroup.cpp for more info on built-in and special
 * storage groups.
 *
 * See libs/libmythbase/mythdirs.cpp for more info on finding the MythTV
 * configuration directory.
 */

final case class StorageGroupId(id: Int) extends AnyVal with IntegerIdentifier

trait StorageGroupDir {
  def id: StorageGroupId
  def groupName: String
  def hostName: String
  def dirName: String

  override def toString: String = s"<StorageGroupDir $id $groupName@$hostName: $dirName>"
}

// Only used in Myth protocol, querySGGetFileList
sealed trait StorageGroupInfo
final case class StorageGroupInfoRoot(rootDir: String) extends StorageGroupInfo
final case class StorageGroupInfoDir(dirName: String) extends StorageGroupInfo
final case class StorageGroupInfoFile(fileName: String, size: ByteCount, fullPath: String) extends StorageGroupInfo
