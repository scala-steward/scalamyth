package mythtv
package model

import java.time.LocalDateTime

import util.{ IntBitmaskEnum, LooseEnum }
import EnumTypes.{ JobFlags, JobStatus, JobType }

final case class JobId(id: Int) extends AnyVal

trait Job {
  def id: JobId
  def jobType: JobType
  def chanId: ChanId
  def startTime: LocalDateTime
  def comment: String
  def hostname: String
  def flags: JobFlags
  def status: JobStatus
  def statusTime: LocalDateTime
  def insertedTime: LocalDateTime
  def schedRunTime: LocalDateTime
}

object JobCommand extends LooseEnum {
  type JobCommand = Value
  final val Run     = Value(0x0000)
  final val Pause   = Value(0x0001)
  final val Resume  = Value(0x0002)
  final val Stop    = Value(0x0004)
  final val Restart = Value(0x0008)
}

object JobFlags extends IntBitmaskEnum {
  type JobFlags = Base
  final val None       =  Mask(0x0000)
  final val UseCutlist = Value(0x0001)
  final val LiveRec    = Value(0x0002)
  final val External   = Value(0x0004)
  final val Rebuild    = Value(0x0008)
}

object JobStatus extends LooseEnum {
  type JobStatus = Value
  final val Unknown     = Value(0x0000)
  final val Queued      = Value(0x0001)
  final val Pending     = Value(0x0002)
  final val Starting    = Value(0x0003)
  final val Running     = Value(0x0004)
  final val Stopping    = Value(0x0005)
  final val Paused      = Value(0x0006)
  final val Retry       = Value(0x0007)
  final val Erroring    = Value(0x0008)
  final val Aborting    = Value(0x0009)
  final val Finished    = Value(0x0110)
  final val Aborted     = Value(0x0120)
  final val Errored     = Value(0x0130)
  final val Cancelled   = Value(0x0140)

  def isDone(status: JobStatus): Boolean = new RichJobStatus(status).isDone

  implicit class RichJobStatus(val status: JobStatus) extends AnyVal {
    def isDone: Boolean = (status.id & 0x100) != 0
  }
}

object JobType extends LooseEnum {
  type JobType = Value
  final val None      = Value(0x0000)
  final val Transcode = Value(0x0001)
  final val CommFlag  = Value(0x0002)
  final val Metadata  = Value(0x0004)
  final val UserJob1  = Value(0x0100)
  final val UserJob2  = Value(0x0200)
  final val UserJob3  = Value(0x0400)
  final val UserJob4  = Value(0x0800)

  def isSystem(jt: JobType): Boolean = new RichJobType(jt).isSystem
  def isUser(jt: JobType): Boolean   = new RichJobType(jt).isUser

  implicit class RichJobType(val jobtype: JobType) extends AnyVal {
    def isSystem: Boolean = (jobtype.id & 0x00ff) != 0
    def isUser: Boolean   = (jobtype.id & 0xff00) != 0
  }
}
