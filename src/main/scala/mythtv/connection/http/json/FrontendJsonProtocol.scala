package mythtv
package connection
package http
package json

import spray.json.{ JsonFormat, jsonWriter }
import spray.json.{ JsObject, JsValue }

import model.{ FrontendActionMap, FrontendState, FrontendStatus }

trait FrontendJsonProtocol extends CommonJsonProtocol {

  // ActionList is a Map[String, String] from actionName -> description
  implicit object FrontendActionMapJsonFormat extends JsonFormat[FrontendActionMap] {
    def write(a: FrontendActionMap): JsValue = JsObject(Map(
      "ActionList" -> jsonWriter[Map[String, String]].write(a.actions)
    ))

    def read(value: JsValue): FrontendActionMap = {
      val obj = value.asJsObject
      FrontendActionMap(obj.fields("ActionList").convertTo[Map[String, String]])
    }
  }

  implicit object FrontendStatusJsonFormat extends JsonFormat[FrontendStatus] {
    def write(s: FrontendStatus): JsValue = JsObject(Map(
      "State"          -> jsonWriter[Map[String, String]].write(s.stateMap),
      "AudioTracks"    -> jsonWriter[Map[String, String]].write(s.audioTracks),
      "SubtitleTracks" -> jsonWriter[Map[String, String]].write(s.subtitleTracks)
        // TODO ChapterTimes
    ))

    def read(value: JsValue): FrontendStatus = {
      val obj = value.asJsObject
      val states = obj.fields("State").convertTo[Map[String, String]]
      val audios = obj.fields("AudioTracks").convertTo[Map[String, String]]
      val subtitles = obj.fields("SubtitleTracks").convertTo[Map[String, String]]
      val stateVal = FrontendState.withName(states("state"))
      //val chapters = obj.fields("ChapterTimes").???

      new FrontendStatus {
        def state = stateVal
        def stateMap = states
        def audioTracks = audios
        def subtitleTracks = subtitles
        def chapterTimes = ???
      }
    }
  }

}
