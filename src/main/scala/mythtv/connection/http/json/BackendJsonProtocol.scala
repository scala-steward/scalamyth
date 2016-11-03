package mythtv
package connection
package http
package json

import java.time.{ Duration, Instant, LocalTime, Year, ZoneOffset }

import scala.util.DynamicVariable

import spray.json.{ DefaultJsonProtocol, JsonFormat, jsonWriter }
import spray.json.{ JsArray, JsObject, JsString, JsValue }

import util.{ DecimalByteCount, MythDateTime, MythFileHash }
import services.PagedList
import model.EnumTypes._
import model._

// TODO artwork info on recordings?

/* ----------------------------------------------------------------- */

// TODO Pull out all but `items` into a separate trait and share with guide result?
private[http] trait MythJsonObjectList[+T] {
  def items: List[T]
  def asOf: MythDateTime
  def mythVersion: String
  def mythProtocolVersion: String
}

private[http] abstract class MythJsonPagedObjectList[+T]
  extends PagedList[T] with MythJsonObjectList[T] {
  def count: Int
  def totalAvailable: Int
  def startIndex: Int
}

/* ----------------------------------------------------------------- */

private[http] trait MythJsonObjectListFormat[T]
  extends BaseMythJsonListFormat[T]
     with MythJsonObjectFormat[MythJsonObjectList[T]] {
  import RichJsonObject._

  def write(obj: MythJsonObjectList[T]): JsValue = ???

  def read(value: JsValue): MythJsonObjectList[T] = {
    val obj = value.asJsObject
    val itemList = readItems(obj)
    new MythJsonObjectList[T] {
      def items = itemList
      def asOf = obj.dateTimeField("AsOf")
      def mythVersion = obj.stringField("Version")
      def mythProtocolVersion = obj.stringField("ProtoVer")
    }
  }
}

/* Top level object will contain a field for the list,
 *    e.g. RecRuleList or ProgramList, etc.
 *
 *  This List object will then contain fields:
 *
 "AsOf": "2016-10-23T06:06:17Z",
 "Count": "171",
 "StartIndex": "0",
 "TotalAvailable": "171",
 "ProtoVer": "77",
 "Version": "0.27.20140323-1"
 *
 *  plus a field for the objects, e.g.
 *
 "Programs": [ ... ]
 *
 *
 * Not all of the "*List" objects follow, this pattern.
 * Exceptions include:
 *    StringList, StorageGroupDirList, ...
 */
private[http] trait MythJsonPagedObjectListFormat[T]
  extends BaseMythJsonListFormat[T]
     with MythJsonObjectFormat[MythJsonPagedObjectList[T]] {
  import RichJsonObject._

  def write(obj: MythJsonPagedObjectList[T]): JsValue = ???

  def read(value: JsValue): MythJsonPagedObjectList[T] = {
    val obj = value.asJsObject
    val itemList = readItems(obj)

    new MythJsonPagedObjectList[T] {
      def items = itemList
      def count = obj.intField("Count")
      def totalAvailable = obj.intField("TotalAvailable")
      def startIndex = obj.intField("StartIndex")
      def asOf = obj.dateTimeField("AsOf")
      def mythVersion = obj.stringField("Version")
      def mythProtocolVersion = obj.stringField("ProtoVer")
    }
  }
}

// TODO FIXME ineffecient, maps rebuilt on each operation
private[http] trait EnumDescriptionFormat[T] extends JsonFormat[T] {
  def id2Description: Map[T, String]
  def description2Id: Map[String, T] = id2Description map (_.swap)
  def write(p: T): JsValue = JsString(id2Description(p))
  def read(value: JsValue): T = value match {
    case JsString(s) => description2Id(s)
    case x => description2Id(x.toString)
  }
}

