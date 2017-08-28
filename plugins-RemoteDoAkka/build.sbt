libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.4"

libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.5.4"

libraryDependencies += "com.thoughtworks.each" %% "each" % "3.3.1"

import java.io.FileWriter

lazy val s = taskKey[Unit]("")

s := {
  val buf = new StringBuilder
  val seq = (fullClasspath in Runtime).value.files
  buf.append("interp.load.cp(Seq(ammonite.ops.Path(\"")
  buf.append(seq.head)
  buf.append("\")")
  seq.tail.foreach { f =>
    val p = f.getAbsolutePath
    buf.append(", ammonite.ops.Path(\"")
    buf.append(p)
    buf.append("\")")
  }
  buf.append("))")
  val bw = new FileWriter("jupyter-script.sc")
  bw.write(buf.result)
  bw.close()
}
