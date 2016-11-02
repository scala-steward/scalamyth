package mythtv
package connection
package http
package json

import services.Service

abstract class JsonService(conn: JsonConnection)
  extends Service
     with MythServiceProtocol
     with CommonJsonProtocol {

  def request(endpoint: String, params: Map[String, Any] = Map.empty): JsonResponse =
    conn.request(buildPath(endpoint, params))

  def post(endpoint: String, params: Map[String, Any] = Map.empty): JsonResponse =
    conn.post(buildPath(endpoint), params)

  def responseRoot(response: JsonResponse) =
    response.json.asJsObject

  def responseRoot(response: JsonResponse, fieldName: String) =
    response.json.asJsObject.fields(fieldName)
}
