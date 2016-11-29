package mythtv
package util

import java.net.URI

private[mythtv] object URIFactory {
  private val emptyUri = new URI("")

  def empty: URI = emptyUri

  def apply(s: String): URI = if (s.isEmpty) empty else new URI(s)
}
