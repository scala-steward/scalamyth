// SPDX-License-Identifier: LGPL-2.1-only
/*
 * ByteCount.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package util

trait ByteCount extends Any {
  def bytes: Long
  def kilobytes: Long
  def megabytes: Long
  def gigabytes: Long
  def terabytes: Long

  def b: Long = bytes
  def kb: Long = kilobytes
  def mb: Long = megabytes
  def gb: Long = gigabytes
  def tb: Long = terabytes

  def + (that: ByteCount): ByteCount
  def - (that: ByteCount): ByteCount
  def * (that: ByteCount): ByteCount
  def / (that: ByteCount): ByteCount

  def toBinaryByteCount: BinaryByteCount
  def toDecimalByteCount: DecimalByteCount

  protected def toString(units: Array[String], thresholds: Array[Long]): String = {
    val pairs = (units zip thresholds).reverse
    val chosen = pairs find { case (_, t) => t <= bytes }
    val r = chosen map {
      case (u, t) if t == 0 => bytes.toString + " " + u
      case (u, t) => (bytes / t.toDouble).toString + " " + u  // TODO control precision of output
    }
    r getOrElse bytes.toString
  }
}

object ByteCount {
  import scala.language.implicitConversions
  implicit def bc2binbc(v: ByteCount): BinaryByteCount = BinaryByteCount(v.bytes)
  implicit def bc2decbc(v: ByteCount): DecimalByteCount = DecimalByteCount(v.bytes)
}

trait BinaryByteCounter extends Any {
  self: ByteCount =>
  def kilobytes: Long = bytes >> 10
  def megabytes: Long = bytes >> 20
  def gigabytes: Long = bytes >> 30
  def terabytes: Long = bytes >> 40

  def displayUnits = Array("B", "KiB", "MiB", "GiB", "TiB")
  def thresholds = Array(0, 1024, 1048576, 1073741824, 1099511627776L)
}

trait DecimalByteCounter extends Any {
  self: ByteCount =>
  def kilobytes: Long = bytes /  1000L
  def megabytes: Long = bytes / (1000L * 1000)
  def gigabytes: Long = bytes / (1000L * 1000 * 1000)
  def terabytes: Long = bytes / (1000L * 1000 * 1000 * 1000)

  def displayUnits = Array("B", "kB", "MB", "GB", "TB")
  def thresholds = Array(0, 1000, 1000000, 1000000000, 1000000000000L)
}

case class BinaryByteCount(bytes: Long) extends AnyVal with ByteCount with BinaryByteCounter {
  def + (that: ByteCount): BinaryByteCount = BinaryByteCount(bytes + that.bytes)
  def - (that: ByteCount): BinaryByteCount = BinaryByteCount(bytes - that.bytes)
  def * (that: ByteCount): BinaryByteCount = BinaryByteCount(bytes * that.bytes)
  def / (that: ByteCount): BinaryByteCount = BinaryByteCount(bytes / that.bytes)

  def toBinaryByteCount = this
  def toDecimalByteCount = DecimalByteCount(bytes)

  override def toString: String = {
    super.toString(displayUnits, thresholds)
  }
}

case class DecimalByteCount(bytes: Long) extends AnyVal with ByteCount with DecimalByteCounter {
  def + (that: ByteCount): DecimalByteCount = DecimalByteCount(bytes + that.bytes)
  def - (that: ByteCount): DecimalByteCount = DecimalByteCount(bytes - that.bytes)
  def * (that: ByteCount): DecimalByteCount = DecimalByteCount(bytes * that.bytes)
  def / (that: ByteCount): DecimalByteCount = DecimalByteCount(bytes / that.bytes)

  def toBinaryByteCount = BinaryByteCount(bytes)
  def toDecimalByteCount = this

  override def toString: String = {
    super.toString(displayUnits, thresholds)
  }
}
