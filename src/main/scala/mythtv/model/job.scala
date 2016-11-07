package mythtv
package model

import java.time.LocalDateTime

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
