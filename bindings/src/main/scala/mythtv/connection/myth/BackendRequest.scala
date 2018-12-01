// SPDX-License-Identifier: LGPL-2.1-only
/*
 * BackendRequest.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package myth

private[myth] sealed trait BackendRequest {
  def command: String
  def args: Seq[Any]
  def serialized: String
}

private[myth] object BackendRequest {
  def apply(command: String, args: Seq[Any], serialized: String): BackendRequest =
    Request(command, args, serialized)
}

private final case class Request(command: String, args: Seq[Any], serialized: String)
    extends BackendRequest
