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
  val Run     = Value(0x0000)
  val Pause   = Value(0x0001)
  val Resume  = Value(0x0002)
  val Stop    = Value(0x0004)
  val Restart = Value(0x0008)
}

object JobFlags extends IntBitmaskEnum {
  type JobFlags = Base
  val None       =  Mask(0x0000)
  val UseCutlist = Value(0x0001)
  val LiveRec    = Value(0x0002)
  val External   = Value(0x0004)
  val Rebuild    = Value(0x0008)
}

object JobStatus extends LooseEnum {
  type JobStatus = Value
  val Unknown     = Value(0x0000)
  val Queued      = Value(0x0001)
  val Pending     = Value(0x0002)
  val Starting    = Value(0x0003)
  val Running     = Value(0x0004)
  val Stopping    = Value(0x0005)
  val Paused      = Value(0x0006)
  val Retry       = Value(0x0007)
  val Erroring    = Value(0x0008)
  val Aborting    = Value(0x0009)
  val Finished    = Value(0x0110)
  val Aborted     = Value(0x0120)
  val Errored     = Value(0x0130)
  val Cancelled   = Value(0x0140)

  def isDone(status: JobStatus): Boolean = new RichJobStatus(status).isDone

  implicit class RichJobStatus(val status: JobStatus) extends AnyVal {
    def isDone: Boolean = (status.id & 0x100) != 0
  }
}

object JobType extends LooseEnum {
  type JobType = Value
  val None      = Value(0x0000)
  val Transcode = Value(0x0001)
  val CommFlag  = Value(0x0002)
  val Metadata  = Value(0x0004)
  val UserJob1  = Value(0x0100)
  val UserJob2  = Value(0x0200)
  val UserJob3  = Value(0x0400)
  val UserJob4  = Value(0x0800)

  def isSystem(jt: JobType): Boolean = new RichJobType(jt).isSystem
  def isUser(jt: JobType): Boolean   = new RichJobType(jt).isUser

  implicit class RichJobType(val jobtype: JobType) extends AnyVal {
    def isSystem: Boolean = (jobtype.id & 0x00ff) != 0
    def isUser: Boolean   = (jobtype.id & 0xff00) != 0
  }
}
