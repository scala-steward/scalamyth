package mythtv
package connection
package myth

trait BackendResponse extends Any {
  def raw: String
  def split: Array[String] = raw split MythProtocol.SplitPattern
}

private[myth] object BackendResponse {
  def apply(r: String): BackendResponse = Response(r)
}

private final case class Response(raw: String) extends AnyVal with BackendResponse
