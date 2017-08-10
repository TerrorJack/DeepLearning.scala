package com.thoughtworks.deeplearning.plugins

trait RemoteLocal extends Remote {

  import java.io.{ByteArrayInputStream, ObjectInputStream, ByteArrayOutputStream, ObjectOutputStream}

  override type RemoteAgentRef = Unit

  override def anyAgent: RemoteAgentRef = Unit

  override def send(receiver: RemoteAgentRef, message: Any): Unit = {
    val bos = new ByteArrayOutputStream()
    new ObjectOutputStream(bos).writeObject(message)
    onReceiveBuf(bos.toByteArray)
  }

  protected def onReceiveBuf(buf: Array[Byte]): Unit = {
    onReceive(new ObjectInputStream(new ByteArrayInputStream(buf)).readObject())
  }

  override def onReceive(message: Any): Unit = {}
}
