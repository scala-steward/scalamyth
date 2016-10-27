package mythtv
package util

import org.scalatest.FunSuite

class MythFileHashSuite extends FunSuite {
  test("Hash of empty buffer equals 'NULL'") {
    assert(MythFileHash(new Array[Byte](0)) === "NULL")
  }

  test("Hash of zero'd array of size 1 equals '1'") {
    assert(MythFileHash(new Array[Byte](1)) === "1")
  }
}

/*object HashTest {
  import java.io.{ File, FileInputStream }
  def readFile(filename: String): Array[Byte] = {
    val file = new File(filename)
    val len = file.length.toInt
    val buf = new Array[Byte](len)
    val stream = new FileInputStream(file)

    var n, off = 0
    do {
      n = stream.read(buf, off, len - off)
      off += n
    } while (n > 0)

    println(s"read $off bytes")

    stream.close()
    buf
 }

  def main(args: Array[String]): Unit = {
    /*
    val data = readFile(args(0))
    println("0x" + MythFileHash(data))
     */
    val path = Paths.get(args(0))
    println("0x" + MythFileHash(path))
  }
}
 */
