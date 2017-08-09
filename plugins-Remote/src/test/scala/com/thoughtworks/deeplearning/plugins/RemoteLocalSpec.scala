package com.thoughtworks.deeplearning.plugins

import scala.collection.mutable.ArrayBuffer
import org.scalatest._

object RemoteLocalSpec {

  val messageLog = ArrayBuffer.empty[(Int, Int, String)]

  trait Clef extends RemoteLocal {
    override type RemoteLocalAgent <: RemoteLocalAgentApi with RemoteAgent

    trait RemoteLocalAgentApi extends super.RemoteLocalAgentApi {
      this: RemoteLocalAgent =>
      override type RemoteMessage = String

      override def onReceive(sender: RemoteAgentRef, message: RemoteMessage): Unit = {
        messageLog.append((sender, selfAgentRef, message))
      }
    }

  }

}

class RemoteLocalSpec extends FreeSpec {

  import RemoteLocalSpec._

  "RemoteLocal" in {

    import com.thoughtworks.feature.Factory

    val clef = Factory[Clef].newInstance()
    val session = clef.RemoteLocalSession(8)

    val x = session.anyAgent
    val xa = session.getAgent(x)
    val y = session.anyAgent
    val ya = session.getAgent(y)

    xa.send(y, "YOLO")

    assert(messageLog == ArrayBuffer((x, y, "YOLO")))
  }
}
