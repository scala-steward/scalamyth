package mythtv
package model

import util.{ IntBitmaskEnum, LooseEnum }
import EnumTypes.FrontendState

trait FrontendStatus {
  def state: FrontendState
  def stateMap: Map[String, String]        // map of state item key -> value
  def audioTracks: Map[String, String]     // Action name -> descripton
  def subtitleTracks: Map[String, String]  //     "             "
  def chapterTimes: IndexedSeq[Long]       // Starting offset of each chapter, in seconds(?)

  override def toString: String = s"<FrontendStatus $state>"
}

// Note that this inherits many enumeration vals from AbstractTvStateEnum
object FrontendState extends AbstractTvStateEnum {
  type FrontendState = Value
  final val Idle    = Value("idle")
  final val Standby = Value("standby")
}

object NotificationType extends LooseEnum {
  type NotificationType = Value
  final val New     = Value
  final val Error   = Value
  final val Warning = Value
  final val Info    = Value
  final val Update  = Value
  final val Check   = Value
  final val Busy    = Value
}

object NotificationPriority extends LooseEnum {
  type NotificationPriority = Value
  final val Default  = Value(0)
  final val Low      = Value(1)
  final val Medium   = Value(2)
  final val High     = Value(3)
  final val Higher   = Value(4)
  final val Highest  = Value(5)
}

object NotificationVisibility extends IntBitmaskEnum {
  type NotificationVisibility = Base
  final val None       =  Mask(0)
  final val All        =  Mask(~None)
  final val Playback   = Value(0x01)
  final val Settings   = Value(0x02)
  final val Wizard     = Value(0x04)
  final val Videos     = Value(0x08)
  final val Music      = Value(0x10)
  final val Recordings = Value(0x20)
}

sealed trait ScreenshotFormat { def formatString: String }

object ScreenshotFormat {
  case object Jpg extends ScreenshotFormat { def formatString = "jpg" }
  case object Png extends ScreenshotFormat { def formatString = "png" }
}

object FrontendActions {
  ////////////////////////
  // from mythuiactions.h

  val Digit0: Action = "0"
  val Digit1: Action = "1"
  val Digit2: Action = "2"
  val Digit3: Action = "3"
  val Digit4: Action = "4"
  val Digit5: Action = "5"
  val Digit6: Action = "6"
  val Digit7: Action = "7"
  val Digit8: Action = "8"
  val Digit9: Action = "9"

  val Select: Action = "SELECT"
  val Up: Action     = "UP"
  val Down: Action   = "DOWN"
  val Left: Action   = "LEFT"
  val Right: Action  = "RIGHT"

  val HandleMedia: Action = "HANDLEMEDIA"
  val ScreenShot: Action  = "SCREENSHOT"
  val TvPowerOff: Action  = "TVPOWEROFF"
  val TvPowerOn: Action   = "TVPOWERON"

  ////////////////////////
  // from tv_actions.h

  val ExitShowNoPrompts: Action  = "EXITSHOWNOPROMPTS"

  val MenuCompact: Action        = "MENUCOMPACT"
  val Playback: Action           = "PLAYBACK"
  val Stop: Action               = "STOPPLAYBACK"
  val DayLeft: Action            = "DAYLEFT"
  val DayRight: Action           = "DAYRIGHT"
  val PageLeft: Action           = "PAGELEFT"
  val PageRight: Action          = "PAGERIGHT"
  val TogglePgOrder: Action      = "TOGGLEEPGORDER"
  val ClearOsd: Action           = "CLEAROSD"
  val Pause: Action              = "PAUSE"
  val ChannelUp: Action          = "CHANNELUP"
  val ChannelDown: Action        = "CHANNELDOWN"

  val ToggleRecord: Action       = "TOGGLERECORD"
  val ToggleFav: Action          = "TOGGLEFAV"
  val ToggleChanControls: Action = "TOGGLECHANCONTROLS"
  val ToggleRecControls: Action  = "TOGGLERECCONTROLS"

  val ListRecordedEpisodes: Action = "LISTRECORDEDEPISODES"

  val Guide: Action             = "GUIDE"
  val Finder: Action            = "FINDER"
  val ChannelSearch: Action     = "CHANNELSEARCH"
  val ToggleSleep: Action       = "TOGGLESLEEP"
  val Play: Action              = "PLAY"
  val ViewScheduled: Action     = "VIEWSCHEDULED"
  val PrevRecorded: Action      = "PREVRECORDED"
  val SignalMon: Action         = "SIGNALMON"

  /* Navigation */
  val JumpPrev: Action             = "JUMPPREV"
  val JumpRec: Action              = "JUMPREC"
  val SeekAbsolute: Action         = "SEEKABSOLUTE"
  val SeekArb: Action              = "ARBSEEK"
  val SeekRwnd: Action             = "SEEKRWND"
  val SeekFfwd: Action             = "SEEKFFWD"
  val JumpFfwd: Action             = "JUMPFFWD"
  val JumpRwnd: Action             = "JUMPRWND"
  val JumpBkmk: Action             = "JUMPBKMRK"
  val JumpStart: Action            = "JUMPSTART"
  val JumpToDvdRootMenu: Action    = "JUMPTODVDROOTMENU"
  val JumpToPopupMenu: Action      = "JUMPTOPOPUPMENU"
  val JumpToDvdChapterMenu: Action = "JUMPTODVDCHAPTERMENU"
  val JumpToDvdTitleMenu: Action   = "JUMPTODVDTITLEMENU"
  val JumpChapter: Action          = "JUMPTOCHAPTER"
  val SwitchTitle: Action          = "JUMPTOTITLE"
  val SwitchAngle: Action          = "SWITCHTOANGLE"

