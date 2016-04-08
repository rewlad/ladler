
fork in test := true

// libraryDependencies += "org.deephacks.lmdbjni" % "lmdbjni-linux64" % "0.4.6" % Test

libraryDependencies += "org.deephacks.lmdbjni" % "lmdbjni-linux64" % "0.4.1" % Test

val runL = TaskKey[Unit]("runL")

runL := {
  println("A")
  val cp = (fullClasspath in Test).value.files.map(f⇒f.getPath)
  val cpStr = cp.mkString(s"${java.io.File.pathSeparatorChar}")
  println(cpStr)
  ("java" :: "-cp" :: cpStr :: "ee.cone.base.test_loots.TestApp" :: Nil) !
}