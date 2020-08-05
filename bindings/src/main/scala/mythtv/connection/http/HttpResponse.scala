// SPDX-License-Identifier: LGPL-2.1-only
/*
 * HttpResponse.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package connection
package http

import java.io.InputStream
import java.net.URL

sealed trait HttpRequestMethod {
  def name: String
  override def toString: String = name
}

object HttpRequestMethod {
  case object Get extends HttpRequestMethod { def name = "GET" }
  case object Post extends HttpRequestMethod { def name = "POST" }
}

trait HttpResponse {
  def statusCode: Int
  def headers: HttpHeaders
}

case class HttpStreamResponse(
  statusCode: Int,
  headers: HttpHeaders,
  stream: InputStream
) extends HttpResponse

case class HttpResponseException(
  url: URL,
  responseCode: Int
) extends RuntimeException(responseCode.toString + " response from " + url)
