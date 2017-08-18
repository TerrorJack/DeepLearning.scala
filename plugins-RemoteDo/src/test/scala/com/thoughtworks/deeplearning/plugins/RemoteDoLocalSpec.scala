package com.thoughtworks.deeplearning.plugins

import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest._
import scalaz.syntax.all._
import com.thoughtworks.future._
import com.thoughtworks.raii.asynchronous._
import com.thoughtworks.feature.Factory
import com.thoughtworks.deeplearning.plugins._

object RemoteDoLocalSpec extends App with Serializable {
  sys.props("sun.io.serialization.extendedDebugInfo") = "true"

  def assert(tag: String)(cond: => Boolean): Unit = {
    try {
      if (cond)
        println(s"Test $tag passed")
      else
        Console.err.println(s"Test $tag failed, assertion doesn't hold")
    } catch {
      case e: Exception =>
        Console.err.println(s"""
             |Test $tag failed, occured exception:
             |$e
             |Stack trace:""".stripMargin)
        e.getStackTrace.foreach { frame =>
          Console.err.println(frame)
        }
    }
  }

  assert("Trivial") {
    val remoteDoLocal = Factory[RemoteDoLocal].newInstance()
    remoteDoLocal.runDo(Do.now(1 + 1)).run.blockingAwait == 2
  }

  assert("Zero layer") {
    val remoteDoLocal = Factory[RemoteDoLocal].newInstance()
    remoteDoLocal
      .runDo {
        val hyperparameters = Factory[Builtins].newInstance()
        import hyperparameters.implicits._
        hyperparameters.DoubleLayer(0.0.forward).forward
      }
      .run
      .blockingAwait
      .data == 0.0
  }

  assert("Zero layer, hyperparameters lifted") {
    val remoteDoLocal = Factory[RemoteDoLocal].newInstance()

    lazy val hyperparameters = Factory[Builtins].newInstance()

    remoteDoLocal
      .runDo {
        import hyperparameters.implicits._
        hyperparameters.DoubleLayer(0.0.forward).forward
      }
      .run
      .blockingAwait
      .data == 0.0
  }

  assert("Zero layer lifted") {
    val remoteDoLocal = Factory[RemoteDoLocal].newInstance()

    def hyperparameters = Factory[Builtins].newInstance()

    lazy val zeroLayer = {
      val hp = hyperparameters
      import hp.implicits._
      hp.DoubleLayer(0.0.forward)
    }

    remoteDoLocal.runDo(zeroLayer.forward).run.blockingAwait.data == 0.0
  }

  assert("Non-constant layer") {
    val remoteDoLocal = Factory[RemoteDoLocal].newInstance()

    lazy val hyperparameters = Factory[Builtins].newInstance()

    lazy val sixLayer = {
      import hyperparameters.implicits._
      val two = hyperparameters.DoubleLayer(2.0.forward)
      val three = hyperparameters.DoubleLayer(3.0.forward)
      two * three
    }

    remoteDoLocal.runDo(sixLayer.forward).run.blockingAwait.data == 6.0
  }

  assert("Transient problem replay") {
    val remoteDoLocal = Factory[RemoteDoLocal].newInstance()
    object O {
      def delayed = Do.now(true)
    }
    import O._
    delayed

    remoteDoLocal.runDo(delayed).run.blockingAwait
  }
}