  /* Picture */
  val ToggleStudioLevels: Action   = "TOGGLESTUDIOLEVELS"
  val ToggleNightMode: Action      = "TOGGLENIGHTMODE"
  val SetBrightness: Action        = "SETBRIGHTNESS"
  val SetContrast: Action          = "SETCONTRAST"
  val SetColour: Action            = "SETCOLOUR"
  val SetHue: Action               = "SETHUE"

  /* Subtitles */
  val EnableSubs: Action           = "ENABLESUBS"
  val DisableSubs: Action          = "DISABLESUBS"
  val ToggleSubs: Action           = "TOGGLECC"
  val EnableForcedSubs: Action     = "ENABLEFORCEDSUBS"
  val DisableForcedSubs: Action    = "DISABLEFORCEDSUBS"
  val DisableExtText: Action       = "DISABLEEXTTEXT"
  val EnableExtText: Action        = "ENABLEEXTTEXT"
  val ToggleExtText: Action        = "TOGGLETEXT"
  val ToggleSubtitleZoom: Action   = "TOGGLESUBZOOM"
  val ToggleSubtitleDelay: Action  = "TOGGLESUBDELAY"

  /* Interactive Television keys */
  val MenuRed: Action    = "MENURED"
  val MenuGreen: Action  = "MENUGREEN"
  val MenuYellow: Action = "MENUYELLOW"
  val MenuBlue: Action   = "MENUBLUE"
  val TextExit: Action   = "TEXTEXIT"
  val MenuText: Action   = "MENUTEXT"
  val MenuEpg: Action    = "MENUEPG"

  /* Editing keys */
  val ClearMap: Action     = "CLEARMAP"
  val InvertMap: Action    = "INVERTMAP"
  val SaveMap: Action      = "SAVEMAP"
  val LoadCommSkip: Action = "LOADCOMMSKIP"
  val NextCut: Action      = "NEXTCUT"
  val PrevCut: Action      = "PREVCUT"
  val BigJumpRew: Action   = "BIGJUMPREW"
  val BigJumpFwd: Action   = "BIGJUMPFWD"

  /* Teletext keys */
  val NextPage: Action         = "NEXTPAGE"
  val PrevPage: Action         = "PREVPAGE"
  val NextSubPage: Action      = "NEXTSUBPAGE"
  val PrevSubPage: Action      = "PREVSUBPAGE"
  val ToggleTt: Action         = "TOGGLETT"
  val MenuWhite: Action        = "MENUWHITE"
  val ToggleBackground: Action = "TOGGLEBACKGROUND"
  val Reveal: Action           = "REVEAL"

  /* Audio */
  val MuteAudio: Action        = "MUTE"
  val ToggleUpmix: Action      = "TOGGLEUPMIX"
  val EnableUpmix: Action      = "ENABLEUPMIX"
  val DisableUpmix: Action     = "DISABLEUPMIX"
  val VolumeUp: Action         = "VOLUMEUP"
  val VolumeDown: Action       = "VOLUMEDOWN"
  val SetVolume: Action        = "SETVOLUME"
  val ToggleAudioSync: Action  = "TOGGLEAUDIOSYNC"
  val SetAudioSync: Action     = "SETAUDIOSYNC"

  /* Visualisations */
  val ToggleVisualisation: Action  = "TOGGLEVISUALISATION"
  val EnableVisualisation: Action  = "ENABLEVISUALISATION"
  val DisableVisualisation: Action = "DISABLEVISUALISATION"

  /* OSD playback information screen */
  val ToggleOsdDebug: Action = "DEBUGOSD"

  /* 3D TV */
  val ThreeDNone: Action                = "3DNONE"
  val ThreeDSideBySide: Action          = "3DSIDEBYSIDE"
  val ThreeDSideBySideDiscard: Action   = "3DSIDEBYSIDEDISCARD"
  val ThreeDTopAndBottom: Action        = "3DTOPANDBOTTOM"
  val ThreeDTopAndBottomDiscard: Action = "3DTOPANDBOTTOMDISCARD"

  /* Zoom mode */
  val ZoomUp: Action            = "ZOOMUP"
  val ZoomDown: Action          = "ZOOMDOWN"
  val ZoomLeft: Action          = "ZOOMLEFT"
  val ZoomRight: Action         = "ZOOMRIGHT"
  val ZoomAspectUp: Action      = "ZOOMASPECTUP"
  val ZoomAspectDown: Action    = "ZOOMASPECTDOWN"
  val ZoomIn: Action            = "ZOOMIN"
  val ZoomOut: Action           = "ZOOMOUT"
  val ZoomVerticalIn: Action    = "ZOOMVERTICALIN"
  val ZoomVerticalOut: Action   = "ZOOMVERTICALOUT"
  val ZoomHoritontalIn: Action  = "ZOOMHORIZONTALIN"
  val ZoomHorizontalOut: Action = "ZOOMHORIZONTALOUT"
  val ZoomQuit: Action          = "ZOOMQUIT"
  val ZoomCommit: Action        = "ZOOMCOMMIT"
}
