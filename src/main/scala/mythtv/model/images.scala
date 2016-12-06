package mythtv
package model

import util.LooseEnum

sealed trait ImageId extends Any with IntegerIdentifier

final case class ImageFileId(id: Int) extends AnyVal with ImageId

final case class ImageDirId(id: Int) extends AnyVal with ImageId

private[mythtv] final case class ImageUnknownId(id: Int) extends AnyVal with ImageId

object ImageFileTransform extends LooseEnum {
  type ImageFileTransform = Value
  val ResetToExif            = Value(0)
  val RotateClockwise        = Value(1)
  val RotateCounterClockwise = Value(2)
  val FlipHorizontal         = Value(3)
  val FlipVertical           = Value(4)
}
