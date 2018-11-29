package mythtv

package object model {
  type Action = String
  type FrontendActionMap = Map[Action, String]
  type RecordedMarkupBytes = RecordedMarkup[VideoPositionBytes]
  type RecordedMarkupFrame = RecordedMarkup[VideoPositionFrame]
  type RecordedMarkupMilliseconds = RecordedMarkup[VideoPositionMilliseconds]
  type RecordedSeekBytes = RecordedSeek[VideoPositionBytes]
  type RecordedSeekMilliseconds = RecordedSeek[VideoPositionMilliseconds]
  type VideoSegmentBytes = VideoSegment[VideoPositionBytes]
  type VideoSegmentFrames = VideoSegment[VideoPositionFrame]
  type VideoSegmentMilliseconds = VideoSegment[VideoPositionMilliseconds]
}
