package mythtv
package connection
package http
package json

import spray.json.{ JsObject, JsValue, JsonFormat, jsonWriter }
import spray.json.DefaultJsonProtocol.indexedSeqFormat

import model.{ FrontendActionMap, FrontendState, FrontendStatus }

private[json] trait FrontendJsonProtocol extends CommonJsonProtocol {
  implicit object FrontendStatusJsonFormat extends JsonFormat[FrontendStatus] {
    def write(s: FrontendStatus): JsValue = JsObject(Map(
      "State"          -> jsonWriter[Map[String, String]].write(s.stateMap),
      "AudioTracks"    -> jsonWriter[Map[String, String]].write(s.audioTracks),
      "SubtitleTracks" -> jsonWriter[Map[String, String]].write(s.subtitleTracks),
      "ChapterTimes"   -> jsonWriter[IndexedSeq[Long]].write(s.chapterTimes)
    ))

    def read(value: JsValue): FrontendStatus = {
      val obj = value.asJsObject
      val states = obj.fields("State").convertTo[Map[String, String]]
      val audios = obj.fields("AudioTracks").convertTo[Map[String, String]]
      val subtitles = obj.fields("SubtitleTracks").convertTo[Map[String, String]]
      val stateVal = FrontendState.withName(states("state"))
      val chapters = obj.fields("ChapterTimes").convertTo[IndexedSeq[Long]]

      new FrontendStatus {
        def state = stateVal
        def stateMap = states
        def audioTracks = audios
        def subtitleTracks = subtitles
        def chapterTimes = chapters
      }
    }
  }

}
