package mythtv
package model

object EnumTypes {
  type AudioProperties = model.AudioProperties.Base
  type CategoryType = model.CategoryType.Value
  type ChannelBrowseDirection = model.ChannelBrowseDirection.Value
  type ChannelChangeDirection = model.ChannelChangeDirection.Value
  type ChannelCommDetectMethod = model.ChannelCommDetectMethod.Value
  type DupCheckIn = model.DupCheckIn.Base
  type DupCheckMethod = model.DupCheckMethod.Base
  type FrontendState = model.FrontendState.Value
  type ImageFileTransform = model.ImageFileTransform.Value
  type JobFlags = model.JobFlags.Base
  type JobStatus = model.JobStatus.Value
  type JobType = model.JobType.Value
  type ListingSourceType = model.ListingSourceType.Value
  type LiveStreamStatus = model.LiveStreamStatus.Value
  type Markup = model.Markup.Value
  type MetadataGrabberType = model.MetadataGrabberType.Value
  type MusicImageType = model.MusicImageType.Value
  type MythLogLevel = model.MythLogLevel.Value
  type MythVerboseLevel = model.MythVerboseLevel.Base
  type NotificationPriority = model.NotificationPriority.Value
  type NotificationType = model.NotificationType.Value
  type NotificationVisibility = model.NotificationVisibility.Base
  type PictureAdjustType = model.PictureAdjustType.Value
  type ProgramFlags = model.ProgramFlags.Base
  type ProgramType = model.ProgramType.Value
  type RecSearchType = model.RecSearchType.Value
  type RecStatus = model.RecStatus.Value
  type RecType = model.RecType.Value
  type SleepStatus = model.SleepStatus.Value
  type SubtitleType = model.SubtitleType.Base
  type TvState = model.TvState.Value
  type VideoContentType = model.VideoContentType.Value
  type VideoProperties = model.VideoProperties.Base
}

object MetadataGrabberType extends Enumeration {
  type MetadataGrabberType = Value
  val Unknown    = Value
  val Movie      = Value
  val Television = Value
  val Music      = Value
  val Game       = Value
}
