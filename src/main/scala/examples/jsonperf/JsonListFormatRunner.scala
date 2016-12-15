package examples.jsonperf

import java.io._

import org.scalameter._
import spray.json.{ JsValue, JsonParser }
import mythtv.model._
import mythtv.services._
import mythtv.connection.http.json.BackendJsonProtocol

/*
object JsonListFormatRunner extends BackendJsonProtocol {
  final val inputFile = "/home/tgrigg/program-guide.json"

  val standardConfig = config(
    Key.exec.minWarmupRuns -> 5,
    //Key.exec.maxWarmupRuns -> 10,
    Key.exec.benchRuns -> 10,
    Key.verbose -> true
  ) withWarmer new Warmer.Default

  def main(args: Array[String]): Unit = {
    val json = loadJsonFile(inputFile)
    val time = standardConfig measure {
      doDeserialize(json)
    }
    println("Deserialization time: " + time)
  }

  def doDeserialize(value: JsValue): Unit = {
    var n: Int = 0
    while (n < 1000000) {
      value.convertTo[ServicesObject[Guide[ChannelDetails, Program]]]
      n += 1
    }
  }

  def loadJsonFile(fileName: String): JsValue = {
    parseJson(new FileInputStream(new File(fileName)))
  }

  private def parseJson(stream: InputStream): JsValue = {
    val reader = new BufferedReader(new InputStreamReader(stream))
    val writer = new StringWriter()

    var line: String = null
    do {
      line = reader.readLine()
      if (line != null) writer.write(line)
    } while (line != null)

    stream.close()

    JsonParser(writer.toString)
  }
}
*/
