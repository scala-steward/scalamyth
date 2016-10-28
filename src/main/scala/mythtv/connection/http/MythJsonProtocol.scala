package mythtv
package connection
package http

import java.time.{ Duration, Instant, LocalTime, Year, ZoneOffset }

import scala.util.DynamicVariable

import spray.json.{ DefaultJsonProtocol, RootJsonFormat, JsonFormat, deserializationError }
import spray.json.{ JsArray, JsObject, JsString, JsValue }

import util.{ ByteCount, DecimalByteCount, MythDateTime, MythFileHash }
import services.PagedList
import model.EnumTypes._
import model._

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

private[http] trait MythJsonObjectFormat[T] extends RootJsonFormat[T] {
  def objectFieldName: String
}

private[http] trait BaseMythJsonListFormat[T] {
  def listFieldName: String

  def convertElement(value: JsValue): T

  def readItems(obj: JsObject): List[T] = {
    if (!(obj.fields contains listFieldName))
      deserializationError(s"expected to find field name $listFieldName")

    val itemList: List[T] = obj.fields(listFieldName) match {
      case JsArray(elements) => elements.map(convertElement)(scala.collection.breakOut)
      case x => deserializationError(s"expected array in $listFieldName but got $x")
    }
    itemList
  }
}

private[http] trait MythJsonListFormat[T] extends BaseMythJsonListFormat[T] with MythJsonObjectFormat[List[T]] {
  def write(obj: List[T]): JsValue = ???

  def read(value: JsValue): List[T] = {
    val obj = value.asJsObject
    readItems(obj)
  }
}

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
private[http] trait MythJsonProtocol extends /*DefaultJsonProtocol*/ {

  // NB StringList can be converted by obj.fields("StringList").convertTo[List[String]]
  // NB StorageGroupDirList has a single field StorageGroupDirs which is an array of StorageGroupDir items

  // TODO [Root]JsonFormat are traits; build our own subclass trait
  // hierarchy so that the final objects are simple and made of
  // re-usable parts

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
      DupCheckIn.DupsInRecorded    -> "Current Recordings",
      DupCheckIn.DupsInOldRecorded -> "Previous Recordings",
      DupCheckIn.DupsInAll         -> "All Recordings",
      DupCheckIn.DupsNewEpisodes   -> "New Episodes Only"
    )
  }

  implicit object DupCheckMethodJsonFormat extends EnumDescriptionFormat[DupCheckMethod] {
    val id2Description: Map[DupCheckMethod, String] = Map(
      DupCheckMethod.DupCheckNone        -> "None",
      DupCheckMethod.DupCheckSubtitle    -> "Subtitle",
      DupCheckMethod.DupCheckDescription -> "Description",
      DupCheckMethod.DupCheckSubDesc     -> "Subtitle and Description",
      DupCheckMethod.DupCheckSubThenDesc -> "Subtitle then Description"
    )
  }

  implicit object StringListJsonFormat extends MythJsonListFormat[String] {
    import DefaultJsonProtocol.StringJsonFormat
    def objectFieldName = ""
    def listFieldName = "StringList"
    def convertElement(value: JsValue) = value.convertTo[String]
  }

  implicit object ZoneOffsetJsonFormat extends MythJsonObjectFormat[ZoneOffset] {
    def objectFieldName = "UTCOffset"

    def write(x: ZoneOffset): JsValue = ???

    def read(value: JsValue): ZoneOffset = {
      val secs = value match {
        case JsString(s) => s.toInt
        case x => x.toString.toInt
      }
      ZoneOffset.ofTotalSeconds(secs)
    }
  }

  implicit object InstantJsonFormat extends JsonFormat[Instant] {
    def write(x: Instant): JsValue = ???

    def read(value: JsValue): Instant = {
      val dtString = value match {
        case JsString(s) => s
        case x => x.toString
      }
      Instant.parse(dtString)
    }
  }

  implicit object ProgramJsonFormat extends MythJsonObjectFormat[Program] {
    def objectFieldName = "Program"

    def write(p: Program): JsValue = ???

    def read(value: JsValue): Program = {
      val obj = value.asJsObject

      /* Channel =
       "ATSCMajorChan":  "0",
       "ATSCMinorChan":  "0",
       "CallSign":       "KPBS-HD",
       "ChanFilters":    "",
       "ChanId":         "1151",
       "ChanNum":        "15-1",
       "ChannelName":    "KPBSDT (KPBS-DT)",
       "CommFree":       "0",
       "DefaultAuth":    "",
       "FineTune":       "0",
       "Format":         "",
       "Frequency":      "0",
       "FrequencyId":    "",
       "FrequencyTable": "",
       "IconURL":        "/Guide/GetChannelIcon?ChanId=1151",
       "InputId":        "0",
       "Modulation":     "",
       "MplexId":        "0",
       "NetworkId":      "0",
       "Programs":       [],
       "SIStandard":     "",
       "ServiceId":      "0",
       "SourceId":       "0",
       "TransportId":    "0",
       "UseEIT":         "false",
       "Visible":        "true",
       "XMLTVID":        ""
       */

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

      /* Recording=
       "DupInType":    "15",
       "DupMethod":    "6",
       "EncoderId":    "0",
       "EndTs":        "2016-09-26T13:00:00Z",
       "PlayGroup":    "Default",
       "Priority":     "0",
       "Profile":      "Default",
       "RecGroup":     "Cooking",
       "RecType":      "0",
       "RecordId":     "562",
       "StartTs":      "2016-09-26T12:30:00Z",
       "Status":       "-3",
       "StorageGroup": "Default"
       */

      // TODO Year field does not exist separately, but it the "Airdate" field may sometimes only
      //      contain a year, in which case originalAirDate should be None....

      // Return a Recording if there is a non-empty recording start time AND a non-empty filename
      if (rec.stringFieldOption("StartTs", "").nonEmpty) {
        if (obj.stringFieldOption("FileName", "").nonEmpty) {
          new Recording {
            override def toString: String = s"<JsonRecording $chanId, $startTime: $title>"

            def title                   = obj.stringField("Title")
            def subtitle                = obj.stringField("SubTitle")
            def description             = obj.stringField("Description")
            def syndicatedEpisodeNumber = ???
            def category                = obj.stringField("Category")
            def chanId                  = ChanId(channel.intFieldOrElse("ChanId", 0))
            def startTime               = obj.dateTimeField("StartTime")
            def endTime                 = obj.dateTimeField("EndTime")
            def seriesId                = obj.stringField("SeriesId")
            def programId               = obj.stringField("ProgramId")
            def stars                   = obj.doubleFieldOption("Stars")
            def originalAirDate         = obj.dateFieldOption("Airdate")
            def audioProps              = AudioProperties(obj.intField("AudioProps"))
            def videoProps              = VideoProperties(obj.intField("VideoProps"))
            def subtitleType            = SubtitleType(obj.intField("SubProps"))
            def year                    = obj.intFieldOption("Year") map Year.of  // TODO year field does not exist
            def partNumber              = None
            def partTotal               = None

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
            def programFlags            = obj.intField("ProgramFlags")
            def outputFilters           = ???

            def filename                = obj.stringField("FileName")
            def filesize                = DecimalByteCount(obj.longField("FileSize"))
            def season                  = obj.intFieldOrElse("Season", 0)
            def episode                 = obj.intFieldOrElse("Episode", 0)
            def inetRef                 = obj.stringField("Inetref")
          }
        }
        // Generate a Recordable if we have a recording StartTS but not FileName
        else new Recordable {
            override def toString: String = s"<JsonRecordable $chanId, $startTime: $title>"

            def title                   = obj.stringField("Title")
            def subtitle                = obj.stringField("SubTitle")
            def description             = obj.stringField("Description")
            def syndicatedEpisodeNumber = ???
            def category                = obj.stringField("Category")
            def chanId                  = ChanId(channel.intFieldOrElse("ChanId", 0))
            def startTime               = obj.dateTimeField("StartTime")
            def endTime                 = obj.dateTimeField("EndTime")
            def seriesId                = obj.stringField("SeriesId")
            def programId               = obj.stringField("ProgramId")
            def stars                   = obj.doubleFieldOption("Stars")
            def originalAirDate         = obj.dateFieldOption("Airdate")
            def audioProps              = AudioProperties(obj.intField("AudioProps"))
            def videoProps              = VideoProperties(obj.intField("VideoProps"))
            def subtitleType            = SubtitleType(obj.intField("SubProps"))
            def year                    = obj.intFieldOption("Year") map Year.of  // TODO year field does not exist
            def partNumber              = None
            def partTotal               = None

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
            def programFlags            = obj.intField("ProgramFlags")
            def outputFilters           = ???
        }
        // Recordable/Recording fields missing
        // findId
        // recpriority2
        // parentId
        // outputFilters
      }
      else new Program {
        override def toString: String = s"<JsonProgram $chanId, $startTime: $title>"

        def title                   = obj.stringField("Title")
        def subtitle                = obj.stringField("SubTitle")
        def description             = obj.stringField("Description")
        def syndicatedEpisodeNumber = ???
        def category                = obj.stringField("Category")
        def chanId                  = ChanId(channel.intFieldOrElse("ChanId", 0))
        def startTime               = obj.dateTimeField("StartTime")
        def endTime                 = obj.dateTimeField("EndTime")
        def seriesId                = obj.stringField("SeriesId")
        def programId               = obj.stringField("ProgramId")
        def stars                   = obj.doubleFieldOption("Stars")
        def originalAirDate         = obj.dateFieldOption("Airdate")
        def audioProps              = AudioProperties(obj.intField("AudioProps"))
        def videoProps              = VideoProperties(obj.intField("VideoProps"))
        def subtitleType            = SubtitleType(obj.intField("SubProps"))
        def year                    = obj.intFieldOption("Year") map Year.of
        def partNumber              = None
        def partTotal               = None
      }

      /*
       * Other fields: (data)
       * Everything seems to be encoded as a string!
       *
        "Airdate":      "2015-10-29",
        "AudioProps":   "1",
        "CatType":      "",
        "Category":     "Cooking",
        "Description":  "Sticky toffee pudding ...",
        "EndTime":      "2016-09-26T13:00:00Z",
        "Episode":      "5",
        "FileName":     "1151_20160926123000.mpg",
        "FileSize":     "3130823784",
        "HostName":     "myth1",
        "Inetref":      "270411",
        "LastModified": "2016-09-26T13:00:00Z",
        "ProgramFlags": "804319236",
        "ProgramId":    "EP013602850055",
        "Repeat":       "true",
        "Season":       "5",
        "SeriesId":     "EP01360285",
        "Stars":        "0",
        "StartTime":    "2016-09-26T12:30:00Z",
        "SubProps":     "1",
        "SubTitle":     "Bake It Dark",
        "Title":        "Martha Bakes",
        "VideoProps":   "18"

       missing:
         syndicatedEpisodeNumber   (but we do have "Season" and "Episode")
         partNumber
         partTotal
       */
    }

  }

  implicit object PagedProgramListJsonFormat extends MythJsonPagedObjectListFormat[Program] {
    def objectFieldName = "ProgramList"
    def listFieldName = "Programs"
    def convertElement(value: JsValue): Program = value.convertTo[Program]
  }

  // used for reading Guide
  implicit object ProgramListJsonFormat extends MythJsonListFormat[Program] {
    def objectFieldName = "ChannelInfo"
    def listFieldName = "Programs"
    def convertElement(value: JsValue): Program = value.convertTo[Program]
  }

  implicit object ChannelDetailsJsonFormat extends MythJsonObjectFormat[ChannelDetails] {
    def objectFieldName = "ChannelInfo"  // or "Channel"

    def write(c: ChannelDetails): JsValue = ???
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
        def useOnAirGuide    = obj.booleanField("UseEIT")
        def mplexId          = obj.intFieldOption("MplexId", 0) map MultiplexId
        def serviceId        = obj.intFieldOption("ServiceId", 0)
        def atscMajorChan    = obj.intFieldOption("ATSCMajorChan", 0)
        def atscMinorChan    = obj.intFieldOption("ATSCMinorChan", 0)
        def defaultAuthority = obj.stringFieldOption("DefaultAuth", "")
      }
    }
  }

  implicit object ChannelDetailsListJsonFormat extends MythJsonPagedObjectListFormat[ChannelDetails] {
    def objectFieldName = "ChannelInfoList"
    def listFieldName = "ChannelInfos"
    def convertElement(value: JsValue): ChannelDetails = value.convertTo[ChannelDetails]
  }

  implicit object RecordRuleJsonFormat extends MythJsonObjectFormat[RecordRule] {
    def objectFieldName = "RecRule"

    def write(p: RecordRule): JsValue = ???

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
  }

  implicit object RemoteEncoderStateJsonFormat extends MythJsonObjectFormat[RemoteEncoderState] {
    def objectFieldName = "Encoder" // TODO is this right? do we ever see this?

    def write(p: RemoteEncoderState): JsValue = ???

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
  }

  implicit object CaptureCardJsonFormat extends MythJsonObjectFormat[CaptureCard] {
    def objectFieldName = "CaptureCard"

    def write(c: CaptureCard): JsValue = ???

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
  }

  implicit object ListingSourceJsonFormat extends MythJsonObjectFormat[ListingSource] {
    def objectFieldName = "VideoSource"

    def write(s: ListingSource): JsValue = ???

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
  }

  implicit object SettingsJsonFormat extends MythJsonObjectFormat[Settings] {
    def objectFieldName = "SettingList"

    def write(s: Settings): JsValue = ???

    def read(value: JsValue): Settings = {
      val obj = value.asJsObject
      val fields = obj.fields("Settings").asJsObject.fields
      val settingsMap: Map[String, String] =
        if (fields.contains("") && fields.size == 1) Map.empty
        else fields mapValues {
          case JsString(s) => s
          case x => x.toString
        }

      new Settings {
        def hostName = obj.stringField("HostName")
        def settings = settingsMap
      }
    }
  }

  implicit object TimeZoneInfoJsonFormat extends MythJsonObjectFormat[TimeZoneInfo] {
    def objectFieldName = "TimeZoneInfo"

    def write(z: TimeZoneInfo): JsValue = ???

    def read(value: JsValue): TimeZoneInfo = {
      val obj = value.asJsObject
      new TimeZoneInfo {
        def tzName      = obj.stringField("TimeZoneID")
        def offset      = obj.fields("UTCOffset").convertTo[ZoneOffset]
        def currentTime = obj.fields("CurrentDateTime").convertTo[Instant]
      }
    }
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

    def write(v: Video): JsValue = ???

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
        def inetRef         = obj.stringField("Inetref")
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
        def collectionRef   = obj.intField("Collectionref")
        // TODO release data may not always be in strict ISO format, see VideoId(12)
        def releaseDate     = obj.dateTimeField("ReleaseDate").toLocalDateTime().toLocalDate
      }
    }
  }

  implicit object VideoListJsonFormat extends MythJsonPagedObjectListFormat[Video] {
    def objectFieldName = "VideoMetadataInfoList"
    def listFieldName = "VideoMetadataInfos"
    def convertElement(value: JsValue): Video = value.convertTo[Video]
  }

  implicit object VideoMultiplexJsonFormat extends MythJsonObjectFormat[VideoMultiplex] {
    def objectFieldName = "VideoMultiplex"

    def write(m: VideoMultiplex): JsValue = ???

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
  }

}
