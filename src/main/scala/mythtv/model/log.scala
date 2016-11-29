package mythtv
package model

import java.time.Instant

trait LogMessage {
  def hostName: String
  def application: String
  def pid: Int
  def tid: Int
  def thread: String
  def fileName: String
  def lineNum: Int
  def function: String
  def messageTime: Instant
  def level: String   // TODO should be an enum?
  def message: String

  override def toString: String =
    s"$messageTime $hostName $application[$pid] ${level.head} $thread $fileName:$lineNum ($function) $message"
}
