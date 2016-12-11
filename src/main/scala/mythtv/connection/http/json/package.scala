package mythtv
package connection
package http

import model.{ Channel, ChannelDetails, Program, ProgramBrief }

package object json {
  type GuideTuple = (ChannelDetails, Seq[Program])
  type GuideBriefTuple = (Channel, Seq[ProgramBrief])
}
