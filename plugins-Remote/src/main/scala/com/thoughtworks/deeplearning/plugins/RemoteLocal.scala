package com.thoughtworks.deeplearning.plugins

trait RemoteLocal extends Remote {

  import shapeless.Witness
  import com.thoughtworks.feature.Factory.inject
  import com.thoughtworks.feature.{Factory, PartialApply, ImplicitApply}

  override type RemoteAgentRef = Int
  type RemoteLocalAgent <: RemoteLocalAgentApi with RemoteAgent

  trait RemoteLocalAgentApi extends RemoteAgentApi {
    this: RemoteLocalAgent =>

    import java.io.{ByteArrayInputStream, ObjectInputStream, ByteArrayOutputStream, ObjectOutputStream}

    protected val selfAgentRef: RemoteAgentRef
    protected val parentSession: RemoteLocalSession

    override def send(receiver: RemoteAgentRef, message: RemoteMessage): Unit = {
      parentSession.agentMap.get(receiver) match {
        case Some(realReceiver) =>
          val bos = new ByteArrayOutputStream()
          new ObjectOutputStream(bos).writeObject(message)
          realReceiver.onReceiveByteArray(selfAgentRef, bos.toByteArray)
        case None => sys.error("Impossible happened in RemoteLocalAgentApi.send")
      }
    }

    protected def onReceiveByteArray(sender: RemoteAgentRef, buf: Array[Byte]): Unit = {
      onReceive(sender, new ObjectInputStream(new ByteArrayInputStream(buf)).readObject().asInstanceOf[RemoteMessage])
    }

    override def onReceive(sender: RemoteAgentRef, message: RemoteMessage): Unit = {}
  }

  @inject
  protected val remoteLocalAgentFactory: Factory[RemoteLocalAgent]

  @inject
  protected val remoteLocalAgentPartialApplySelfAgentRef: PartialApply[remoteLocalAgentFactory.Constructor,
                                                                       Witness.`"selfAgentRef"`.T]

  @inject
  protected val remoteLocalAgentSelfAgentRefParameter: RemoteAgentRef <:< remoteLocalAgentPartialApplySelfAgentRef.Parameter

  @inject
  protected val remoteLocalAgentPartialApplyParentSession: PartialApply[remoteLocalAgentPartialApplySelfAgentRef.Rest,
                                                                        Witness.`"parentSession"`.T]

  @inject
  protected val remoteLocalAgentParentSessionParameter: RemoteLocalSession <:< remoteLocalAgentPartialApplyParentSession.Parameter

  @inject
  protected val remoteLocalAgentImplicitApply: ImplicitApply[remoteLocalAgentPartialApplyParentSession.Rest]

  @inject
  protected val remoteLocalAgentImplicitApplyOut: remoteLocalAgentImplicitApply.Out <:< RemoteLocalAgent

  type RemoteLocalSession <: RemoteLocalSessionApi with RemoteSession

  trait RemoteLocalSessionApi extends RemoteSessionApi {
    this: RemoteLocalSession =>

    import java.util.concurrent.atomic.AtomicInteger
    import scala.collection.parallel.mutable.ParHashMap

    override type RemoteAgentConf = Unit
    val agentMap: ParHashMap[RemoteAgentRef, RemoteLocalAgent] = ParHashMap.empty
    val agentCounter = new AtomicInteger(0)

    override def newAgent(conf: RemoteAgentConf): RemoteAgentRef = {
      val agentId = agentCounter.getAndIncrement()
      val agent = remoteLocalAgentImplicitApplyOut(
        remoteLocalAgentImplicitApply(
          remoteLocalAgentPartialApplyParentSession(
            remoteLocalAgentPartialApplySelfAgentRef(remoteLocalAgentFactory.newInstance,
                                                     remoteLocalAgentSelfAgentRefParameter(agentId)),
            remoteLocalAgentParentSessionParameter(this)
          )))
      agentMap.update(agentId, agent)
      agentId
    }

    override def getAgent(agent: RemoteAgentRef): RemoteLocalAgent = {
      agentMap.get(agent) match {
        case Some(realAgent) => realAgent
        case None            => sys.error("Impossible happened in RemoteLocalSessionApi.getAgent")
      }
    }

    override def killAgent(agent: RemoteAgentRef): Unit = {
      agentMap.remove(agent)
    }
  }

  @inject
  protected val remoteLocalSessionFactory: Factory[RemoteLocalSession]

  object RemoteLocalSession {
    def apply[Out <: RemoteLocalSession]()(
        implicit implicitApply: ImplicitApply.Aux[remoteLocalSessionFactory.Constructor, Out]
    ): Out = {
      implicitApply(remoteLocalSessionFactory.newInstance)
    }
  }

}
