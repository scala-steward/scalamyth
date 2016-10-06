package mythtv
package connection
package myth

import java.util.regex.Pattern

trait MythProtocol extends MythProtocolLike {
  def PROTO_VERSION: Int
  def PROTO_TOKEN: String
}

object MythProtocol extends MythProtocolSerializer {
  final val BACKEND_SEP: String = "[]:[]"
  final val SPLIT_PATTERN: String = Pattern.quote(BACKEND_SEP)
}

//                                                          protocol version 63   // myth 0.24.x
//                                                          protocol version 72   // myth 0.25.x

private[myth] trait MythProtocol75 extends MythProtocol with MythProtocolLike75 { // myth 0.26.x
  final val PROTO_VERSION = 75
  final val PROTO_TOKEN = "SweetRock"
}

private[myth] trait MythProtocol77 extends MythProtocol with MythProtocolLike77 { // myth 0.27.x
  final val PROTO_VERSION = 77
  final val PROTO_TOKEN = "WindMark"
}

//                                                          protocol version 88   // myth 0.28.x
