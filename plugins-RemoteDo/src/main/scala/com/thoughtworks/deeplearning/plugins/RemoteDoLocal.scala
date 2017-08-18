package com.thoughtworks.deeplearning.plugins

import java.io.{ObjectOutputStream, ByteArrayOutputStream, ObjectInputStream, ByteArrayInputStream}
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.parallel.mutable.ParHashMap
import scala.util.Try
import scalaz.syntax.all._
import com.thoughtworks.tryt.covariant._
import com.thoughtworks.continuation._
import com.thoughtworks.raii.covariant._
import com.thoughtworks.raii.asynchronous._

trait RemoteDoLocal extends RemoteDo with Serializable {
  protected type RemoteDoRef = Int

  @transient
  protected lazy val remoteDoRefSupply = new AtomicInteger(0)

  @transient
  protected lazy val doConstructorMap: ParHashMap[RemoteDoRef, Array[Byte]] = ParHashMap.empty


  override def runDo[A](doA: => Do[A]): Do[A] = {
    val remoteDoRef = remoteDoRefSupply.getAndIncrement
    val byteArrayOutputStream = new ByteArrayOutputStream
    new ObjectOutputStream(byteArrayOutputStream).writeObject(doA _)
    doConstructorMap.update(remoteDoRef, byteArrayOutputStream.toByteArray)
    Do.delay {
      val recoveredConstructor =
        new ObjectInputStream(new ByteArrayInputStream(doConstructorMap(remoteDoRef))).readObject
          .asInstanceOf[Function0[Do[A]]]
      val Do(TryT(ResourceT(recoveredCont))) = recoveredConstructor()
      Do(TryT(ResourceT(recoveredCont.map { recoveredResource =>
        new Resource[UnitContinuation, Try[A]] {
          override def value = recoveredResource.value
          override def release = recoveredResource.release.map { _ =>
            {
              doConstructorMap -= remoteDoRef
            }
          }
        }
      })))
    }.join
  }

  protected def writeReplace(): Any = {
    RemoteDoLocalProxy
  }
}

case object RemoteDoLocalProxy extends Serializable {
  protected def readResolve(): Any = {
    new AnyRef with RemoteDoLocal
  }
}
