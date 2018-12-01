// SPDX-License-Identifier: LGPL-2.1-only
/*
 * images.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package model

import util.LooseEnum

sealed trait ImageId extends Any with IntegerIdentifier

final case class ImageFileId(id: Int) extends AnyVal with ImageId

final case class ImageDirId(id: Int) extends AnyVal with ImageId

final case class ImageUnknownId(id: Int) extends AnyVal with ImageId

final case class ImageSyncStatus(running: Boolean, progress: Int, total: Int)

object ImageFileTransform extends LooseEnum {
  type ImageFileTransform = Value
  final val ResetToExif            = Value(0)
  final val RotateClockwise        = Value(1)
  final val RotateCounterClockwise = Value(2)
  final val FlipHorizontal         = Value(3)
  final val FlipVertical           = Value(4)
}
