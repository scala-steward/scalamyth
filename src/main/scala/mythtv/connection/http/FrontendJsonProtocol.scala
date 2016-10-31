package mythtv
package connection
package http

import spray.json.JsonFormat
import spray.json.{ JsObject, JsString, JsValue }


case class ActionMap(actions: Map[String, String])

// ActionList is a Map[String, String] from actionName -> description
//  (see SettingsJsonProtocol for another example of a Map[String, String])

trait FrontendJsonProtocol {

  implicit object ActionMapJsonFormat extends JsonFormat[ActionMap] {
    def write(a: ActionMap): JsValue = JsObject(Map(
      "ActionList" -> JsObject(a.actions.mapValues(JsString(_)))
    ))

    def read(value: JsValue): ActionMap = {
      val obj = value.asJsObject
      val fields = obj.fields("ActionList").asJsObject.fields
      ActionMap(fields mapValues {
        case JsString(s) => s
        case x => x.toString
      })
    }
  }

  /*
   Result of GetStatus:

   {
     "FrontendStatus": {
       "AudioTracks": {},
       "ChapterTimes": [],
       "State": {
         "currentlocation": "playbackbox",
         "menutheme": "mediacentermenu",
         "state": "idle"
       },
       "SubtitleTracks": {}
     }
   }

   Result of GetContextList:

   {
    "StringList": [
        "TV Frontend",
        "Game",
        "Global",
        "Music",
        "News",
        "TV Playback",
        "Video",
        "Gallery",
        "Weather",
        "JumpPoints",
        "Main Menu",
        "TV Editing",
        "Teletext Menu",
        "Browser"
    ]
   }

   Result of GetActionList:

{
    "FrontendActionList": {
        "ActionList": {
            "0": "0",
            "1": "1",
            "2": "2",
            "3": "3",
            "3DNONE": "No 3D",
            "3DSIDEBYSIDE": "3D Side by Side",
            "3DSIDEBYSIDEDISCARD": "Discard 3D Side by Side",
            "ZoneMinder Console": "ZoneMinder Console",
            "ZoneMinder Events": "ZoneMinder Events",
            "ZoneMinder Live View": "ZoneMinder Live View"
        }
    }
}

   */
}
