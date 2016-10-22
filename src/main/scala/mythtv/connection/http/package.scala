package mythtv
package connection

import java.util.{ List => JList }

package object http {
  type HttpHeaders = Map[String, JList[String]]
}
