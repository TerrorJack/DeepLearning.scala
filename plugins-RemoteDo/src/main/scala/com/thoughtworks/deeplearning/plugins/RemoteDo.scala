package com.thoughtworks.deeplearning.plugins

import com.thoughtworks.raii.asynchronous._

trait RemoteDo {
  def runDo[A](doA: => Do[A]): Do[A]
}
