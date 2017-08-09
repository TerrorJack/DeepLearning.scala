package com.thoughtworks.deeplearning.plugins

trait Remote {
  type RemoteAgentRef
  type RemoteAgent <: RemoteAgentApi

  trait RemoteAgentApi {
    type RemoteMessage

    def send(receiver: RemoteAgentRef, message: RemoteMessage): Unit

    def onReceive(sender: RemoteAgentRef, message: RemoteMessage): Unit
  }

  type RemoteSession <: RemoteSessionApi

  trait RemoteSessionApi {
    type RemoteAgentConf

    def newAgent(conf: RemoteAgentConf): RemoteAgentRef

    def getAgent(agent: RemoteAgentRef): RemoteAgent

    def killAgent(agent: RemoteAgentRef): Unit
  }

}
