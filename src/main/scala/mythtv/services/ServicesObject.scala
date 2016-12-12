package mythtv
package services

import util.MythDateTime

trait ServicesObject[+T] {
  def data: T
  def asOf: MythDateTime
  def mythVersion: String
  def mythProtocolVersion: String
}
