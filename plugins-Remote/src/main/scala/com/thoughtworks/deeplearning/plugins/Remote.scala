package com.thoughtworks.deeplearning.plugins

trait Remote {
  type RemoteAgentRef

  def anyAgent: RemoteAgentRef

  def send(receiver: RemoteAgentRef, message: Any): Unit

  def onReceive(message: Any): Unit
}
