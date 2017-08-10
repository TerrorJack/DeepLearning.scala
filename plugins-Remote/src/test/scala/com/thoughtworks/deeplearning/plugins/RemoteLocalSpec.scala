package com.thoughtworks.deeplearning.plugins

import scala.collection.mutable.ArrayBuffer
import org.scalatest._

object RemoteLocalSpec {
  val messageLog = ArrayBuffer.empty[String]

  trait Clef extends RemoteLocal {
    override def onReceive(message: Any): Unit = {
      messageLog.append(message.toString)
    }
  }

}

class RemoteLocalSpec extends FreeSpec {

  import RemoteLocalSpec._

  "RemoteLocal" in {

    import com.thoughtworks.feature.Factory

    val clef = Factory[Clef].newInstance()

    clef.send(clef.anyAgent, "YOLO")

    assert(messageLog == ArrayBuffer("YOLO"))
  }
}
