package mythtv
package model

import util.LooseEnum

object CecOpcode extends LooseEnum {
  type CecOpcode = Value

  final val FeatureAbort                  = Value(0x00)
  final val ImageViewOn                   = Value(0x04)
  final val TunerStepIncrement            = Value(0x05)
  final val TunerStepDecrement            = Value(0x06)
  final val TunerDeviceStatus             = Value(0x07)
  final val GiveTunerDeviceStatus         = Value(0x08)
  final val RecordOn                      = Value(0x09)
  final val RecordStatus                  = Value(0x0A)
  final val RecordOff                     = Value(0x0B)
  final val TextViewOn                    = Value(0x0D)
  final val RecordTvScreen                = Value(0x0F)

  final val GiveDeckStatus                = Value(0x1A)
  final val DeckStatus                    = Value(0x1B)

  final val SetMenuLanguage               = Value(0x32)
  final val ClearAnalogueTimer            = Value(0x33)
  final val SetAnalogueTimer              = Value(0x34)
  final val TimerStatus                   = Value(0x35)
  final val Standby                       = Value(0x36)

  final val Play                          = Value(0x41)
  final val DeckControl                   = Value(0x42)
  final val TimerClearedStatus            = Value(0x43)
  final val UserControlPressed            = Value(0x44)
  final val UserControlRelease            = Value(0x45)
  final val GiveOsdName                   = Value(0x46)
  final val SetOsdName                    = Value(0x47)

  final val SetOsdString                  = Value(0x64)
  final val SetTimerProgramTitle          = Value(0x67)

  final val SystemAudioModeRequest        = Value(0x70)
  final val GiveAudioStatus               = Value(0x71)
  final val SetSystemAudioMode            = Value(0x72)
  final val ReportAudioStatus             = Value(0x7A)
  final val GiveSystemAudioModeStatus     = Value(0x7D)
  final val SystemAudioModeStatus         = Value(0x7E)

  final val RoutingChange                 = Value(0x80)
  final val RoutingInformation            = Value(0x81)
  final val ActiveSource                  = Value(0x82)
  final val GivePhysicalAddress           = Value(0x83)
  final val ReportPhysicalAddress         = Value(0x84)
  final val RequestActiveSource           = Value(0x85)
  final val SetStreamPath                 = Value(0x86)
  final val DeviceVendorId                = Value(0x87)
  final val VendorCommand                 = Value(0x89)
  final val VendorRemoteButtonDown        = Value(0x8A)
  final val VendorRemoteButtonUp          = Value(0x8B)
  final val GiveDeviceVendorId            = Value(0x8C)
  final val MenuRequest                   = Value(0x8D)
  final val MenuStatus                    = Value(0x8E)
  final val GiveDevicePowerStatus         = Value(0x8F)

  final val ReportPowerStatus             = Value(0x90)
  final val GetMenuLanguage               = Value(0x91)
  final val SelectAnalogueService         = Value(0x92)
  final val SelectDigitalService          = Value(0x93)
  final val SetDigitalTimer               = Value(0x97)
  final val ClearDigitalTimer             = Value(0x99)
  final val SetAudioRate                  = Value(0x9A)
  final val InactiveSource                = Value(0x9D)
  final val CecVersion                    = Value(0x9E)
  final val GetCecVersion                 = Value(0x9F)

  final val VendorCommandWithId           = Value(0xA0)
  final val ClearExternalTimer            = Value(0xA1)
  final val SetExternalTimer              = Value(0xA2)

  final val StartArc                      = Value(0xC0)
  final val ReportArcStarted              = Value(0xC1)
  final val ReportArcEnded                = Value(0xC2)
  final val RequestArcStart               = Value(0xC3)
  final val RequestArcEnd                 = Value(0xC4)
  final val EndArc                        = Value(0xC5)

  final val Cdc                           = Value(0xF8)
  final val None                          = Value(0xFD)
  final val Abort                         = Value(0xFF)
}
