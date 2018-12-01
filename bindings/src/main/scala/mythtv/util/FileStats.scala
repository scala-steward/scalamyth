// SPDX-License-Identifier: LGPL-2.1-only
/*
 * FileStats.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package util

import java.time.Instant

case class FileStats(
  deviceId: Long,           // st_dev
  inode: Long,              // st_ino
  mode: Int,                // st_mode
  numLinks: Long,           // st_nlink
  uid: Int,                 // st_uid
  gid: Int,                 // st_gid
  rDevId: Long,             // st_rdev
  size: ByteCount,          // st_size
  fsBlockSize: ByteCount,   // st_blksize
  blocks: Long,             // st_blocks
  accessTime: Instant,      // st_atim
  modifyTime: Instant,      // st_mtim
  changeTime: Instant       // st_ctim
)
