package com.thoughtworks.deeplearning.plugins

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.SyncVar
import scala.collection.parallel.mutable.ParHashMap
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.{BalancingPool, Router}
import com.thoughtworks.future._
import com.thoughtworks.raii.asynchronous._

private class RemoteDoActor(context: RemoteDoAkka) extends Actor {
  import RemoteDoAkka._

  def receive = {
    case IndexedDoConstructor(doConstructor, ref) =>
      val result = doConstructor().run.blockingAwait
      sender() ! IndexedResult(result, ref)

    case IndexedResult(result, ref) =>
      context.remoteDoResultMap(ref).put(result)
  }
}

object RemoteDoAkka {
  type RemoteDoRef = Int

  final case class IndexedDoConstructor[A](doConstructor: () => Do[A], ref: RemoteDoRef)
  final case class IndexedResult[A](result: A, ref: RemoteDoRef)
}

trait RemoteDoAkka extends RemoteDo {

  import RemoteDoAkka._

  protected val remoteDoRefSupply = new AtomicInteger(0)

  // For communication between RemoteDoActor. Should really be protected, but..
  val remoteDoResultMap: ParHashMap[RemoteDoRef, SyncVar[Any]] = ParHashMap.empty

  val akkaActorSystem: ActorSystem

  protected val balancingPool = BalancingPool(8)

  protected lazy val localRemoteDoActor: ActorRef =
    akkaActorSystem.actorOf(Props(new RemoteDoActor(this)), "localRemoteDoActor")

  protected lazy val balancingPoolRouter: Router =
    balancingPool.createRouter(akkaActorSystem).addRoutee(localRemoteDoActor)

  override def runDo[A](doA: => Do[A]): Do[A] = {
    val ref = remoteDoRefSupply.getAndIncrement
    remoteDoResultMap.update(ref, new SyncVar())
    balancingPoolRouter.route(IndexedDoConstructor(doA _, ref), localRemoteDoActor)
    Do.delay {
      val result = remoteDoResultMap(ref).take
      remoteDoResultMap.remove(ref)
      result.asInstanceOf[A]
    }
  }

}
