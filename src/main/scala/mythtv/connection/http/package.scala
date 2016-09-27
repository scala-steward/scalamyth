package mythtv
package connection

import java.util.{ List => JList, Map => JMap }

package object http {
  type HttpHeaders = Map[String, JList[String]]
}
