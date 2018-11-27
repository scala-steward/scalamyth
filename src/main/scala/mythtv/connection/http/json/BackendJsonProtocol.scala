package mythtv
package connection
package http
package json

import java.net.InetAddress
import java.time.{ DayOfWeek, Duration, Instant, LocalTime, ZoneOffset }

import scala.util.{ DynamicVariable, Try }

import spray.json.{ JsonFormat, RootJsonFormat, jsonWriter }
import spray.json.{ JsNumber, JsObject, JsString, JsValue }
import spray.json.DefaultJsonProtocol.{ listFormat, StringJsonFormat }

import util.{ DecimalByteCount, MythFileHash, URIFactory }
import services.{ IsServicesListObject, ServicesObject }
import model.EnumTypes._
import model._

private[json] trait ServicesObjectFormat[T] extends RootJsonFormat[ServicesObject[T]] {
  import RichJsonObject._

  def dataFieldName: String
  def readData(obj: JsObject): T
  def writeData(data: T): JsValue
  def writeExtraFields(obj: ServicesObject[T]): Map[String, JsValue] = Map.empty

  def write(obj: ServicesObject[T]): JsValue = JsObject(Map(
    dataFieldName -> writeData(obj.data),
    "AsOf"        -> JsString(obj.asOf.toIsoFormat),
    "Version"     -> JsString(obj.mythVersion),
    "ProtoVer"    -> JsString(obj.mythProtocolVersion)
  ) ++ writeExtraFields(obj))

  def read(value: JsValue): ServicesObject[T] = {
    val obj = value.asJsObject
    val dataValue = readData(obj)
    new ServicesObject[T] {
      def data = dataValue
      def asOf = obj.dateTimeField("AsOf")
      def mythVersion = obj.stringField("Version")
      def mythProtocolVersion = obj.stringField("ProtoVer")
    }
  }
}

private[json] trait ServicesObjectGuideJsonFormat[C <: Channel, P <: ProgramBrief] extends ServicesObjectFormat[Guide[C, P]] {
  import RichJsonObject._

  def dataFieldName = "Channels"
  def readChannelGuide(obj: JsValue): Map[C, Seq[P]]

  override def writeExtraFields(obj: ServicesObject[Guide[C, P]]): Map[String, JsValue] = {
    val g = obj.data
    Map(
      "StartTime"     -> JsString(g.startTime.toIsoFormat),
      "EndTime"       -> JsString(g.endTime.toIsoFormat),
      "StartChanId"   -> JsString(g.startChanId.id.toString),
      "EndChanId"     -> JsString(g.endChanId.id.toString),
      "NumOfChannels" -> JsString(g.programs.size.toString),
      "Count"         -> JsString(g.programCount.toString)
    )
  }

  def readData(obj: JsObject): Guide[C, P] = new Guide[C, P] {
    def startTime    = obj.dateTimeField("StartTime")
    def endTime      = obj.dateTimeField("EndTime")
    def startChanId  = ChanId(obj.intField("StartChanId"))
    def endChanId    = ChanId(obj.intField("EndChanId"))
    def programCount = obj.intField("Count")
    def programs     = readChannelGuide(obj.fields(dataFieldName))
  }
}

private[json] trait EnumDescriptionFormat[T] extends JsonFormat[T] {
  def id2Description: (T) => String
  def description2Id: (String) => T

  def write(p: T): JsValue = JsString(id2Description(p))
  def read(value: JsValue): T = value match {
    case JsString(s) => description2Id(s)
    case x => description2Id(x.toString)
  }
}

/*
 * NOTES
 *
 * Starting in MythTV 0.28, RecStatus is serialized different in XML vs JSON:
 *   XML is serialized as name text, JSON is serialized as an integer enum id
 */

