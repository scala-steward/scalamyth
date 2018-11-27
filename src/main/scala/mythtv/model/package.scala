package mythtv

package object model {
  type Action = String
  type FrontendActionMap = Map[Action, String]
  type RecordedMarkupBytes = RecordedMarkup[VideoPositionBytes]
  type RecordedMarkupFrame = RecordedMarkup[VideoPositionFrame]
}
