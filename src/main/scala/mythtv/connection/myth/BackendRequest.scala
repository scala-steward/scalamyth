package mythtv
package connection
package myth

sealed trait BackendRequest {
  def command: String
  def args: Seq[Any]
  def serialized: String
}

object BackendRequest {
  def apply(command: String, args: Seq[Any], serialized: String): BackendRequest =
    Request(command, args, serialized)
}

private final case class Request(command: String, args: Seq[Any], serialized: String)
    extends BackendRequest
