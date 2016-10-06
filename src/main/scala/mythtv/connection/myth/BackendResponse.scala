package mythtv
package connection
package myth

sealed trait BackendResponse extends Any {
  def raw: String
  def split: Array[String]
}

object BackendResponse {
  def apply(r: String): BackendResponse = Response(r)
}

private final case class Response(raw: String) extends AnyVal with BackendResponse {
  def split: Array[String] = raw split MythProtocol.SPLIT_PATTERN
}