/* Inheriting from DefaultJsonProtocol can cause huge bytecode bloat */
private[json] trait BackendJsonProtocol extends CommonJsonProtocol {
  import RichJsonObject._
  import scala.language.implicitConversions

  type GuideTuple = (ChannelDetails, Seq[Program])
  type GuideBriefTuple = (Channel, Seq[ProgramBrief])

  private val channelContext = new DynamicVariable[RichJsonObject](EmptyJsonObject)

  implicit def servicesObjectListFormat[T :IsServicesListObject :JsonFormat]: ServicesObjectFormat[List[T]] = {
    new ServicesObjectFormat[List[T]] {
      def dataFieldName = implicitly[IsServicesListObject[T]].listFieldName
      def writeData(data: List[T]) = jsonWriter[List[T]].write(data)
      def readData(obj: JsObject): List[T] = obj.fields(dataFieldName).convertTo[List[T]]
    }
  }

  implicit def servicesPagedListFormat[T :IsServicesListObject :JsonFormat]: RootJsonFormat[ServicesPagedList[T]] = {
    val listFieldName = implicitly[IsServicesListObject[T]].listFieldName

    new RootJsonFormat[ServicesPagedList[T]] {
      def write(obj: ServicesPagedList[T]): JsValue = JsObject(Map(
        listFieldName    -> jsonWriter[List[T]].write(obj.data),
        "Count"          -> JsString(obj.count.toString),
        "TotalAvailable" -> JsString(obj.totalAvailable.toString),
        "StartIndex"     -> JsString(obj.startIndex.toString),
        "AsOf"           -> JsString(obj.asOf.toIsoFormat),
        "Version"        -> JsString(obj.mythVersion),
        "ProtoVer"       -> JsString(obj.mythProtocolVersion)
      ))

      def read(value: JsValue): ServicesPagedList[T] = {
        val obj = value.asJsObject
        val itemList = obj.fields(listFieldName).convertTo[List[T]]

        new ServicesPagedList[T] {
          def data                = itemList
          def count               = obj.intField("Count")
          def totalAvailable      = obj.intField("TotalAvailable")
          def startIndex          = obj.intField("StartIndex")
          def asOf                = obj.dateTimeField("AsOf")
          def mythVersion         = obj.stringField("Version")
          def mythProtocolVersion = obj.stringField("ProtoVer")
        }
      }
    }
  }

  implicit object RecSearchTypeJsonFormat extends EnumDescriptionFormat[RecSearchType] {
    def id2Description: (RecSearchType) => String = RecSearchType.description
    def description2Id: (String) => RecSearchType = RecSearchType.withDescription
  }

  implicit object RecTypeJsonFormat extends EnumDescriptionFormat[RecType] {
    def id2Description: (RecType) => String = RecType.description
    def description2Id: (String) => RecType = RecType.withDescription
  }

  implicit object DupCheckInJsonFormat extends EnumDescriptionFormat[DupCheckIn] {
    def id2Description: (DupCheckIn) => String = DupCheckIn.description
    def description2Id: (String) => DupCheckIn = DupCheckIn.withDescription
  }

  implicit object DupCheckMethodJsonFormat extends EnumDescriptionFormat[DupCheckMethod] {
    def id2Description: (DupCheckMethod) => String = DupCheckMethod.description
    def description2Id: (String) => DupCheckMethod = DupCheckMethod.withDescription
  }

  implicit object MythLogLevelJsonFormat extends EnumDescriptionFormat[MythLogLevel] {
    def id2Description: (MythLogLevel) => String = MythLogLevel.description
    def description2Id: (String) => MythLogLevel = MythLogLevel.withDescription
  }

  trait IntegerIdentifierJsonFormat[T <: IntegerIdentifier] extends JsonFormat[T] {
    def factory: (Int) => T
    def write(i: T): JsValue = JsString(i.id.toString)
    def read(value: JsValue): T = {
      val i = value match {
        case JsString(s) => s.toInt
        case JsNumber(x) => x.intValue
        case x => x.toString.toInt
      }
      factory(i)
    }
  }

  implicit object CaptureCardIdJsonFormat   extends IntegerIdentifierJsonFormat[CaptureCardId]   { def factory = CaptureCardId.apply }
  implicit object InputIdJsonFormat         extends IntegerIdentifierJsonFormat[InputId]         { def factory = InputId.apply }
  implicit object ListingSourceIdJsonFormat extends IntegerIdentifierJsonFormat[ListingSourceId] { def factory = ListingSourceId.apply }
  implicit object RecordRuleIdJsonFormat    extends IntegerIdentifierJsonFormat[RecordRuleId]    { def factory = RecordRuleId.apply }

  implicit object RecordedIdJsonFormat extends RootJsonFormat[RecordedId] {
    import RecordedId._
    def write(recordedId: RecordedId): JsValue = JsString(recordedId.idString)
    def read(value: JsValue): RecordedId = {
      // we only support reading RecordedIdInt, not RecordedIdChanTime
      val i = value match {
        case JsString(s) => s.toInt
        case JsNumber(x) => x.intValue
        case x => x.toString.toInt
      }
      RecordedIdInt(i)
    }
  }

  trait VideoPositionJsonFormat[VP <: VideoPosition] extends RootJsonFormat[VP] {
    def factory: Long => VP
    def write(pos: VP): JsValue = JsString(pos.pos.toString)
    def read(value: JsValue): VP = {
      val f = value match {
        case JsString(s) => s.toLong
        case JsNumber(x) => x.longValue
        case x => x.toString.toLong
      }
      factory(f)
    }
  }

  implicit object VideoPositionBytesFormat extends VideoPositionJsonFormat[VideoPositionBytes] {
    def factory = VideoPositionBytes.apply
  }

  implicit object VideoPositionFrameFormat extends VideoPositionJsonFormat[VideoPositionFrame] {
    def factory = VideoPositionFrame.apply
  }

  implicit object VideoPositionMillisecondsFormat extends VideoPositionJsonFormat[VideoPositionMilliseconds] {
    def factory = VideoPositionMilliseconds.apply
  }

  implicit object ArtworkInfoJsonFormat extends RootJsonFormat[ArtworkInfo] {
    def write(a: ArtworkInfo): JsValue = JsObject(Map(
      "URL"          -> JsString(a.uri.toString),
      "FileName"     -> JsString(a.fileName),
      "StorageGroup" -> JsString(a.storageGroup),
      "Type"         -> JsString(a.artworkType)
    ))

    def read(value: JsValue): ArtworkInfo = {
      val obj = value.asJsObject
      new ArtworkInfo {
        def uri          = URIFactory(obj.stringField("URL"))
        def fileName     = obj.stringField("FileName")
        def storageGroup = obj.stringField("StorageGroup")
        def artworkType  = obj.stringField("Type")
      }
    }
  }

  implicit object ArtworkItemJsonFormat extends JsonFormat[ArtworkItem] {
    def write(a: ArtworkItem): JsValue = JsObject(Map(
      "Url"       -> JsString(a.uri.toString),
      "Thumbnail" -> JsString(a.thumbnail),
      "Type"      -> JsString(a.artworkType),
      "Width"     -> JsString(a.width.getOrElse(0).toString),
      "Height"    -> JsString(a.height.getOrElse(0).toString)
    ))
    def read(value: JsValue): ArtworkItem = {
      val obj = value.asJsObject
      new ArtworkItem {
        def uri         = URIFactory(obj.stringField("Url"))
        def thumbnail   = obj.stringField("Thumbnail")
        def artworkType = obj.stringField("Type")
        def width       = obj.intFieldOption("Width", 0)
        def height      = obj.intFieldOption("Height", 0)
      }
    }
  }

  implicit object RecordingJsonFormat extends RootJsonFormat[Recording] {
    import RecordedId._

    def write(r: Recording): JsValue = {
      val rmap: Map[String, JsValue] = RecordableJsonFormat.write(r) match {
        case JsObject(fields) => fields
        case _ => throw new RuntimeException("RecordableJsonFormat.write failed to create a JsObject")
      }
      val recmap: Map[String, JsValue] = rmap("Recording") match {
        case JsObject(fields) => fields
        case _ => throw new RuntimeException("RecordableJsonFormat failed to add a Recording node")
      }
      val recIdMap: Map[String, JsValue] = r.recordedId match {
        case RecordedIdInt(id) => Map("RecordedId" -> JsString(id.toString))
        case RecordedIdChanTime(_, _) => Map.empty
      }
      JsObject(rmap ++ Map(
        "FileName"   -> JsString(r.filename),
        "FileSize"   -> JsString(r.filesize.bytes.toString),
        "Season"     -> JsString(r.season.getOrElse(0).toString),
        "Episode"    -> JsString(r.episode.getOrElse(0).toString),
        "Inetref"    -> JsString(r.inetRef.getOrElse("")),
        "Recording"  -> JsObject(recmap ++ recIdMap ++ Map(
          "FileName"   -> JsString(r.filename),
          "FileSize"   -> JsString(r.filesize.bytes.toString)
        ))
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

      val inferredRecordedId: RecordedId = {
        rec.intFieldOption("RecordedId") map RecordedIdInt
      } getOrElse {
        if (obj.stringFieldOption("FileName", "").nonEmpty) {
          val chanId = ChanId(channel.intFieldOrElse("ChanId", 0))
          val startTs = rec.dateTimeField("StartTs")
          RecordedIdChanTime(chanId, startTs)
        }
        else RecordedId.empty
      }

      new Recording with HasArtworkInfo {
        override def toString: String = s"<JsonRecording $chanId, $startTime: $combinedTitle>"

        def title                   = obj.stringField("Title")
        def subtitle                = obj.stringField("SubTitle")
        def description             = obj.stringField("Description")
        def syndicatedEpisode       = ""
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
        def year                    = None
        def season                  = obj.intFieldOption("Season", 0)
        def episode                 = obj.intFieldOption("Episode", 0)
        def totalEpisodes           = obj.intFieldOption("TotalEpisodes", 0)
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
        def parentId                = None
        def lastModified            = obj.dateTimeField("LastModified")
        def chanNum                 = ChannelNumber(channel.stringFieldOrElse("ChanNum", ""))
        def callsign                = channel.stringFieldOrElse("CallSign", "")
        def chanName                = channel.stringFieldOrElse("ChannelName", "")
        def outputFilters           = channel.stringFieldOrElse("ChanFilters", "")

        def filename                = obj.stringField("FileName")
        def filesize                = DecimalByteCount(obj.longField("FileSize"))
        def inetRef                 = obj.stringFieldOption("Inetref", "")
        def recordedId              = inferredRecordedId

        def artworkInfo             = obj.fields("Artwork").asJsObject.fields("ArtworkInfos").convertTo[List[ArtworkInfo]]
      }
    }
  }

  implicit object RecordableJsonFormat extends RootJsonFormat[Recordable] {
    // Recordable/Recording fields missing
    // findId
    // recpriority2
    // parentId

    def write(r: Recordable): JsValue = {
      val pmap: Map[String, JsValue] = ProgramJsonFormat.write(r) match {
        case JsObject(fields) => fields
        case _ => throw new RuntimeException("ProgramJsonFormat failed to create a JsObject")
      }
      val cmap: Map[String, JsValue] = pmap("Channel") match {
        case JsObject(fields) => fields
        case _ => throw new RuntimeException("ProgramJsonFormat failed to add a Channel node")
      }
      JsObject(pmap ++ Map(
        "HostName"       -> JsString(r.hostname),                 // deprecated to serialize here, starting with 0.28
        "LastModified"   -> JsString(r.lastModified.toString),    // deprecated to serialize here, starting with 0.28
        "Channel"      -> JsObject(cmap ++ Map(
          "ChanNum"      -> JsString(r.chanNum.num),
          "CallSign"     -> JsString(r.callsign),
          "ChannelName"  -> JsString(r.chanName),
          "SourceId"     -> JsString(r.sourceId.id.toString),
          "InputId"      -> JsString(r.inputId.id.toString)
        )),
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
          "HostName"     -> JsString(r.hostname),               // serialized here starting with MythTV 0.28
          "LastModified" -> JsString(r.lastModified.toString),  // serialized here starting with MythTV 0.28
          "RecGroup"     -> JsString(r.recGroup),
          "PlayGroup"    -> JsString(r.playGroup),
          "StorageGroup" -> JsString(r.storageGroup),
          "Profile"      -> JsString("Default")  // RecProfile is not in Recordable/Recording
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

      new Recordable with HasArtworkInfo {
        override def toString: String = s"<JsonRecordable $chanId, $startTime: $combinedTitle>"

        def title                   = obj.stringField("Title")
        def subtitle                = obj.stringField("SubTitle")
        def description             = obj.stringField("Description")
        def syndicatedEpisode       = ""
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
        def year                    = None
        def season                  = obj.intFieldOption("Season", 0)
        def episode                 = obj.intFieldOption("Episode", 0)
        def totalEpisodes           = obj.intFieldOption("TotalEpisodes", 0)
        def partNumber              = None
        def partTotal               = None
        def programFlags            = ProgramFlags(obj.intField("ProgramFlags"))

        def findId                  = 0 //???
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
        def parentId                = None
        def lastModified            = obj.dateTimeField("LastModified")
        def chanNum                 = ChannelNumber(channel.stringFieldOrElse("ChanNum", ""))
        def callsign                = channel.stringFieldOrElse("CallSign", "")
        def chanName                = channel.stringFieldOrElse("ChannelName", "")
        def outputFilters           = channel.stringFieldOrElse("ChanFilters", "")

        def artworkInfo             = obj.fields("Artwork").asJsObject.fields("ArtworkInfos").convertTo[List[ArtworkInfo]]
      }
    }
  }

  implicit object ProgramJsonFormat extends RootJsonFormat[Program] {
    def write(p: Program): JsValue = JsObject(Map(
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
      "Season"       -> JsString(p.season.getOrElse(0).toString),
      "Episode"      -> JsString(p.episode.getOrElse(0).toString),
      "FileSize"     -> JsString("0"),   // does not exist in Program, DEPRECATED
      "FileName"     -> JsString(""),    // does not exist in Program, DEPRECATED
      "HostName"     -> JsString(""),    // does not exist in Program, DEPRECATED
      "LastModified" -> JsString(""),    // does not exist in Program
      "Inetref"      -> JsString(""),    // does not exist in Program
      "Channel"      -> JsObject(Map(
        "ChanId"     -> JsString(p.chanId.id.toString)
      )),
      "Recording"    -> JsObject(Map.empty[String, JsValue]),
      "Artwork"      -> writeArtwork(p)
    ))

    private[BackendJsonProtocol] def writeArtwork(p: ProgramBrief): JsValue = {
      val art = p match {
        case a: HasArtworkInfo => a.artworkInfo
        case _ => Nil
      }
      JsObject(Map("ArtworkInfos" -> jsonWriter[List[ArtworkInfo]].write(art)))
    }

    def read(value: JsValue): Program = {
      val obj = value.asJsObject

      val channel: RichJsonObject =  // inner object
        if (obj.fields contains "Channel") obj.fields("Channel").asJsObject
        else channelContext.value

      val rec: RichJsonObject =      // inner object
        if (obj.fields contains "Recording") obj.fields("Recording").asJsObject
        else EmptyJsonObject

      // Determine which type of trait we should return based on available data:
      //  - a Recording  if there is a non-empty recording StartTS AND a non-empty FileName
      //  - a Recordable if we have a recording StartTS but not FileName
      //  - a Program    otherwise
      if (rec.stringFieldOption("StartTs", "").nonEmpty) {
        if (obj.stringFieldOption("FileName", "").nonEmpty) RecordingJsonFormat.read(value)
        else                                                RecordableJsonFormat.read(value)
      }
      else new Program with HasArtworkInfo {
        override def toString: String = s"<JsonProgram $chanId, $startTime: $combinedTitle>"

        def title                   = obj.stringField("Title")
        def subtitle                = obj.stringField("SubTitle")
        def description             = obj.stringField("Description")
        def syndicatedEpisode       = ""
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
        def year                    = None
        def season                  = obj.intFieldOption("Season", 0)
        def episode                 = obj.intFieldOption("Episode", 0)
        def totalEpisodes           = obj.intFieldOption("TotalEpisodes", 0)
        def partNumber              = None
        def partTotal               = None
        def programFlags            = ProgramFlags(obj.intField("ProgramFlags"))

        def artworkInfo             = obj.fields("Artwork").asJsObject.fields("ArtworkInfos").convertTo[List[ArtworkInfo]]
      }

      /* missing:
         syndicatedEpisode   (but we do have "Season" and "Episode")
         year                (but we do have "Airdate")
         partNumber
         partTotal */
    }
  }

  implicit object RecordingBriefJsonFormat extends RootJsonFormat[RecordingBrief] {
    import RecordedId._

    def write(r: RecordingBrief): JsValue = {
      val rmap: Map[String, JsValue] = RecordableBriefJsonFormat.write(r) match {
        case JsObject(fields) => fields
        case _ => throw new RuntimeException("RecordableBriefJsonFormat.write failed to create a JsObject")
      }
      val recmap: Map[String, JsValue] = rmap("Recording") match {
        case JsObject(fields) => fields
        case _ => throw new RuntimeException("RecordableJsonFormat failed to add a Recording node")
      }
      val recIdMap: Map[String, JsValue] = r.recordedId match {
        case RecordedIdInt(id) => Map("RecordedId" -> JsString(id.toString))
        case RecordedIdChanTime(_, _) => Map.empty
      }
      JsObject(rmap ++ Map(
        "Recording"  -> JsObject(recmap ++ recIdMap)
      ))
    }

    def read(value: JsValue): RecordingBrief = {
      val obj = value.asJsObject

      val channel: RichJsonObject =  // inner object
        if (obj.fields contains "Channel") obj.fields("Channel").asJsObject
        else channelContext.value

      val rec: RichJsonObject =      // inner object
        if (obj.fields contains "Recording") obj.fields("Recording").asJsObject
        else EmptyJsonObject

      val inferredRecordedId: RecordedId = {
        rec.intFieldOption("RecordedId") map RecordedIdInt
      } getOrElse {
        val chanId = ChanId(channel.intFieldOrElse("ChanId", 0))
        val startTs = rec.dateTimeField("StartTs")
        RecordedIdChanTime(chanId, startTs)
      }

      new RecordingBrief {
        override def toString: String = s"<JsonRecordingBrief $chanId, $startTime: $combinedTitle>"

        def title        = obj.stringField("Title")
        def subtitle     = obj.stringField("SubTitle")
        def chanId       = ChanId(channel.intFieldOrElse("ChanId", 0))
        def startTime    = obj.dateTimeField("StartTime")
        def endTime      = obj.dateTimeField("EndTime")
        def category     = obj.stringField("Category")
        def categoryType = obj.stringFieldOption("CatType", "") map CategoryType.withName
        def audioProps   = AudioProperties(obj.intField("AudioProps"))
        def videoProps   = VideoProperties(obj.intField("VideoProps"))
        def subtitleType = SubtitleType(obj.intField("SubProps"))
        def isRepeat     = obj.booleanField("Repeat")

        def recStatus    = RecStatus.applyOrUnknown(rec.intField("Status"))
        def recPriority  = rec.intField("Priority")
        def recStartTS   = rec.dateTimeField("StartTs")
        def recEndTS     = rec.dateTimeField("EndTs")
        def chanNum      = ChannelNumber(channel.stringFieldOrElse("ChanNum", ""))
        def callsign     = channel.stringFieldOrElse("CallSign", "")
        def chanName     = channel.stringFieldOrElse("ChannelName", "")

        def recordedId   = inferredRecordedId
      }
    }
  }

  implicit object RecordableBriefJsonFormat extends RootJsonFormat[RecordableBrief] {
    def write(r: RecordableBrief): JsValue = {
      val pmap: Map[String, JsValue] = ProgramBriefJsonFormat.write(r) match {
        case JsObject(fields) => fields
        case _ => throw new RuntimeException("ProgramBriefJsonFormat failed to create a JsObject")
      }
      val cmap: Map[String, JsValue] = pmap("Channel") match {
        case JsObject(fields) => fields
        case _ => throw new RuntimeException("ProgramBriefJsonFormat failed to add a Channel node")
      }
      JsObject(pmap ++ Map(
        "Channel"      -> JsObject(cmap ++ Map(
          "ChanNum"      -> JsString(r.chanNum.num),
          "CallSign"     -> JsString(r.callsign),
          "ChannelName"  -> JsString(r.chanName)
        )),
        "Recording"    -> JsObject(Map(
          "Priority"     -> JsString(r.recPriority.toString),
          "Status"       -> JsString(r.recStatus.id.toString),
          "StartTs"      -> JsString(r.recStartTS.toString),
          "EndTs"        -> JsString(r.recEndTS.toString)
        ))
      ))
    }

    def read(value: JsValue): RecordableBrief = {
      val obj = value.asJsObject

      val channel: RichJsonObject =  // inner object
        if (obj.fields contains "Channel") obj.fields("Channel").asJsObject
        else channelContext.value

      val rec: RichJsonObject =      // inner object
        if (obj.fields contains "Recording") obj.fields("Recording").asJsObject
        else EmptyJsonObject

      new RecordableBrief {
        override def toString: String = s"<JsonRecordableBrief $chanId, $startTime: $combinedTitle>"

        def title        = obj.stringField("Title")
        def subtitle     = obj.stringField("SubTitle")
        def chanId       = ChanId(channel.intFieldOrElse("ChanId", 0))
        def startTime    = obj.dateTimeField("StartTime")
        def endTime      = obj.dateTimeField("EndTime")
        def category     = obj.stringField("Category")
        def categoryType = obj.stringFieldOption("CatType", "") map CategoryType.withName
        def audioProps   = AudioProperties(obj.intField("AudioProps"))
        def videoProps   = VideoProperties(obj.intField("VideoProps"))
        def subtitleType = SubtitleType(obj.intField("SubProps"))
        def isRepeat     = obj.booleanField("Repeat")

        def recStatus    = RecStatus.applyOrUnknown(rec.intField("Status"))
        def recPriority  = rec.intField("Priority")
        def recStartTS   = rec.dateTimeField("StartTs")
        def recEndTS     = rec.dateTimeField("EndTs")
        def chanNum      = ChannelNumber(channel.stringFieldOrElse("ChanNum", ""))
        def callsign     = channel.stringFieldOrElse("CallSign", "")
        def chanName     = channel.stringFieldOrElse("ChannelName", "")
      }
    }
  }

  implicit object ProgramBriefJsonFormat extends RootJsonFormat[ProgramBrief] {
    def write(p: ProgramBrief): JsValue = JsObject(Map(
      "Title"        -> JsString(p.title),
      "SubTitle"     -> JsString(p.subtitle),
      "StartTime"    -> JsString(p.startTime.toString),
      "EndTime"      -> JsString(p.endTime.toString),
      "Category"     -> JsString(p.category),
      "CatType"      -> JsString(p.categoryType.map(_.toString).getOrElse("")),
      "AudioProps"   -> JsString(p.audioProps.id.toString),
      "VideoProps"   -> JsString(p.videoProps.id.toString),
      "SubProps"     -> JsString(p.subtitleType.id.toString),
      "Repeat"       -> JsString(p.isRepeat.toString),
      "Channel"      -> JsObject(Map(
        "ChanId"     -> JsString(p.chanId.id.toString)
      )),
      "Recording"    -> JsObject(Map.empty[String, JsValue]),
      "Artwork"      -> ProgramJsonFormat.writeArtwork(p)
    ))
    def read(value: JsValue): ProgramBrief = {
      val obj = value.asJsObject

      val channel: RichJsonObject =  // inner object
        if (obj.fields contains "Channel") obj.fields("Channel").asJsObject
        else channelContext.value

      val rec: RichJsonObject =      // inner object
        if (obj.fields contains "Recording") obj.fields("Recording").asJsObject
        else EmptyJsonObject

      def statusHasRecording(rs: Int): Boolean = {
        val status = RecStatus.applyOrUnknown(rs)
        status == RecStatus.Recorded || status == RecStatus.Recording
      }

      // test whether result should be a RecordingBrief or RecordableBrief
      if (rec.stringFieldOption("StartTs", "").nonEmpty) {
        if (rec.intFieldOption("RecordedId", 0).nonEmpty)    RecordingBriefJsonFormat.read(value)
        else if (statusHasRecording(rec.intField("Status"))) RecordingBriefJsonFormat.read(value)
        else                                                 RecordableBriefJsonFormat.read(value)
      }
      else new ProgramBrief {
        override def toString: String = s"<JsonProgramBrief $chanId, $startTime: $combinedTitle>"

        def title        = obj.stringField("Title")
        def subtitle     = obj.stringField("SubTitle")
        def chanId       = ChanId(channel.intFieldOrElse("ChanId", 0))
        def startTime    = obj.dateTimeField("StartTime")
        def endTime      = obj.dateTimeField("EndTime")
        def category     = obj.stringField("Category")
        def categoryType = obj.stringFieldOption("CatType", "") map CategoryType.withName
        def audioProps   = AudioProperties(obj.intField("AudioProps"))
        def videoProps   = VideoProperties(obj.intField("VideoProps"))
        def subtitleType = SubtitleType(obj.intField("SubProps"))
        def isRepeat     = obj.booleanField("Repeat")
      }
    }
  }

  implicit object ChannelJsonFormat extends RootJsonFormat[Channel] {
    def write(c: Channel): JsValue = JsObject(Map(
      "ChanId"      -> JsString(c.chanId.id.toString),
      "ChannelName" -> JsString(c.name),
      "ChanNum"     -> JsString(c.number.num),
      "CallSign"    -> JsString(c.callsign),
      "SourceId"    -> JsString(c.sourceId.toString)
    ))
    def read(value: JsValue): Channel = {
      val obj = value.asJsObject
      new Channel {
        def chanId   = ChanId(obj.intField("ChanId"))
        def name     = obj.stringField("ChannelName")
        def number   = ChannelNumber(obj.stringField("ChanNum"))
        def callsign = obj.stringField("CallSign")
        def sourceId = ListingSourceId(0) //ListingSourceId(obj.intField("SourceId"))  not present
      }
    }
  }

  implicit object ChannelDetailsJsonFormat extends RootJsonFormat[ChannelDetails] {
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

  implicit object ChannelGroupJsonFormat extends JsonFormat[ChannelGroup] {
    def write(g: ChannelGroup): JsValue = JsObject(Map(
      "GroupId"  -> JsString(g.groupId.toString),
      "Name"     -> JsString(g.name),
      "Password" -> JsString("")   // not supported by backend, always serialized as empty string for now
    ))
    def read(value: JsValue): ChannelGroup = {
      val obj = value.asJsObject
      new ChannelGroup {
        def groupId  = ChannelGroupId(obj.intField("GroupId"))
        def name     = obj.stringField("Name")
        def channels = Nil  // not included in services serialization
      }
    }
  }

  implicit object RecordRuleJsonFormat extends RootJsonFormat[RecordRule] {
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
      "FindDay"           -> jsonWriter[Option[DayOfWeek]].write(r.findDay),
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
      "Filter"            -> JsString(r.filters.getOrElse(0).toString)
    ))

    def read(value: JsValue): RecordRule = {
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
        def findDay         = obj.fields("FindDay").convertTo[Option[DayOfWeek]]
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
        def filters         = obj.intFieldOption("Filter", 0)
      }
    }
  }

  implicit object RecRuleFilterItemJsonFormat extends JsonFormat[RecRuleFilterItem] {
    def write(f: RecRuleFilterItem): JsValue = JsObject(Map(
      "Id"          -> JsString(f.id.toString),
      "Description" -> JsString(f.name)
    ))
    def read(value: JsValue): RecRuleFilterItem = {
      val obj = value.asJsObject
      new RecRuleFilterItem {
        def id   = obj.intField("Id")
        def name = obj.stringField("Description")
      }
    }
  }

  implicit object TitleInfoJsonFormat extends RootJsonFormat[TitleInfo] {
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

  trait RecordedMarkupJsonFormat[VP <: VideoPosition] extends JsonFormat[RecordedMarkup[VP]] {
    def factory: (Markup, VP) => RecordedMarkup[VP]
    def posfactory: Long => VP

    def write(mk: RecordedMarkup[VP]): JsValue = JsObject(Map(
      "Mark"   -> JsString(mk.tag.id.toString),
      "Offset" -> JsString(mk.position.pos.toString)
    ))

    def read(value: JsValue): RecordedMarkup[VP] = {
      val obj = value.asJsObject
      val tag = Markup.applyOrUnknown(obj.intField("Mark"))
      val pos = posfactory(obj.longField("Offset"))
      factory(tag, pos)
    }
  }

  implicit object RecordedMarkupFrameJsonFormat extends RecordedMarkupJsonFormat[VideoPositionFrame] {
    def factory = (_tag, _pos) => new RecordedMarkupFrame {
      def tag      = _tag
      def position = _pos
    }
    def posfactory = VideoPositionFrame.apply
  }

  implicit object RecordedMarkupMsJsonFormat extends RecordedMarkupJsonFormat[VideoPositionMilliseconds] {
    def factory = (_tag, _pos) => new RecordedMarkupMilliseconds {
      def tag      = _tag
      def position = _pos
    }
    def posfactory = VideoPositionMilliseconds.apply
  }

  implicit object RecordedMarkupBytesJsonFormat extends RecordedMarkupJsonFormat[VideoPositionBytes] {
    def factory = (_tag, _pos) => new RecordedMarkupBytes {
      def tag      = _tag
      def position = _pos
    }
    def posfactory = VideoPositionBytes.apply
  }

  implicit object StorageGroupDirJsonFormat extends RootJsonFormat[StorageGroupDir] {
    def write(sg: StorageGroupDir): JsValue = JsObject(Map(
      "Id"        -> JsString(sg.id.id.toString),
      "GroupName" -> JsString(sg.groupName),
      "HostName"  -> JsString(sg.hostName),
      "DirName"   -> JsString(sg.dirName)
    ))

    def read(value: JsValue): StorageGroupDir = {
      val obj = value.asJsObject
      new StorageGroupDir {
        def id        = StorageGroupId(obj.intField("Id"))
        def groupName = obj.stringField("GroupName")
        def hostName  = obj.stringField("HostName")
        def dirName   = obj.stringField("DirName")
      }
    }
  }

  implicit object RemoteEncoderStateJsonFormat extends RootJsonFormat[RemoteEncoderState] {
    def write(e: RemoteEncoderState): JsValue = JsObject(Map(
      "Id"             -> JsString(e.cardId.toString),
      "HostName"       -> JsString(e.host),
      "Local"          -> JsString(e.local.toString),
      "Connected"      -> JsString(e.connected.toString),
      "LowOnFreeSpace" -> JsString(e.lowFreeSpace.toString),
      "State"          -> JsString(e.state.id.toString),
      "SleepStatus"    -> JsString(e.sleepStatus.id.toString),
      "Recording"      -> writeEmbeddedRecording(e)
    ))

    private def writeEmbeddedRecording(e: RemoteEncoderState): JsValue = e.currentRecording match {
      case Some(rec) => jsonWriter[Recording].write(rec)
      case None      => JsObject(Map.empty[String, JsValue])
    }

    def read(value: JsValue): RemoteEncoderState = {
      val obj = value.asJsObject

      val recObj: RichJsonObject =      // inner object
        if (obj.fields contains "Recording") obj.fields("Recording").asJsObject
        else EmptyJsonObject

      val currentRec: Option[Recording] =
        if (recObj.stringFieldOption("StartTime", "").nonEmpty)
          Some(RecordingJsonFormat.read(obj.fields("Recording")))
        else None

      new RemoteEncoderState {
        def cardId           = CaptureCardId(obj.intField("Id"))
        def host             = obj.stringField("HostName")
        def port             = 0   // NB not serialized in services API

        def local            = obj.booleanField("Local")
        def connected        = obj.booleanField("Connected")
        def lowFreeSpace     = obj.booleanField("LowOnFreeSpace")
        def state            = TvState.applyOrUnknown(obj.intField("State"))
        def sleepStatus      = SleepStatus.applyOrUnknown(obj.intField("SleepStatus"))
        def currentRecording = currentRec
      }
    }
  }

  implicit object InputJsonFormat extends RootJsonFormat[Input] {
    def write(i: Input): JsValue = JsObject(Map(
      "Id"            -> JsString(i.inputId.id.toString),
      "CardId"        -> JsString(i.cardId.id.toString),
      "SourceId"      -> JsString(i.sourceId.id.toString),
      "InputName"     -> JsString(i.name),
      "DisplayName"   -> JsString(i.displayName),
      "RecPriority"   -> JsString(i.recPriority.toString),
      "ScheduleOrder" -> JsString(i.scheduleOrder.toString),
      "LiveTVOrder"   -> JsString(i.liveTvOrder.toString),
      "QuickTune"     -> JsString(i.quickTune.toString)
    ))
    def read(value: JsValue): Input = {
      val obj = value.asJsObject
      new Input {
        def inputId       = InputId(obj.intField("Id"))
        def cardId        = CaptureCardId(obj.intField("CardId"))
        def sourceId      = ListingSourceId(obj.intField("SourceId"))
        def chanId        = None
        def mplexId       = None
        def name          = obj.stringField("InputName")
        def displayName   = obj.stringField("DisplayName")
        def recPriority   = obj.intField("RecPriority")
        def scheduleOrder = obj.intField("ScheduleOrder")
        def liveTvOrder   = obj.intField("LiveTVOrder")
        def quickTune     = obj.booleanField("QuickTune")
      }
    }
  }

  implicit object CaptureCardJsonFormat extends RootJsonFormat[CaptureCard] {
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

  implicit object ListingSourceJsonFormat extends RootJsonFormat[ListingSource] {
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

  implicit object SettingsJsonFormat extends RootJsonFormat[Settings] {
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

  implicit object TimeZoneInfoJsonFormat extends RootJsonFormat[TimeZoneInfo] {
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

  implicit object ChannelGuideBriefJsonFormat extends RootJsonFormat[GuideBriefTuple] {
    def write(tuple: GuideBriefTuple): JsValue = {
      val (chan, progs) = tuple
      JsObject(Map(
        "ChanId"      -> JsString(chan.chanId.id.toString),
        "ChanNum"     -> JsString(chan.number.num),
        "ChannelName" -> JsString(chan.name),
        "CallSign"    -> JsString(chan.callsign),
        "IconURL"     -> JsString(""),  // not in Channel
        "Programs"    -> jsonWriter[List[ProgramBrief]].write(progs.toList)
      ))
    }

    def read(value: JsValue): GuideBriefTuple = {
      val obj = value.asJsObject

      val progs = channelContext.withValue(obj) { obj.fields("Programs").convertTo[List[ProgramBrief]] }
      val chan = new Channel {
        def chanId   = ChanId(obj.intField("ChanId"))
        def name     = obj.stringField("ChannelName")
        def number   = ChannelNumber(obj.stringField("ChanNum"))
        def callsign = obj.stringField("CallSign")
        def sourceId = ListingSourceId(0)  // not serialized in services API w/Details=false
      }
      (chan, progs)
    }
  }

  implicit object ChannelGuideJsonFormat extends RootJsonFormat[GuideTuple] {
    def write(tuple: GuideTuple): JsValue = {
      val (chan, progs) = tuple
      JsObject(Map(
        "ChanId"           -> JsString(chan.chanId.id.toString),
        "ChannelName"      -> JsString(chan.name),
        "ChanNum"          -> JsString(chan.number.num),
        "CallSign"         -> JsString(chan.callsign),
        "SourceId"         -> JsString(chan.sourceId.toString),
        "FrequencyId"      -> JsString(chan.freqId.getOrElse("")),
        "IconURL"          -> JsString(chan.iconPath),
        "FineTune"         -> JsString(chan.fineTune.getOrElse(0).toString),
        "XMLTVID"          -> JsString(chan.xmltvId),
        "Format"           -> JsString(chan.format),
        "Visible"          -> JsString(chan.visible.toString),
        "ChanFilters"      -> JsString(chan.outputFilters.getOrElse("")),
        "UseEIT"           -> JsString(chan.useOnAirGuide.toString),
        "MplexId"          -> JsString(chan.mplexId.map(_.id).getOrElse(0).toString),
        "ServiceId"        -> JsString(chan.serviceId.getOrElse(0).toString),
        "ATSCMajorChan"    -> JsString(chan.atscMajorChan.getOrElse(0).toString),
        "ATSCMinorChan"    -> JsString(chan.atscMinorChan.getOrElse(0).toString),
        "DefaultAuthority" -> JsString(chan.defaultAuthority.getOrElse("")),
        "CommFree"         -> JsString(if (chan.isCommercialFree) "1" else "0'"),
        "Programs"         -> jsonWriter[List[Program]].write(progs.toList)
      ))
    }

    def read(value: JsValue): GuideTuple = {
      val obj = value.asJsObject

      val progs = channelContext.withValue(obj) { obj.fields("Programs").convertTo[List[Program]] }
      val chan = new ChannelDetails {
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
      (chan, progs)
    }
  }

  implicit object VideoJsonFormat extends RootJsonFormat[Video] {
    def write(v: Video): JsValue = JsObject(Map(
      "Id"               -> JsString(v.id.id.toString),
      "Title"            -> JsString(v.title),
      "SubTitle"         -> JsString(v.subtitle),
      "Director"         -> JsString(v.director),
      "Tagline"          -> JsString(v.tagline.getOrElse("")),
      "Description"      -> JsString(v.description),
      "Inetref"          -> JsString(v.inetRef.getOrElse("00000000")),
      "HomePage"         -> JsString(v.homePage.getOrElse("")),
      "Studio"           -> JsString(v.studio.getOrElse("")),
      "Season"           -> JsString(v.season.getOrElse(0).toString),
      "Episode"          -> JsString(v.episode.getOrElse(0).toString),
      "Length"           -> JsString(v.length.map(_.toMinutes).getOrElse(0).toString),
      "PlayCount"        -> JsString(v.playCount.toString),
      "Hash"             -> JsString(v.hash.toString),
      "Visible"          -> JsString(v.visible.toString),
      "FileName"         -> JsString(v.fileName),
      "ContentType"      -> JsString(v.contentType.toString),
      "HostName"         -> JsString(v.hostName),
      "AddDate"          -> JsString(v.addedDate.map(_.toString).getOrElse("")),
      "Watched"          -> JsString(v.watched.toString),
      "UserRating"       -> JsString(v.userRating.toString),
      "Certification"    -> JsString(v.rating),
      "Collectionref"    -> JsString(v.collectionRef.getOrElse(-1).toString),
      "ReleaseDate"      -> JsString(v.releasedDate.map(_.toString).getOrElse(""))
    ))

    def read(value: JsValue): Video = {
      val obj = value.asJsObject
      new Video {
        def id              = VideoId(obj.intField("Id"))
        def title           = obj.stringField("Title")
        def subtitle        = obj.stringField("SubTitle")
        def director        = obj.stringField("Director")
        def year            = None
        def tagline         = obj.stringFieldOption("Tagline", "")
        def description     = obj.stringField("Description")
        def inetRef         = obj.stringFieldOption("Inetref", "00000000")
        def homePage        = obj.stringFieldOption("HomePage", "")
        def studio          = obj.stringFieldOption("Studio", "")
        def season          = obj.intFieldOption("Season", 0)
        def episode         = obj.intFieldOption("Episode", 0)
        def length          = obj.intFieldOption("Length", 0) map (x => Duration.ofMinutes(x))
        def playCount       = obj.intField("PlayCount")
        def hash            = new MythFileHash(obj.stringField("Hash"))
        def visible         = obj.booleanField("Visible")
        def fileName        = obj.stringField("FileName")
        def contentType     = Try(VideoContentType.withName(obj.stringField("ContentType"))) getOrElse VideoContentType.Unknown
        def hostName        = obj.stringField("HostName")
        def addedDate       = obj.dateTimeFieldOption("AddDate") map (_.toInstant)
        def watched         = obj.booleanField("Watched")
        def userRating      = obj.doubleField("UserRating")
        def rating          = obj.stringField("Certification")
        def collectionRef   = obj.intFieldOption("Collectionref", -1)
        def releasedDate    = obj.dateTimeFieldOption("ReleaseDate").map(_.toLocalDateTime().toLocalDate)

        def artworkInfo     = obj.fields("Artwork").asJsObject.fields("ArtworkInfos").convertTo[List[ArtworkInfo]]
      }
    }
  }

  implicit object VideoMultiplexJsonFormat extends RootJsonFormat[VideoMultiplex] {
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
      "UpdateTimeStamp"  -> jsonWriter[Instant].write(m.updatedTimestamp),
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
        def updatedTimestamp = obj.fields("UpdateTimeStamp").convertTo[Instant]
        def defaultAuthority = obj.stringFieldOption("DefaultAuthority", "")
      }
    }
  }

  implicit object LiveStreamJsonFormat extends RootJsonFormat[LiveStream] {
    def write(s: LiveStream): JsValue = JsObject(Map(
      "Id"               -> JsString(s.id.id.toString),
      "Width"            -> JsString(s.width.toString),
      "Height"           -> JsString(s.height.toString),
      "Bitrate"          -> JsString(s.bitrate.toString),
      "AudioBitrate"     -> JsString(s.audioBitrate.toString),
      "SegmentSize"      -> JsString(s.segmentSize.toString),
      "MaxSegments"      -> JsString(s.maxSegments.toString),
      "StartSegment"     -> JsString(s.startSegment.toString),
      "CurrentSegment"   -> JsString(s.currentSegment.toString),
      "SegmentCount"     -> JsString(s.segmentCount.toString),
      "PercentComplete"  -> JsString(s.percentComplete.toString),
      "Created"          -> JsString(s.created.toString),
      "LastModified"     -> JsString(s.lastModified.toString),
      "RelativeURL"      -> JsString(s.relativeUrl),
      "FullURL"          -> JsString(s.fullUrl),
      "StatusStr"        -> JsString(s.statusText),
      "StatusInt"        -> JsString(s.status.id.toString),
      "StatusMessage"    -> JsString(s.statusMessage),
      "SourceFile"       -> JsString(s.sourceFile),
      "SourceHost"       -> JsString(s.sourceHost),
      "SourceWidth"      -> JsString(s.sourceWidth.toString),
      "SourceHeight"     -> JsString(s.sourceHeight.toString),
      "AudioOnlyBitrate" -> JsString(s.audioOnlyBitrate.toString)
    ))

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
        def status           = LiveStreamStatus.applyOrUnknown(obj.intField("StatusInt"))
        def statusText       = obj.stringField("StatusStr")
        def statusMessage    = obj.stringField("StatusMessage")
        def sourceFile       = obj.stringField("SourceFile")
        def sourceHost       = obj.stringField("SourceHost")
        def sourceWidth      = obj.intField("SourceWidth")
        def sourceHeight     = obj.intField("SourceHeight")
        def audioOnlyBitrate = obj.intField("AudioOnlyBitrate")
      }
    }
  }

  implicit object LineupJsonFormat extends RootJsonFormat[Lineup] {
    def write(lu: Lineup): JsValue = JsObject(Map(
      "LineupId"    -> JsString(lu.lineupId),
      "Name"        -> JsString(lu.name),
      "DisplayName" -> JsString(lu.displayName),
      "Type"        -> JsString(lu.lineupType),
      "Postal"      -> JsString(lu.postalCode),
      "Device"      -> JsString(lu.device)
    ))

    def read(value: JsValue): Lineup = {
      val obj = value.asJsObject
      new Lineup {
        def lineupId    = obj.stringField("LineupId")
        def name        = obj.stringField("Name")
        def displayName = obj.stringField("DisplayName")
        def lineupType  = obj.stringField("Type")
        def postalCode  = obj.stringField("Postal")
        def device      = obj.stringField("Device")
      }
    }
  }

  implicit object BlurayInfoJsonFormat extends RootJsonFormat[BlurayInfo] {
    def write(bd: BlurayInfo): JsValue = JsObject(Map(
      "Path"                 -> JsString(bd.path),
      "Title"                -> JsString(bd.title),
      "AltTitle"             -> JsString(bd.altTitle),
      "DiscLang"             -> JsString(bd.discLang),
      "DiscNum"              -> JsString(bd.discNumber.toString),
      "TotalDiscNum"         -> JsString(bd.totalDiscNumber.toString),
      "TitleCount"           -> JsString(bd.titleCount.toString),
      "ThumbCount"           -> JsString(bd.thumbCount.toString),
      "TopMenuSupported"     -> JsString(bd.topMenuSupported.toString),
      "FirstPlaySupported"   -> JsString(bd.firstPlaySupported.toString),
      "NumHDMVTitles"        -> JsString(bd.numHdmvTitles.toString),
      "NumBDJTitles"         -> JsString(bd.numBdJTitles.toString),
      "NumUnsupportedTitles" -> JsString(bd.numUnsupportedTitles.toString),
      "AACSDetected"         -> JsString(bd.aacsDetected.toString),
      "LibAACSDetected"      -> JsString(bd.libaacsDetected.toString),
      "AACSHandled"          -> JsString(bd.aacsHandled.toString),
      "BDPlusDetected"       -> JsString(bd.bdplusDetected.toString),
      "LibBDPlusDetected"    -> JsString(bd.libbdplusDetected.toString),
      "BDPlusHandled"        -> JsString(bd.bdplusHandled.toString)
    ))

    def read(value: JsValue): BlurayInfo = {
      val obj = value.asJsObject
      new BlurayInfo {
        def path                 = obj.stringField("Path")
        def title                = obj.stringField("Title")
        def altTitle             = obj.stringField("AltTitle")
        def discLang             = obj.stringField("DiscLang")
        def discNumber           = obj.intField("DiscNum")
        def totalDiscNumber      = obj.intField("TotalDiscNum")
        def titleCount           = obj.intField("TitleCount")
        def thumbCount           = obj.intField("ThumbCount")
        def thumbPath            = obj.stringField("ThumbPath")
        def topMenuSupported     = obj.booleanField("TopMenuSupported")
        def firstPlaySupported   = obj.booleanField("FirstPlaySupported")
        def numHdmvTitles        = obj.intField("NumHDMVTitles")
        def numBdJTitles         = obj.intField("NumBDJTitles")
        def numUnsupportedTitles = obj.intField("NumUnsupportedTitles")
        def aacsDetected         = obj.booleanField("AACSDetected")
        def libaacsDetected      = obj.booleanField("LibAACSDetected")
        def aacsHandled          = obj.booleanField("AACSHandled")
        def bdplusDetected       = obj.booleanField("BDPlusDetected")
        def libbdplusDetected    = obj.booleanField("LibBDPlusDetected")
        def bdplusHandled        = obj.booleanField("BDPlusHandled")
      }
    }
  }

  implicit object VideoLookupJsonFormat extends RootJsonFormat[VideoLookup] {
    def write(v: VideoLookup): JsValue = JsObject(Map(
      "Title"         -> JsString(v.title),
      "SubTitle"      -> JsString(v.subtitle),
      "Season"        -> JsString(v.season.toString),
      "Episode"       -> JsString(v.episode.getOrElse(0).toString),
      "Year"          -> JsString(v.year.toString),
      "Tagline"       -> JsString(v.tagline),
      "Description"   -> JsString(v.description),
      "Certification" -> JsString(v.certification.getOrElse("")),
      "Inetref"       -> JsString(v.inetRef),
      "Collectionref" -> JsString(v.collectionRef),
      "HomePage"      -> JsString(v.homePage),
      "ReleaseDate"   -> JsString(v.releasedDate.toString),
      "UserRating"    -> JsString(v.userRating.toString),
      "Length"        -> JsString(v.length.toString),
      "Language"      -> JsString(v.language),
      "Countries"     -> jsonWriter[List[String]].write(v.countries),
      "Popularity"    -> JsString(v.popularity.getOrElse(0).toString),
      "Budget"        -> JsString(v.budget.getOrElse(0).toString),
      "Revenue"       -> JsString(v.revenue.toString),
      "IMDB"          -> JsString(v.imdb.getOrElse("")),
      "TMSRef"        -> JsString(v.tmsRef.getOrElse("")),
      "Artwork"       -> jsonWriter[List[ArtworkItem]].write(v.artwork)
    ))

    def read(value: JsValue): VideoLookup = {
      val obj = value.asJsObject
      new VideoLookup {
        def title          = obj.stringField("Title")
        def subtitle       = obj.stringField("SubTitle")
        def season         = obj.intField("Season")
        def episode        = obj.intFieldOption("Episode")
        def year           = obj.intField("Year")
        def tagline        = obj.stringField("Tagline")
        def description    = obj.stringField("Description")
        def certification  = obj.stringFieldOption("Certification", "")
        def inetRef        = obj.stringField("Inetref")
        def collectionRef  = obj.stringField("Collectionref")
        def homePage       = obj.stringField("HomePage")
        def releasedDate   = obj.dateTimeField("ReleaseDate").toInstant
        def userRating     = obj.doubleFieldOption("UserRating", 0)
        def length         = obj.intFieldOption("Length")
        def language       = obj.stringField("Language")
        def countries      = obj.fields("Countries").convertTo[List[String]]
        def popularity     = obj.intFieldOption("Popularity", 0)
        def budget         = obj.intFieldOption("Budget", 0)
        def revenue        = obj.intFieldOption("Revenue", 0)
        def imdb           = obj.stringFieldOption("IMDB", "")
        def tmsRef         = obj.stringFieldOption("TMSRef", "")
        def artwork        = obj.fields("Artwork").convertTo[List[ArtworkItem]]
      }
    }
  }

  implicit object MythTvVersionInfoJsonFormat extends JsonFormat[MythTvVersionInfo] {
    def write(v: MythTvVersionInfo): JsValue = JsObject(Map(
      "Version"  -> JsString(v.fullVersion),
      "Branch"   -> JsString(v.branch),
      "Protocol" -> JsString(v.protocol),
      "Binary"   -> JsString(v.binary),
      "Schema"   -> JsString(v.schema)
    ))
    def read(value: JsValue): MythTvVersionInfo = {
      val obj = value.asJsObject
      new MythTvVersionInfo {
        def fullVersion = obj.stringField("Version")
        def branch      = obj.stringField("Branch")
        def protocol    = obj.stringField("Protocol")
        def binary      = obj.stringField("Binary")
        def schema      = obj.stringField("Schema")
      }
    }
  }

  implicit object DatabaseConnectionInfoJsonFormat extends JsonFormat[DatabaseConnectionInfo] {
    def write(d: DatabaseConnectionInfo): JsValue = JsObject(Map(
      "Host"          -> JsString(d.host),
      "Port"          -> JsString(d.port.toString),
      "Ping"          -> JsString(d.ping.toString),
      "UserName"      -> JsString(d.userName),
      "Password"      -> JsString(d.password),
      "Name"          -> JsString(d.dbName),
      "Type"          -> JsString(d.driver),
      "LocalEnabled"  -> JsString(d.localEnabled.toString),
      "LocalHostName" -> JsString(d.localHostName)
    ))
    def read(value: JsValue): DatabaseConnectionInfo = {
      val obj = value.asJsObject
      new DatabaseConnectionInfo {
        def host          = obj.stringField("Host")
        def port          = obj.intField("Port")
        def ping          = obj.booleanField("Ping")
        def userName      = obj.stringField("UserName")
        def password      = obj.stringField("Password")
        def dbName        = obj.stringField("Name")
        def driver        = obj.stringField("Type")
        def localEnabled  = obj.booleanField("LocalEnabled")
        def localHostName = obj.stringField("LocalHostName")
      }
    }
  }

  implicit object WakeOnLanInfoJsonFormat extends JsonFormat[WakeOnLanInfo] {
    def write(w: WakeOnLanInfo): JsValue = JsObject(Map(
      "Enabled"   -> JsString(w.enabled.toString),
      "Reconnect" -> JsString(w.reconnect.toString),
      "Retry"     -> JsString(w.retry.toString),
      "Command"   -> JsString(w.command)
    ))
    def read(value: JsValue): WakeOnLanInfo = {
      val obj = value.asJsObject
      new WakeOnLanInfo {
        def enabled   = obj.booleanField("Enabled")
        def reconnect = obj.intField("Reconnect")
        def retry     = obj.intField("Retry")
        def command   = obj.stringField("Command")
      }
    }
  }

  implicit object BackendDetailsJsonFormat extends RootJsonFormat[BackendDetails] {
    def write(b: BackendDetails): JsValue = JsObject(Map(
      "Build" -> JsObject(Map(
        "Version"   -> JsString(b.fullVersion),
        "LibX264"   -> JsString(b.hasLibX264.toString),
        "LibDNS_SD" -> JsString(b.hasLibDnsSd.toString)
      )),
      "Env"   -> JsObject(b.environment mapValues (JsString(_))),
      "Log"   -> JsObject(Map("LogArgs" -> JsString(b.logArgs)))
    ))
    def read(value: JsValue): BackendDetails = {
      val obj = value.asJsObject
      val bld = obj.fields("Build").asJsObject
      val log = obj.fields("Log").asJsObject
      val env = obj.fields("Env").asJsObject.fields mapValues { case JsString(s) => s ; case _ => "" }
      new BackendDetails {
        def fullVersion = bld.stringField("Version")
        def hasLibX264  = bld.booleanField("LibX264")
        def hasLibDnsSd = bld.booleanField("LibDNS_SD")
        def logArgs     = log.stringField("LogArgs")
        def environment = env
      }
    }
  }

  implicit object ConnectionInfoJsonFormat extends RootJsonFormat[ConnectionInfo] {
    def write(c: ConnectionInfo): JsValue = JsObject(Map(
      "Version" -> jsonWriter[MythTvVersionInfo].write(c.version),
      "Databse" -> jsonWriter[DatabaseConnectionInfo].write(c.database),
      "WOL"     -> jsonWriter[WakeOnLanInfo].write(c.wakeOnLan)
    ))
    def read(value: JsValue): ConnectionInfo = {
      val obj = value.asJsObject
      new ConnectionInfo {
        def version   = obj.fields("Version").convertTo[MythTvVersionInfo]
        def database  = obj.fields("Database").convertTo[DatabaseConnectionInfo]
        def wakeOnLan = obj.fields("WOL").convertTo[WakeOnLanInfo]
      }
    }
  }

  implicit object KnownFrontendJsonFormat extends RootJsonFormat[KnownFrontendInfo] {
    def write(f: KnownFrontendInfo): JsValue = JsObject(Map(
      "Name"    -> JsString(f.hostName),
      "IP"      -> JsString(f.addresses.headOption.map(_.getHostAddress).getOrElse("")),
      "Port"    -> JsString(f.servicesPort.toString),
      "OnLine"  -> JsString(if (f.online) "1" else "0")
    ))
    def read(value: JsValue): KnownFrontendInfo = {
      val obj = value.asJsObject
      new KnownFrontendInfo {
        def hostName          = obj.stringField("Name")
        def addresses         = List(InetAddress.getByName(obj.stringField("IP")))
        def servicesPort      = obj.intField("Port")
        def remoteControlPort = 0   // not included in the service result
        def online            = obj.intField("OnLine") != 0
      }
    }
  }

  implicit object LabelValueJsonFormat extends JsonFormat[LabelValue] {
    def write(v: LabelValue): JsValue = JsObject(Map(
      "Label" -> JsString(v.label),
      "Value" -> JsString(v.value)
    ))
    def read(value: JsValue): LabelValue = {
      val obj = value.asJsObject
      new LabelValue {
        def label = obj.stringField("Label")
        def value = obj.stringField("Value")
      }
    }
  }

  implicit object LogMessageJsonFormat extends JsonFormat[LogMessage] {
    def write(m: LogMessage): JsValue = JsObject(Map(
      "HostName"    -> JsString(m.hostName),
      "Application" -> JsString(m.application),
      "PID"         -> JsString(m.pid.toString),
      "TID"         -> JsString(m.tid.toString),
      "Thread"      -> JsString(m.thread.toString),
      "Filename"    -> JsString(m.fileName),
      "Line"        -> JsString(m.lineNum.toString),
      "Function"    -> JsString(m.function),
      "Time"        -> JsString(m.messageTime.toString),
      "Level"       -> jsonWriter[MythLogLevel].write(m.level),
      "Message"     -> JsString(m.message)
    ))
    def read(value: JsValue): LogMessage = {
      val obj = value.asJsObject
      new LogMessage {
        def hostName    = obj.stringField("HostName")
        def application = obj.stringField("Application")
        def pid         = obj.intField("PID")
        def tid         = obj.intField("TID")
        def thread      = obj.stringField("Thread")
        def fileName    = obj.stringField("Filename")
        def lineNum     = obj.intField("Line")
        def function    = obj.stringField("Function")
        def messageTime = obj.dateTimeField("Time").toInstant
        def level       = obj.fields("Level").convertTo[MythLogLevel]
        def message     = obj.stringField("Message")
      }
    }
  }

  implicit object ImageSyncStatusJsonFormat extends RootJsonFormat[ImageSyncStatus] {
    def write(s: ImageSyncStatus): JsValue = JsObject(Map(
      "Running" -> JsString(s.running.toString),
      "Current" -> JsString(s.progress.toString),
      "Total"   -> JsString(s.total.toString)
    ))
    def read(value: JsValue): ImageSyncStatus = {
      val obj = value.asJsObject
      ImageSyncStatus(
        obj.booleanField("Running"),
        obj.intField("Current"),
        obj.intField("Total")
      )
    }
  }

  implicit object GuideBriefJsonFormat extends ServicesObjectGuideJsonFormat[Channel, ProgramBrief] {
    override def writeData(g: Guide[Channel, ProgramBrief]): JsValue =
      jsonWriter[List[GuideBriefTuple]].write(g.programs.toList)

    override def writeExtraFields(obj: ServicesObject[Guide[Channel, ProgramBrief]]): Map[String, JsValue] =
      Map("Details" -> JsString("false")) ++ super.writeExtraFields(obj)

    def readChannelGuide(obj: JsValue): Map[Channel, Seq[ProgramBrief]] = {
      // TODO OPTIMIZE avoid intermediate conversion to list
      val channelGuideList = obj.convertTo[List[GuideBriefTuple]]
      val channelGuide = channelGuideList.toMap
      channelGuide
    }
  }

  implicit object GuideJsonFormat extends ServicesObjectGuideJsonFormat[ChannelDetails, Program] {
    def writeData(g: Guide[ChannelDetails, Program]): JsValue =
      jsonWriter[List[GuideTuple]].write(g.programs.toList)

    override def writeExtraFields(obj: ServicesObject[Guide[ChannelDetails, Program]]): Map[String, JsValue] =
      Map("Details" -> JsString("true")) ++ super.writeExtraFields(obj)

    def readChannelGuide(obj: JsValue): Map[ChannelDetails, Seq[Program]] = {
      obj.convertTo[List[GuideTuple]].toMap
    }
  }
}
