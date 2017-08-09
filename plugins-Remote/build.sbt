libraryDependencies += "com.thoughtworks.feature" %% "partialapply" % "2.0.1"

libraryDependencies += "com.thoughtworks.feature" %% "implicitapply" % "2.0.1"

libraryDependencies += "com.thoughtworks.feature" %% "factory" % "2.0.1"

enablePlugins(Example)

exampleSuperTypes ~= { oldExampleSuperTypes =>
  import oldExampleSuperTypes._
  updated(indexOf("_root_.org.scalatest.FreeSpec"), "_root_.org.scalatest.FreeSpec")
}
