package com.thoughtworks.deeplearning.plugins

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

import scalaz.syntax.all._
import com.thoughtworks.raii.asynchronous._
import com.thoughtworks.feature.Factory.inject

trait RemoteDoAkka extends RemoteDo {

  protected val remoteDoRefSupply = new AtomicInteger(0)

  val akkaActorSystem: ActorSystem

  override def runDo[A](doA: => Do[A]): Do[A] = {
    Do.execute(doA)(akkaActorSystem.dispatcher).join
  }

}
