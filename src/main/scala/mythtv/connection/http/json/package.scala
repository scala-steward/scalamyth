package mythtv
package connection
package http

import model.{ Channel, Program }

package object json {
  type GuideTuple = (Channel, Seq[Program])
}