/* Inheriting from DefaultJsonProtocol can cause huge bytecode bloat */
private[http] trait BackendJsonProtocol extends CommonJsonProtocol {

  // NB StorageGroupDirList has a single field StorageGroupDirs which is an array of StorageGroupDir items

  import RichJsonObject._

  private val channelContext = new DynamicVariable[RichJsonObject](EmptyJsonObject)

  implicit object RecSearchTypeJsonFormat extends EnumDescriptionFormat[RecSearchType] {
    val id2Description: Map[RecSearchType, String] = Map(
      RecSearchType.NoSearch      -> "None",
      RecSearchType.PowerSearch   -> "Power Search",
      RecSearchType.TitleSearch   -> "Title Search",
      RecSearchType.KeywordSearch -> "Keyword Search",
      RecSearchType.PeopleSearch  -> "People Search",
      RecSearchType.ManualSearch  -> "Manual Search"
    )
  }

  // TODO this one is tricker because the map has duplicate keys!
  //   (see libs/libmyth/recordingtypes.cpp: toRawString(RecordingType)
  // This also means there is a loss of precision between a record rule
  // RecType and how it is described in the services API representation.
  // TODO this mapping changed between Myth versions (0.26 -> 0.27?)
  implicit object RecTypeJsonFormat extends EnumDescriptionFormat[RecType] {
    val id2Description: Map[RecType, String] = Map(
      RecType.NotRecording   -> "Not Recording",
      RecType.SingleRecord   -> "Single Record",
      RecType.AllRecord      -> "Record All",
      RecType.FindOneRecord  -> "Record One",
      RecType.TimeslotRecord -> "Record Daily",
      RecType.WeekslotRecord -> "Record Weekly",
      RecType.OverrideRecord -> "Override Recording"
    )
  }

  implicit object DupCheckInJsonFormat extends EnumDescriptionFormat[DupCheckIn] {
    val id2Description: Map[DupCheckIn, String] = Map(
      DupCheckIn.Recorded    -> "Current Recordings",
      DupCheckIn.OldRecorded -> "Previous Recordings",
      DupCheckIn.All         -> "All Recordings",
      DupCheckIn.NewEpisodes -> "New Episodes Only"
    )
  }

  implicit object DupCheckMethodJsonFormat extends EnumDescriptionFormat[DupCheckMethod] {
    val id2Description: Map[DupCheckMethod, String] = Map(
      DupCheckMethod.None             -> "None",
      DupCheckMethod.Subtitle         -> "Subtitle",
      DupCheckMethod.Description      -> "Description",
      DupCheckMethod.SubtitleDesc     -> "Subtitle and Description",
      DupCheckMethod.SubtitleThenDesc -> "Subtitle then Description"
    )
  }

  implicit object RecordingJsonFormat extends MythJsonObjectFormat[Recording] {
    def objectFieldName = "Program"

    def write(r: Recording): JsValue = {
      val rmap: Map[String, JsValue] = RecordableJsonFormat.write(r) match {
        case JsObject(fields) => fields
        case _ => Map.empty  // TODO should probably throw an exception here
      }
      JsObject(rmap ++ Map(
        "FileName" -> JsString(r.filename),
        "FileSize" -> JsString(r.filesize.bytes.toString),
        "Season"   -> JsString(r.season.toString),
        "Episode"  -> JsString(r.episode.toString),
        "Inetref"  -> JsString(r.inetRef)
      ))
    }

    def read(value: JsValue): Recording = {
      val obj = value.asJsObject

      val channel: RichJsonObject =  // inner object
        if (obj.fields contains "Channel") obj.fields("Channel").asJsObject
        else channelContext.value

      val rec: RichJsonObject =      // inner object
        if (obj.fields contains "Recording") obj.fields("Recording").asJsObject
        else EmptyJsonObject

      new Recording {
        override def toString: String = s"<JsonRecording $chanId, $startTime: $title>"

        def title                   = obj.stringField("Title")
        def subtitle                = obj.stringField("SubTitle")
        def description             = obj.stringField("Description")
        def syndicatedEpisodeNumber = "" //???
        def category                = obj.stringField("Category")
        def categoryType            = obj.stringFieldOption("CatType", "") map CategoryType.withName
        def chanId                  = ChanId(channel.intFieldOrElse("ChanId", 0))
        def startTime               = obj.dateTimeField("StartTime")
        def endTime                 = obj.dateTimeField("EndTime")
        def seriesId                = obj.stringField("SeriesId")
        def programId               = obj.stringField("ProgramId")
        def stars                   = obj.doubleFieldOption("Stars", 0)
        def originalAirDate         = obj.dateFieldOption("Airdate")
        def audioProps              = AudioProperties(obj.intField("AudioProps"))
        def videoProps              = VideoProperties(obj.intField("VideoProps"))
        def subtitleType            = SubtitleType(obj.intField("SubProps"))
        def year                    = obj.intFieldOption("Year") map Year.of  // TODO year field does not exist
        def partNumber              = None
        def partTotal               = None
        def programFlags            = ProgramFlags(obj.intField("ProgramFlags"))

        def findId                  = 0 // ???
        def hostname                = obj.stringField("HostName")
        def sourceId                = ListingSourceId(channel.intFieldOrElse("SourceId", 0))
        def cardId                  = CaptureCardId(rec.intField("EncoderId"))
        def inputId                 = InputId(channel.intFieldOrElse("InputId", 0))
        def recPriority             = rec.intField("Priority")
        def recStatus               = RecStatus.applyOrUnknown(rec.intField("Status"))
        def recordId                = RecordRuleId(rec.intField("RecordId"))
        def recType                 = RecType.applyOrUnknown(rec.intField("RecType"))
        def dupIn                   = DupCheckIn(rec.intField("DupInType"))
        def dupMethod               = DupCheckMethod(rec.intField("DupMethod"))
        def recStartTS              = rec.dateTimeField("StartTs")
        def recEndTS                = rec.dateTimeField("EndTs")
        def recGroup                = rec.stringField("RecGroup")
        def storageGroup            = rec.stringField("StorageGroup")
        def playGroup               = rec.stringField("PlayGroup")
        def recPriority2            = 0 //???
        def parentId                = 0 //???
        def lastModified            = obj.dateTimeField("LastModified")
        def chanNum                 = ChannelNumber(channel.stringFieldOrElse("ChanNum", ""))
        def callsign                = channel.stringFieldOrElse("CallSign", "")
        def chanName                = channel.stringFieldOrElse("ChannelName", "")
        def outputFilters           = channel.stringFieldOrElse("ChanFilters", "")

        def filename                = obj.stringField("FileName")
        def filesize                = DecimalByteCount(obj.longField("FileSize"))
        def season                  = obj.intFieldOrElse("Season", 0)
        def episode                 = obj.intFieldOrElse("Episode", 0)
        def inetRef                 = obj.stringField("Inetref")
      }
    }
  }

  implicit object RecordableJsonFormat extends MythJsonObjectFormat[Recordable] {
    def objectFieldName = "Program"

    def write(r: Recordable): JsValue = {
      val pmap: Map[String, JsValue] = ProgramJsonFormat.write(r) match {
        case JsObject(fields) => fields
        case _ => Map.empty  // TODO should probably throw an exception here
      }
      JsObject(pmap ++ Map(
        // TODO inner Channel object overrides: SourceId, InputId, ChanNum, CallSign, ChannelName
        "HostName"     -> JsString(r.hostname),
        "LastModified" -> JsString(r.lastModified.toString),
        "Recording"    -> JsObject(Map(
          "EncoderId"    -> JsString(r.cardId.id.toString),
          "Priority"     -> JsString(r.recPriority.toString),
          "Status"       -> JsString(r.recStatus.id.toString),
          "RecordId"     -> JsString(r.recordId.id.toString),
          "RecType"      -> JsString(r.recType.id.toString),
          "DupInType"    -> JsString(r.dupIn.id.toString),
          "DupMethod"    -> JsString(r.dupMethod.id.toString),
          "StartTs"      -> JsString(r.recStartTS.toString),
          "EndTs"        -> JsString(r.recEndTS.toString),
          "RecGroup"     -> JsString(r.recGroup),
          "PlayGroup"    -> JsString(r.playGroup),
          "StorageGroup" -> JsString(r.storageGroup),
          "Profile"      -> JsString("Default")  // TODO not in Recordable/Recording
        ))
      ))
    }

    def read(value: JsValue): Recordable = {
      val obj = value.asJsObject

      val channel: RichJsonObject =  // inner object
        if (obj.fields contains "Channel") obj.fields("Channel").asJsObject
        else channelContext.value

      val rec: RichJsonObject =      // inner object
        if (obj.fields contains "Recording") obj.fields("Recording").asJsObject
        else EmptyJsonObject

      new Recordable {
        override def toString: String = s"<JsonRecordable $chanId, $startTime: $title>"

        def title                   = obj.stringField("Title")
        def subtitle                = obj.stringField("SubTitle")
        def description             = obj.stringField("Description")
        def syndicatedEpisodeNumber = ???
        def category                = obj.stringField("Category")
        def categoryType            = obj.stringFieldOption("CatType", "") map CategoryType.withName
        def chanId                  = ChanId(channel.intFieldOrElse("ChanId", 0))
        def startTime               = obj.dateTimeField("StartTime")
        def endTime                 = obj.dateTimeField("EndTime")
        def seriesId                = obj.stringField("SeriesId")
        def programId               = obj.stringField("ProgramId")
        def stars                   = obj.doubleFieldOption("Stars", 0)
        def originalAirDate         = obj.dateFieldOption("Airdate")
        def audioProps              = AudioProperties(obj.intField("AudioProps"))
        def videoProps              = VideoProperties(obj.intField("VideoProps"))
        def subtitleType            = SubtitleType(obj.intField("SubProps"))
        def year                    = obj.intFieldOption("Year") map Year.of  // TODO year field does not exist
        def partNumber              = None
        def partTotal               = None
        def programFlags            = ProgramFlags(obj.intField("ProgramFlags"))

        def findId                  = ???
        def hostname                = obj.stringField("HostName")
        def sourceId                = ListingSourceId(channel.intFieldOrElse("SourceId", 0))
        def cardId                  = CaptureCardId(rec.intField("EncoderId"))
        def inputId                 = InputId(channel.intFieldOrElse("InputId", 0))
        def recPriority             = rec.intField("Priority")
        def recStatus               = RecStatus.applyOrUnknown(rec.intField("Status"))
        def recordId                = RecordRuleId(rec.intField("RecordId"))
        def recType                 = RecType.applyOrUnknown(rec.intField("RecType"))
        def dupIn                   = DupCheckIn(rec.intField("DupInType"))
        def dupMethod               = DupCheckMethod(rec.intField("DupMethod"))
        def recStartTS              = rec.dateTimeField("StartTs")
        def recEndTS                = rec.dateTimeField("EndTs")
        def recGroup                = rec.stringField("RecGroup")
        def storageGroup            = rec.stringField("StorageGroup")
        def playGroup               = rec.stringField("PlayGroup")
        def recPriority2            = ???
        def parentId                = ???
        def lastModified            = obj.dateTimeField("LastModified")
        def chanNum                 = ChannelNumber(channel.stringFieldOrElse("ChanNum", ""))
        def callsign                = channel.stringFieldOrElse("CallSign", "")
        def chanName                = channel.stringFieldOrElse("ChannelName", "")
        def outputFilters           = channel.stringFieldOrElse("ChanFilters", "")
      }
    }
  }

  implicit object ProgramJsonFormat extends MythJsonObjectFormat[Program] {
    def objectFieldName = "Program"

    def write(p: Program): JsValue = JsObject(Map(
      // TODO nested Channel object
      // TODO nested Recording object
      // TODO nested Artwork object
      "Title"        -> JsString(p.title),
      "SubTitle"     -> JsString(p.subtitle),
      "Description"  -> JsString(p.description),
      "Category"     -> JsString(p.category),
      "CatType"      -> JsString(p.categoryType.map(_.toString).getOrElse("")),
      "StartTime"    -> JsString(p.startTime.toString),
      "EndTime"      -> JsString(p.endTime.toString),
      "SeriesId"     -> JsString(p.seriesId),
      "ProgramId"    -> JsString(p.programId),
      "Stars"        -> JsString(p.stars.getOrElse(0).toString),
      "Airdate"      -> JsString(p.originalAirDate.map(_.toString).getOrElse("")),
      "AudioProps"   -> JsString(p.audioProps.id.toString),
      "VideoProps"   -> JsString(p.videoProps.id.toString),
      "SubProps"     -> JsString(p.subtitleType.id.toString),
      "ProgramFlags" -> JsString(p.programFlags.id.toString),
      "Repeat"       -> JsString(p.isRepeat.toString),
      "FileSize"     -> JsString("0"),   // does not exist in Program
      "FileName"     -> JsString(""),    // does not exist in Program
      "HostName"     -> JsString(""),    // does not exist in Program
      "LastModified" -> JsString(""),    // does not exist in Program
      "Inetref"      -> JsString(""),    // does not exist in Program
      "Season"       -> JsString("0"),   // does not exist in Program
      "Episode"      -> JsString("0")    // does not exist in Program
    ))

    def read(value: JsValue): Program = {
      val obj = value.asJsObject

      // We probably don't care too much about this other than
      // snagging the chanId; maybe callsign, channum, channame
      val channel: RichJsonObject =  // inner object
        if (obj.fields contains "Channel") obj.fields("Channel").asJsObject
        else channelContext.value

      val rec: RichJsonObject =      // inner object
        if (obj.fields contains "Recording") obj.fields("Recording").asJsObject
        else EmptyJsonObject

      if (obj.fields contains "Artwork") {     // inner object
        /*
            "ArtworkInfos": []
         */
      }

      // TODO Year field does not exist separately, but it the "Airdate" field may sometimes only
      //      contain a year, in which case originalAirDate should be None....

      // Return a Recording if there is a non-empty recording start time AND a non-empty filename
      if (rec.stringFieldOption("StartTs", "").nonEmpty) {
        // Generate a Recordable if we have a recording StartTS but not FileName
        if (obj.stringFieldOption("FileName", "").nonEmpty) RecordingJsonFormat.read(value)
        else                                                RecordableJsonFormat.read(value)
        // Recordable/Recording fields missing
        // findId
        // recpriority2
        // parentId
      }
      else new Program {
        override def toString: String = s"<JsonProgram $chanId, $startTime: $title>"

        def title                   = obj.stringField("Title")
        def subtitle                = obj.stringField("SubTitle")
        def description             = obj.stringField("Description")
        def syndicatedEpisodeNumber = ???
        def category                = obj.stringField("Category")
        def categoryType            = obj.stringFieldOption("CatType", "") map CategoryType.withName
        def chanId                  = ChanId(channel.intFieldOrElse("ChanId", 0))
        def startTime               = obj.dateTimeField("StartTime")
        def endTime                 = obj.dateTimeField("EndTime")
        def seriesId                = obj.stringField("SeriesId")
        def programId               = obj.stringField("ProgramId")
        def stars                   = obj.doubleFieldOption("Stars", 0)
        def originalAirDate         = obj.dateFieldOption("Airdate")
        def audioProps              = AudioProperties(obj.intField("AudioProps"))
        def videoProps              = VideoProperties(obj.intField("VideoProps"))
        def subtitleType            = SubtitleType(obj.intField("SubProps"))
        def year                    = obj.intFieldOption("Year") map Year.of    // TODO year field does not exist
        def partNumber              = None
        def partTotal               = None
        def programFlags            = ProgramFlags(obj.intField("ProgramFlags"))
      }

      /* missing:
         syndicatedEpisodeNumber   (but we do have "Season" and "Episode")
         partNumber
         partTotal */
    }

  }

  implicit object PagedProgramListJsonFormat extends MythJsonPagedObjectListFormat[Program] {
    def objectFieldName = "ProgramList"
    def listFieldName = "Programs"
    def convertElement(value: JsValue): Program = value.convertTo[Program]
    def elementToJson(elem: Program): JsValue = jsonWriter[Program].write(elem)
  }

  implicit object PagedRecordableListJsonFormat extends MythJsonPagedObjectListFormat[Recordable] {
    def objectFieldName = "ProgramList"
    def listFieldName = "Programs"
    def convertElement(value: JsValue): Recordable = value.convertTo[Recordable]
    def elementToJson(elem: Recordable): JsValue = jsonWriter[Recordable].write(elem)
  }

  implicit object PagedRecordingListJsonFormat extends MythJsonPagedObjectListFormat[Recording] {
    def objectFieldName = "ProgramList"
    def listFieldName = "Programs"
    def convertElement(value: JsValue): Recording = value.convertTo[Recording]
    def elementToJson(elem: Recording): JsValue = jsonWriter[Recording].write(elem)
  }

  // used for reading Guide
  implicit object ProgramListJsonFormat extends MythJsonListFormat[Program] {
    def objectFieldName = "ChannelInfo"
    def listFieldName = "Programs"
    def convertElement(value: JsValue): Program = value.convertTo[Program]
    def elementToJson(elem: Program): JsValue = jsonWriter[Program].write(elem)
  }

  implicit object ChannelDetailsJsonFormat extends MythJsonObjectFormat[ChannelDetails] {
    def objectFieldName = "ChannelInfo"  // or "Channel"

    def write(c: ChannelDetails): JsValue = JsObject(Map(
      "ChanId"           -> JsString(c.chanId.id.toString),
      "ChannelName"      -> JsString(c.name),
      "ChanNum"          -> JsString(c.number.num),
      "CallSign"         -> JsString(c.callsign),
      "SourceId"         -> JsString(c.sourceId.toString),
      "FrequencyId"      -> JsString(c.freqId.getOrElse("")),
      "IconURL"          -> JsString(c.iconPath),
      "FineTune"         -> JsString(c.fineTune.getOrElse(0).toString),
      "XMLTVID"          -> JsString(c.xmltvId),
      "Format"           -> JsString(c.format),
      "Visible"          -> JsString(c.visible.toString),
      "ChanFilters"      -> JsString(c.outputFilters.getOrElse("")),
      "UseEIT"           -> JsString(c.useOnAirGuide.toString),
      "MplexId"          -> JsString(c.mplexId.map(_.id).getOrElse(0).toString),
      "ServiceId"        -> JsString(c.serviceId.getOrElse(0).toString),
      "ATSCMajorChan"    -> JsString(c.atscMajorChan.getOrElse(0).toString),
      "ATSCMinorChan"    -> JsString(c.atscMinorChan.getOrElse(0).toString),
      "DefaultAuthority" -> JsString(c.defaultAuthority.getOrElse("")),
      "CommFree"         -> JsString(if (c.isCommercialFree) "1" else "0'")
    ))

    def read(value: JsValue): ChannelDetails = {
      val obj = value.asJsObject
      new ChannelDetails {
        def chanId           = ChanId(obj.intField("ChanId"))
        def name             = obj.stringField("ChannelName")
        def number           = ChannelNumber(obj.stringField("ChanNum"))
        def callsign         = obj.stringField("CallSign")
        def sourceId         = ListingSourceId(obj.intField("SourceId"))
        def freqId           = obj.stringFieldOption("FrequencyId", "")
        def iconPath         = obj.stringField("IconURL")
        def fineTune         = obj.intFieldOption("FineTune", 0)
        def xmltvId          = obj.stringField("XMLTVID")
        def format           = obj.stringField("Format")
        def visible          = obj.booleanField("Visible")
        def outputFilters    = obj.stringFieldOption("ChanFilters", "")
        def useOnAirGuide    = obj.booleanField("UseEIT")
        def mplexId          = obj.intFieldOption("MplexId", 0) map MultiplexId
        def serviceId        = obj.intFieldOption("ServiceId", 0)
        def atscMajorChan    = obj.intFieldOption("ATSCMajorChan", 0)
        def atscMinorChan    = obj.intFieldOption("ATSCMinorChan", 0)
        def defaultAuthority = obj.stringFieldOption("DefaultAuth", "")
        def commMethod       = if (obj.intFieldOrElse("CommFree", 0) != 0) ChannelCommDetectMethod.CommFree
                               else ChannelCommDetectMethod.Uninitialized
      }
    }
  }

  implicit object ChannelDetailsListJsonFormat extends MythJsonPagedObjectListFormat[ChannelDetails] {
    def objectFieldName = "ChannelInfoList"
    def listFieldName = "ChannelInfos"
    def convertElement(value: JsValue): ChannelDetails = value.convertTo[ChannelDetails]
    def elementToJson(elem: ChannelDetails): JsValue = jsonWriter[ChannelDetails].write(elem)
  }

  implicit object RecordRuleJsonFormat extends MythJsonObjectFormat[RecordRule] {
    def objectFieldName = "RecRule"

    def write(r: RecordRule): JsValue = JsObject(Map(
      "Id"                -> JsString(r.id.id.toString),
      "Type"              -> jsonWriter[RecType].write(r.recType),
      "ChanId"            -> JsString(r.chanId.map(_.id).getOrElse(0).toString),
      "StartTime"         -> JsString(r.startTime.toString),
      "EndTime"           -> JsString(r.endTime.toString),
      "Title"             -> JsString(r.title),
      "SubTitle"          -> JsString(r.subtitle),
      "Description"       -> JsString(r.description),
      "Season"            -> JsString(r.season.getOrElse(0).toString),
      "Episode"           -> JsString(r.episode.getOrElse(0).toString),
      "Category"          -> JsString(r.category),
      "RecProfile"        -> JsString(r.recProfile),
      "RecPriority"       -> JsString(r.recPriority.toString),
      "AutoExpire"        -> JsString(r.autoExpire.toString),
      "MaxEpisodes"       -> JsString(r.maxEpisodes.toString),
      "MaxNewest"         -> JsString(r.maxNewest.toString),
      "StartOffset"       -> JsString(r.startOffset.toString),
      "EndOffset"         -> JsString(r.endOffset.toString),
      "RecGroup"          -> JsString(r.recGroup),
      "DupMethod"         -> jsonWriter[DupCheckMethod].write(r.dupMethod),
      "DupIn"             -> jsonWriter[DupCheckIn].write(r.dupIn),
      "CallSign"          -> JsString(r.callsign),
      "SeriesId"          -> JsString(r.seriesId.getOrElse("")),
      "ProgramId"         -> JsString(r.programId.getOrElse("")),
      "Inetref"           -> JsString(r.inetRef.getOrElse("")),
      "SearchType"        -> jsonWriter[RecSearchType].write(r.searchType),
      "AutoTranscode"     -> JsString(r.autoTranscode.toString),
      "Autocommflag"      -> JsString(r.autoCommFlag.toString),
      "AutoUserJob1"      -> JsString(r.autoUserJob1.toString),
      "AutoUserJob2"      -> JsString(r.autoUserJob2.toString),
      "AutoUserJob3"      -> JsString(r.autoUserJob3.toString),
      "AutoUserJob4"      -> JsString(r.autoUserJob4.toString),
      "AutoMetaLookup"    -> JsString(r.autoMetadata.toString),
      "FindDay"           -> JsString(r.findDay.toString),
      "FindTime"          -> JsString(r.findTime.getOrElse(LocalTime.MIN).toString),
      "Inactive"          -> JsString(r.inactive.toString),
      "ParentId"          -> JsString(r.parentId.map(_.id).getOrElse(0).toString),
      "Transcoder"        -> JsString(r.transcoder.getOrElse(0).toString),
      "PlayGroup"         -> JsString(r.playGroup),
      "PreferredInput"    -> JsString(r.preferredInput.map(_.id).getOrElse(0).toString),
      "NextRecording"     -> JsString(r.nextRecord.map(_.toString).getOrElse("")),
      "LastRecorded"      -> JsString(r.lastRecord.map(_.toString).getOrElse("")),
      "LastDeleted"       -> JsString(r.lastDelete.map(_.toString).getOrElse("")),
      "StorageGroup"      -> JsString(r.storageGroup),
      "AverageDelay"      -> JsString(r.averageDelay.toString),
      "Filter"            -> JsString(r.filter.getOrElse(0).toString)
    ))

    def read(value: JsValue): RecordRule = {
      /*
       "AutoCommflag": "false",
       "AutoExpire": "true",
       "AutoMetaLookup": "true",
       "AutoTranscode": "false",
       "AutoUserJob1": "false",
       "AutoUserJob2": "false",
       "AutoUserJob3": "false",
       "AutoUserJob4": "false",
       "AverageDelay": "0",
       "CallSign": "KPBS-HD",
       "Category": "House/garden",
       "ChanId": "1151",
       "Description": "Host Nan Sterman takes us ...",
       "DupIn": "All Recordings",
       "DupMethod": "Subtitle and Description",
       "EndOffset": "0",
       "EndTime": "2013-06-28T04:00:00Z",
       "Episode": "0",
       "Filter": "0",
       "FindDay": "0",
       "FindTime": "00:00:00",
       "Id": "349",
       "Inactive": "false",
       "Inetref": "",
       "LastDeleted": "2014-02-07T17:22:46Z",
       "LastRecorded": "2016-10-23T12:30:00Z",
       "MaxEpisodes": "0",
       "MaxNewest": "false",
       "NextRecording": "2016-10-30T12:30:00Z",
       "ParentId": "0",
       "PlayGroup": "Default",
       "PreferredInput": "0",
       "ProgramId": "EP007970840003",
       "RecGroup": "Gardening",
       "RecPriority": "0",
       "RecProfile": "Default",
       "SearchType": "None",
       "Season": "0",
       "SeriesId": "EP00797084",
       "StartOffset": "0",
       "StartTime": "2013-06-28T03:30:00Z",
       "StorageGroup": "Default",
       "SubTitle": "Waterwise and Wonderful",
       "Title": "A Growing Passion",
       "Transcoder": "0",
       "Type": "Record All"
       */
      val obj = value.asJsObject

      new RecordRule {
        def id              = RecordRuleId(obj.intField("Id"))
        def recType         = obj.fields("Type").convertTo[RecType]
        def chanId          = obj.intFieldOption("ChanId", 0) map (ChanId(_))
        def startTime       = obj.dateTimeField("StartTime")
        def endTime         = obj.dateTimeField("EndTime")
        def title           = obj.stringField("Title")
        def subtitle        = obj.stringField("SubTitle")
        def description     = obj.stringField("Description")
        def season          = obj.intFieldOption("Season", 0)
        def episode         = obj.intFieldOption("Episode", 0)
        def category        = obj.stringField("Category")
        def recProfile      = obj.stringField("RecProfile")
        def recPriority     = obj.intField("RecPriority")
        def autoExpire      = obj.booleanField("AutoExpire")
        def maxEpisodes     = obj.intField("MaxEpisodes")
        def maxNewest       = obj.booleanField("MaxNewest")
        def startOffset     = obj.intField("StartOffset")
        def endOffset       = obj.intField("EndOffset")
        def recGroup        = obj.stringField("RecGroup")
        def dupMethod       = obj.fields("DupMethod").convertTo[DupCheckMethod]
        def dupIn           = obj.fields("DupIn").convertTo[DupCheckIn]
        def callsign        = obj.stringField("CallSign")
        def seriesId        = obj.stringFieldOption("SeriesId", "")
        def programId       = obj.stringFieldOption("ProgramId", "")
        def inetRef         = obj.stringFieldOption("Inetref", "")
        def searchType      = obj.fields("SearchType").convertTo[RecSearchType]
        def autoTranscode   = obj.booleanField("AutoTranscode")
        def autoCommFlag    = obj.booleanField("AutoCommflag")
        def autoUserJob1    = obj.booleanField("AutoUserJob1")
        def autoUserJob2    = obj.booleanField("AutoUserJob2")
        def autoUserJob3    = obj.booleanField("AutoUserJob3")
        def autoUserJob4    = obj.booleanField("AutoUserJob4")
        def autoMetadata    = obj.booleanField("AutoMetaLookup")
        def findDay         = obj.intField("FindDay")
        def findTime        = obj.timeFieldOption("FindTime", LocalTime.MIN)
        def inactive        = obj.booleanField("Inactive")
        def parentId        = obj.intFieldOption("ParentId", 0) map RecordRuleId
        def transcoder      = obj.intFieldOption("Transcoder", 0)
        def playGroup       = obj.stringField("PlayGroup")
        def preferredInput  = obj.intFieldOption("PreferredInput", 0) map InputId
        def nextRecord      = obj.dateTimeFieldOption("NextRecording")
        def lastRecord      = obj.dateTimeFieldOption("LastRecorded")
        def lastDelete      = obj.dateTimeFieldOption("LastDeleted")
        def storageGroup    = obj.stringField("StorageGroup")
        def averageDelay    = obj.intField("AverageDelay")
        def filter          = obj.intFieldOption("Filter", 0)
      }
    }
  }

  implicit object RecordRuleListJsonFormat extends MythJsonPagedObjectListFormat[RecordRule] {
    def objectFieldName = "RecRuleList"
    def listFieldName = "RecRules"
    def convertElement(value: JsValue): RecordRule = value.convertTo[RecordRule]
    def elementToJson(elem: RecordRule): JsValue = jsonWriter[RecordRule].write(elem)
  }

  implicit object TitleInfoJsonFormat extends MythJsonObjectFormat[TitleInfo] {
    def objectFieldName = "TitleInfo"

    def write(t: TitleInfo): JsValue = JsObject(Map(
      "Title"   -> JsString(t.title),
      "Inetref" -> JsString(t.inetRef)
    ))

    def read(value: JsValue): TitleInfo = {
      val obj = value.asJsObject
      new TitleInfo {
        def title   = obj.stringField("Title")
        def inetRef = obj.stringField("Inetref")
      }
    }
  }

  implicit object TitleInfoListJsonFormat extends MythJsonListFormat[TitleInfo] {
    def objectFieldName = "TitleInfoList"
    def listFieldName = "TitleInfos"
    def convertElement(value: JsValue): TitleInfo = value.convertTo[TitleInfo]
    def elementToJson(elem: TitleInfo): JsValue = jsonWriter[TitleInfo].write(elem)
  }

  implicit object StorageGroupJsonFormat extends MythJsonObjectFormat[StorageGroup] {
    def objectFieldName = "StorageGroupDir"

    def write(sg: StorageGroup): JsValue = ???

    def read(value: JsValue): StorageGroup = {
      val obj = value.asJsObject
      new StorageGroup {
        def id        = StorageGroupId(obj.intField("Id"))
        def groupName = obj.stringField("GroupName")
        def hostName  = obj.stringField("HostName")
        def dirName   = obj.stringField("DirName")
      }
    }
  }

  implicit object StorageGroupListJsonFormat extends MythJsonListFormat[StorageGroup] {
    def objectFieldName = "StorageGroupDirList"
    def listFieldName = "StorageGroupDirs"
    def convertElement(value: JsValue): StorageGroup = value.convertTo[StorageGroup]
    def elementToJson(elem: StorageGroup): JsValue = jsonWriter[StorageGroup].write(elem)
  }

  implicit object RemoteEncoderStateJsonFormat extends MythJsonObjectFormat[RemoteEncoderState] {
    def objectFieldName = "Encoder" // TODO is this right? do we ever see this?

    def write(e: RemoteEncoderState): JsValue = JsObject(Map(
      "Id"             -> JsString(e.cardId.toString),
      "HostName"       -> JsString(e.host),
      "Local"          -> JsString(e.local.toString),
      "Connected"      -> JsString(e.connected.toString),
      "LowOnFreeSpace" -> JsString(e.lowFreeSpace.toString),
      "State"          -> JsString(e.state.id.toString),
      "SleepStatus"    -> JsString(e.sleepStatus.id.toString)
        // TODO embedded Recording object
    ))

    def read(value: JsValue): RemoteEncoderState = {
      val obj = value.asJsObject
      new RemoteEncoderState {
        def cardId           = CaptureCardId(obj.intField("Id"))
        def host             = obj.stringField("HostName")
        def port             = ???  // TODO

        def local            = obj.booleanField("Local")
        def connected        = obj.booleanField("Connected")
        def lowFreeSpace     = obj.booleanField("LowOnFreeSpace")
        def state            = TvState.applyOrUnknown(obj.intField("State"))
        def sleepStatus      = SleepStatus.applyOrUnknown(obj.intField("SleepStatus"))
        def currentRecording = None  // TODO Option[Recording], embedded object (check for "" StartTime)
      }
    }
  }

  // Result format for Dvr/GetEncoderList
  implicit object EncoderListJsonFormat extends MythJsonListFormat[RemoteEncoderState] {
    def objectFieldName = "EncoderList"
    def listFieldName = "Encoders"
    def convertElement(value: JsValue): RemoteEncoderState = value.convertTo[RemoteEncoderState]
    def elementToJson(elem: RemoteEncoderState): JsValue = jsonWriter[RemoteEncoderState].write(elem)
  }


  implicit object CaptureCardJsonFormat extends MythJsonObjectFormat[CaptureCard] {
    def objectFieldName = "CaptureCard"

    def write(c: CaptureCard): JsValue = JsObject(Map(
      "CardId"             -> JsString(c.cardId.id.toString),
      "VideoDevice"        -> JsString(c.videoDevice.getOrElse("")),
      "AudioDevice"        -> JsString(c.audioDevice.getOrElse("")),
      "VBIDevice"          -> JsString(c.vbiDevice.getOrElse("")),
      "CardType"           -> JsString(c.cardType.getOrElse("")),
      "AudioRateLimit"     -> JsString(c.audioRateLimit.getOrElse(0).toString),
      "HostName"           -> JsString(c.hostName),
      "DVBSWFilter"        -> JsString(c.dvbSwFilter.getOrElse(0).toString),
      "DVBSatType"         -> JsString(c.dvbSatType.getOrElse(0).toString),
      "DVBWaitForSeqStart" -> JsString(c.dvbWaitForSeqStart.toString),
      "SkipBTAudio"        -> JsString(c.skipBtAudio.toString),
      "DVBOnDemand"        -> JsString(c.dvbOnDemand.toString),
      "DVBDiSEqCType"      -> JsString(c.dvbDiseqcType.getOrElse(0).toString),
      "FirewireSpeed"      -> JsString(c.firewireSpeed.getOrElse(0).toString),
      "FirewireModel"      -> JsString(c.firewireModel.getOrElse("")),
      "FirewireConnection" -> JsString(c.firewireConnection.getOrElse(0).toString),
      "SignalTimeout"      -> JsString(c.signalTimeout.toString),
      "ChannelTimeout"     -> JsString(c.channelTimeout.toString),
      "DVBTuningDelay"     -> JsString(c.dvbTuningDelay.toString),
      "Contrast"           -> JsString(c.contrast.toString),
      "Brightness"         -> JsString(c.brightness.toString),
      "Colour"             -> JsString(c.colour.toString),
      "Hue"                -> JsString(c.hue.toString),
      "DiSEqCId"           -> JsString(c.diseqcId.getOrElse(0).toString),
      "DVBEITScan"         -> JsString(c.dvbEitScan.toString)
    ))

    def read(value: JsValue): CaptureCard = {
      val obj = value.asJsObject
      new CaptureCard {
        def cardId             = CaptureCardId(obj.intField("CardId"))
        def videoDevice        = obj.stringFieldOption("VideoDevice", "")
        def audioDevice        = obj.stringFieldOption("AudioDevice", "")
        def vbiDevice          = obj.stringFieldOption("VBIDevice", "")
        def cardType           = obj.stringFieldOption("CardType", "")
        def audioRateLimit     = obj.intFieldOption("AudioRateLimit", 0)
        def hostName           = obj.stringField("HostName")
        def dvbSwFilter        = obj.intFieldOption("DVBSWFilter", 0)
        def dvbSatType         = obj.intFieldOption("DVBSatType", 0)
        def dvbWaitForSeqStart = obj.booleanField("DVBWaitForSeqStart")
        def skipBtAudio        = obj.booleanField("SkipBTAudio")
        def dvbOnDemand        = obj.booleanField("DVBOnDemand")
        def dvbDiseqcType      = obj.intFieldOption("DVBDiSEqCType", 0)
        def firewireSpeed      = obj.intFieldOption("FirewireSpeed", 0)
        def firewireModel      = obj.stringFieldOption("FirewireModel", "")
        def firewireConnection = obj.intFieldOption("FirewireConnection", 0)
        def signalTimeout      = obj.intField("SignalTimeout")
        def channelTimeout     = obj.intField("ChannelTimeout")
        def dvbTuningDelay     = obj.intField("DVBTuningDelay")
        def contrast           = obj.intField("Contrast")
        def brightness         = obj.intField("Brightness")
        def colour             = obj.intField("Colour")
        def hue                = obj.intField("Hue")
        def diseqcId           = obj.intFieldOption("DiSEqCId", 0)
        def dvbEitScan         = obj.booleanField("DVBEITScan")
      }
    }
  }

  implicit object CaptureCardListJsonFormat extends MythJsonListFormat[CaptureCard] {
    def objectFieldName = "CaptureCardList"
    def listFieldName = "CaptureCards"
    def convertElement(value: JsValue): CaptureCard = value.convertTo[CaptureCard]
    def elementToJson(elem: CaptureCard): JsValue = jsonWriter[CaptureCard].write(elem)
  }

  implicit object ListingSourceJsonFormat extends MythJsonObjectFormat[ListingSource] {
    def objectFieldName = "VideoSource"

    def write(s: ListingSource): JsValue = JsObject(Map(
      "Id"         -> JsString(s.sourceId.id.toString),
      "SourceName" -> JsString(s.name),
      "Grabber"    -> JsString(s.grabber.getOrElse("")),
      "FreqTable"  -> JsString(s.freqTable),
      "LineupId"   -> JsString(s.lineupId.getOrElse("")),
      "UserId"     -> JsString(s.userId.getOrElse("")),
      "Password"   -> JsString(s.password.getOrElse("")),
      "UseEIT"     -> JsString(s.useEit.toString),
      "ConfigPath" -> JsString(s.configPath.getOrElse("")),
      "NITId"      -> JsString(s.dvbNitId.getOrElse(-1).toString)
    ))

    def read(value: JsValue): ListingSource = {
      val obj = value.asJsObject
      new ListingSource {
        def sourceId   = ListingSourceId(obj.intField("Id"))
        def name       = obj.stringField("SourceName")
        def grabber    = obj.stringFieldOption("Grabber", "")
        def freqTable  = obj.stringField("FreqTable")
        def lineupId   = obj.stringFieldOption("LineupId", "")
        def userId     = obj.stringFieldOption("UserId", "")
        def password   = obj.stringFieldOption("Password", "")
        def useEit     = obj.booleanField("UseEIT")
        def configPath = obj.stringFieldOption("ConfigPath", "")
        def dvbNitId   = obj.intFieldOption("NITId", -1)
      }
    }
  }

  implicit object ListingSourceListJsonFormat extends MythJsonObjectListFormat[ListingSource] {
    def objectFieldName = "VideoSourceList"
    def listFieldName = "VideoSources"
    def convertElement(value: JsValue): ListingSource = value.convertTo[ListingSource]
    def elementToJson(elem: ListingSource): JsValue = jsonWriter[ListingSource].write(elem)
  }

  implicit object SettingsJsonFormat extends MythJsonObjectFormat[Settings] {
    def objectFieldName = "SettingList"

    def write(s: Settings): JsValue = JsObject(Map(
      "HostName" -> JsString(s.hostName),
      "Settings" -> jsonWriter[Map[String, String]].write(s.settings)
    ))

    def read(value: JsValue): Settings = {
      val obj = value.asJsObject
      val settings = obj.fields("Settings").asJsObject
      val settingsMap: Map[String, String] =
        if (settings.fields.contains("") && settings.fields.size == 1) Map.empty
        else settings.convertTo[Map[String, String]]

      new Settings {
        def hostName = obj.stringField("HostName")
        def settings = settingsMap
      }
    }
  }

  implicit object TimeZoneInfoJsonFormat extends MythJsonObjectFormat[TimeZoneInfo] {
    def objectFieldName = "TimeZoneInfo"

    def write(z: TimeZoneInfo): JsValue = JsObject(Map(
      "TimeZoneID"      -> JsString(z.tzName),
      "UTCOffset"       -> jsonWriter[ZoneOffset].write(z.offset),
      "CurrentDateTime" -> jsonWriter[Instant].write(z.currentTime)
    ))

    def read(value: JsValue): TimeZoneInfo = {
      val obj = value.asJsObject
      new TimeZoneInfo {
        def tzName      = obj.stringField("TimeZoneID")
        def offset      = obj.fields("UTCOffset").convertTo[ZoneOffset]
        def currentTime = obj.fields("CurrentDateTime").convertTo[Instant]
      }
    }
  }

  implicit object ArtworkInfoJsonFormat extends MythJsonObjectFormat[ArtworkInfo] {
    def objectFieldName = "ArtworkInfo"

    def write(a: ArtworkInfo): JsValue = JsObject(Map(
      "URL"          -> JsString(a.url),
      "FileName"     -> JsString(a.fileName),
      "StorageGroup" -> JsString(a.storageGroup),
      "Type"         -> JsString(a.artworkType)
    ))

    def read(value: JsValue): ArtworkInfo = {
      val obj = value.asJsObject
      new ArtworkInfo {
        def url          = obj.stringField("URL")
        def fileName     = obj.stringField("FileName")
        def storageGroup = obj.stringField("StorageGroup")
        def artworkType  = obj.stringField("Type")
      }
    }
  }

  implicit object ArtworkInfoListJsonFormat extends MythJsonListFormat[ArtworkInfo] {
    def objectFieldName = "Artwork"
    def listFieldName = "ArtworkInfos"
    def convertElement(value: JsValue): ArtworkInfo = value.convertTo[ArtworkInfo]
    def elementToJson(elem: ArtworkInfo): JsValue = jsonWriter[ArtworkInfo].write(elem)
  }

  implicit object ChannelGuideJsonFormat extends MythJsonObjectFormat[(Channel, Seq[Program])] {
    def objectFieldName = "ChannelInfo"

    def write(x: (Channel, Seq[Program])): JsValue = ???

    def read(value: JsValue): (Channel, Seq[Program]) = {
      val obj = value.asJsObject

      val progs = channelContext.withValue(obj) { obj.convertTo[List[Program]] }
      val chan = new Channel {
        def chanId   = ChanId(obj.intField("ChanId"))
        def name     = obj.stringField("ChannelName")
        def number   = ChannelNumber(obj.stringField("ChanNum"))
        def callsign = obj.stringField("CallSign")
        def sourceId = ???
      }
      (chan, progs)
    }
  }

  implicit object ChannelGuideListJsonFormat extends MythJsonListFormat[(Channel, Seq[Program])] {
    def objectFieldName = "ProgramGuide"
    def listFieldName = "Channels"

    def convertElement(value: JsValue) = value.convertTo[(Channel, Seq[Program])]
    def elementToJson(elem: (Channel, Seq[Program])): JsValue = jsonWriter[(Channel, Seq[Program])].write(elem)
  }

  implicit object GuideJsonFormat extends MythJsonObjectFormat[Guide[Channel, Program]] {
    def objectFieldName = "ProgramGuide"

    def write(g: Guide[Channel, Program]): JsValue = ???

    def read(value: JsValue): Guide[Channel, Program] = {
      val obj = value.asJsObject

      // TODO FIXME avoid intermediate conversion to list
      val channelGuideList = value.convertTo[List[(Channel, Seq[Program])]]
      val channelGuide = channelGuideList.toMap

      new Guide[Channel, Program] {
        def startTime    = obj.dateTimeField("StartTime")
        def endTime      = obj.dateTimeField("EndTime")
        def startChanId  = ChanId(obj.intField("StartChanId"))
        def endChanId    = ChanId(obj.intField("EndChanId"))
        def programCount = obj.intField("Count")
        def programs     = channelGuide
      }
    }
  }

  implicit object VideoJsonFormat extends MythJsonObjectFormat[Video] {
    def objectFieldName = "VideoMetadataInfo"

    def write(v: Video): JsValue = JsObject(Map(
      "Id"               -> JsString(v.id.id.toString),
      "Title"            -> JsString(v.title),
      "SubTitle"         -> JsString(v.subtitle),
      "Director"         -> JsString(v.director),
      "Tagline"          -> JsString(v.tagline.getOrElse("")),
      "Description"      -> JsString(v.description),
      "Inetref"          -> JsString(v.inetRef),
      "HomePage"         -> JsString(v.homePage.getOrElse("")),
      "Studio"           -> JsString(v.studio.getOrElse("")),
      "Season"           -> JsString(v.season.getOrElse(0).toString),
      "Episode"          -> JsString(v.episode.getOrElse(0).toString),
      "Length"           -> JsString(v.length.map(_.toMinutes).getOrElse(0).toString),
      "PlayCount"        -> JsString(v.playCount.toString),
      "Hash"             -> JsString(v.hash.toString),
      "Visible"          -> JsString(v.visible.toString),
      "FileName"         -> JsString(v.fileName),
      "ContentType"      -> JsString(v.contentType),
      "HostName"         -> JsString(v.hostName),
      "AddDate"          -> JsString(v.addDate.map(_.toString).getOrElse("")),
      "Watched"          -> JsString(v.watched.toString),
      "UserRating"       -> JsString(v.userRating.toString),
      "Certification"    -> JsString(v.rating),
      "Collectionref"    -> JsString(v.collectionRef.toString),
      "ReleaseDate"      -> JsString(v.releaseDate.toString)
    ))

    def read(value: JsValue): Video = {
      val obj = value.asJsObject
      new Video {
        def id              = VideoId(obj.intField("Id"))
        def title           = obj.stringField("Title")
        def subtitle        = obj.stringField("SubTitle")
        def director        = obj.stringField("Director")
        def year            = ???  // TODO no Year field, pluck from releasedate?
        def tagline         = obj.stringFieldOption("Tagline", "")
        def description     = obj.stringField("Description")
        def inetRef         = obj.stringField("Inetref")  // TODO "00000000" used as null placeholder
        def homePage        = obj.stringFieldOption("HomePage", "")
        def studio          = obj.stringFieldOption("Studio", "")
        def season          = obj.intFieldOption("Season", 0)
        def episode         = obj.intFieldOption("Episode", 0)
        def length          = obj.intFieldOption("Length", 0) map (x => Duration.ofMinutes(x))
        def playCount       = obj.intField("PlayCount")
        def hash            = new MythFileHash(obj.stringField("Hash"))
        def visible         = obj.booleanField("Visible")
        def fileName        = obj.stringField("FileName")
        def contentType     = obj.stringField("ContentType")
        def hostName        = obj.stringField("HostName")
        def addDate         = obj.dateTimeFieldOption("AddDate") map (_.toInstant)
        def watched         = obj.booleanField("Watched")
        def userRating      = obj.doubleField("UserRating")
        def rating          = obj.stringField("Certification")
        def collectionRef   = obj.intField("Collectionref")  // TODO -1 used as default placeholder?
        // TODO release date may not always be in strict ISO format, see VideoId(1) or VideoId(12)
        def releaseDate     = obj.dateTimeField("ReleaseDate").toLocalDateTime().toLocalDate

        def artworkInfo     = obj.fields("Artwork").convertTo[List[ArtworkInfo]]
      }
    }
  }

  implicit object VideoListJsonFormat extends MythJsonPagedObjectListFormat[Video] {
    def objectFieldName = "VideoMetadataInfoList"
    def listFieldName = "VideoMetadataInfos"
    def convertElement(value: JsValue): Video = value.convertTo[Video]
    def elementToJson(elem: Video): JsValue = jsonWriter[Video].write(elem)
  }

  implicit object VideoMultiplexJsonFormat extends MythJsonObjectFormat[VideoMultiplex] {
    def objectFieldName = "VideoMultiplex"

    def write(m: VideoMultiplex): JsValue = JsObject(Map(
      "MplexId"          -> JsString(m.mplexId.id.toString),
      "SourceId"         -> JsString(m.sourceId.id.toString),
      "TransportId"      -> JsString(m.transportId.toString),
      "NetworkId"        -> JsString(m.networkId.getOrElse(0).toString),
      "Frequency"        -> JsString(m.frequency.toString),
      "Inversion"        -> JsString(m.inversion.toString),
      "SymbolRate"       -> JsString(m.symbolRate.toString),
      "FEC"              -> JsString(m.fec),
      "Polarity"         -> JsString(m.polarity.toString),
      "Modulation"       -> JsString(m.modulation),
      "Bandwidth"        -> JsString(m.bandwidth.toString),
      "LPCodeRate"       -> JsString(m.lpCodeRate),
      "TransmissionMode" -> JsString(m.transmissionMode.toString),
      "GuardInterval"    -> JsString(m.guardInterval),
      "Visible"          -> JsString(m.visible.toString),
      "Constellation"    -> JsString(m.constellation),
      "Hierarchy"        -> JsString(m.hierarchy),
      "HPCodeRate"       -> JsString(m.hpCodeRate),
      "ModulationSystem" -> JsString(m.modulationSystem),
      "RollOff"          -> JsString(m.rolloff),
      "SIStandard"       -> JsString(m.siStandard),
      "ServiceVersion"   -> JsString(m.serviceVersion.toString),
      "UpdateTimeStamp"  -> jsonWriter[Instant].write(m.updateTimestamp),
      "DefaultAuthority" -> JsString(m.defaultAuthority.getOrElse(""))
    ))

    def read(value: JsValue): VideoMultiplex = {
      val obj = value.asJsObject
      new VideoMultiplex {
        def mplexId          = MultiplexId(obj.intField("MplexId"))
        def sourceId         = ListingSourceId(obj.intField("SourceId"))
        def transportId      = obj.intField("TransportId")
        def networkId        = obj.intFieldOption("NetworkId", 0)
        def frequency        = obj.intField("Frequency")
        def inversion        = obj.charField("Inversion")
        def symbolRate       = obj.intField("SymbolRate")
        def fec              = obj.stringField("FEC")
        def polarity         = obj.charField("Polarity")
        def modulation       = obj.stringField("Modulation")
        def bandwidth        = obj.charField("Bandwidth")
        def lpCodeRate       = obj.stringField("LPCodeRate")
        def transmissionMode = obj.charField("TransmissionMode")
        def guardInterval    = obj.stringField("GuardInterval")
        def visible          = obj.booleanField("Visible")
        def constellation    = obj.stringField("Constellation")
        def hierarchy        = obj.stringField("Hierarchy")
        def hpCodeRate       = obj.stringField("HPCodeRate")
        def modulationSystem = obj.stringField("ModulationSystem")
        def rolloff          = obj.stringField("RollOff")
        def siStandard       = obj.stringField("SIStandard")
        def serviceVersion   = obj.intField("ServiceVersion")
        def updateTimestamp  = obj.fields("UpdateTimeStamp").convertTo[Instant]
        def defaultAuthority = obj.stringFieldOption("DefaultAuthority", "")
      }
    }
  }

  implicit object VideoMultiplexListJsonFormat extends MythJsonPagedObjectListFormat[VideoMultiplex] {
    def objectFieldName = "VideoMultiplexList"
    def listFieldName = "VideoMultiplexes"
    def convertElement(value: JsValue): VideoMultiplex = value.convertTo[VideoMultiplex]
    def elementToJson(elem: VideoMultiplex): JsValue = jsonWriter[VideoMultiplex].write(elem)
  }

  implicit object ListStreamJsonFormat extends MythJsonObjectFormat[LiveStream] {
    def objectFieldName = "LiveStreamInfo"

    def write(s: LiveStream): JsValue = ???

    def read(value: JsValue): LiveStream = {
      val obj = value.asJsObject
      new LiveStream {
        def id               = LiveStreamId(obj.intField("Id"))
        def width            = obj.intField("Width")
        def height           = obj.intField("Height")
        def bitrate          = obj.intField("Bitrate")
        def audioBitrate     = obj.intField("AudioBitrate")
        def segmentSize      = obj.intField("SegmentSize")
        def maxSegments      = obj.intField("MaxSegments")
        def startSegment     = obj.intField("StartSegment")
        def currentSegment   = obj.intField("CurrentSegment")
        def segmentCount     = obj.intField("SegmentCount")
        def percentComplete  = obj.intField("PercentComplete")
        def created          = obj.dateTimeField("Created").toInstant
        def lastModified     = obj.dateTimeField("LastModified").toInstant
        def relativeUrl      = obj.stringField("RelativeURL")
        def fullUrl          = obj.stringField("FullURL")
        def statusText       = obj.stringField("StatusStr")
        def statusCode       = obj.intField("StatusInt")
        def statusMessage    = obj.stringField("StatusMessage")
        def sourceFile       = obj.stringField("SourceFile")
        def sourceHost       = obj.stringField("SourceHost")
        def sourceWidth      = obj.intField("SourceWidth")
        def sourceHeight     = obj.intField("SourceHeight")
        def audioOnlyBitrate = obj.intField("AudioOnlyBitrate")
      }
    }
  }

  implicit object LiveStreamJsonListFormat extends MythJsonListFormat[LiveStream] {
    def objectFieldName = "LiveStreamInfoList"
    def listFieldName = "LiveStreamInfos"
    def convertElement(value: JsValue): LiveStream = value.convertTo[LiveStream]
    def elementToJson(elem: LiveStream): JsValue = jsonWriter[LiveStream].write(elem)
  }

}