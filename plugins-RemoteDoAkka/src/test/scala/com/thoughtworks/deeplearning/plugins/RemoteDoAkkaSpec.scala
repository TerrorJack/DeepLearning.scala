package com.thoughtworks.deeplearning.plugins

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.thoughtworks.future._
import com.thoughtworks.raii.asynchronous._
import com.thoughtworks.feature.Factory
import com.typesafe.config.ConfigFactory

object RemoteDoAkkaSpec extends App with Serializable {
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

  def config = ConfigFactory.parseString("""
      |akka {
      |  actor {
      |    provider = remote
      |  }
      |  remote {
      |    enabled-transports = ["akka.remote.netty.tcp"]
      |    netty.tcp {
      |      hostname = "127.0.0.1"
      |      port = 2552
      |    }
      | }
      |}
    """.stripMargin)

  def system = ActorSystem("RemoteDoAkkaSpecActorSystem", config)

  assert("Expression") {

    val specSystem = system

    try {
      val remoteDoAkka = Factory[RemoteDoAkka].newInstance(akkaActorSystem = specSystem)

      import specSystem.dispatcher

      def hyperparameters = Factory[Builtins].newInstance()

      remoteDoAkka
        .runDo {
          val hp = hyperparameters
          import hp.implicits._
          val two = hp.DoubleLayer(2.0.forward)
          val three = hp.DoubleLayer(3.0.forward)
          (two * three).forward
        }
        .run
        .blockingAwait
        .data == 6.0
    } finally {
      specSystem.terminate
    }

  }
}
