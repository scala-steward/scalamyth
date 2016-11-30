package mythtv
package util

import java.net.{ URI, URISyntaxException }

private[mythtv] object URIFactory {
  private val emptyUri = new URI("")

  def empty: URI = emptyUri

  // FIXME UPSTREAM VideoService (all Services API?) does not properly escape URLs for artwork

  def apply(s: String): URI = {
    if (s.isEmpty) empty
    else {
      try new URI(s)
      catch {  // try a simple fix of whitespace escaping on syntax error
        case _: URISyntaxException => new URI(s.replace(" ", "%20"))
      }
    }
  }
}
