enablePlugins(JmhPlugin)

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.iravid",
      scalaVersion := "2.12.4",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "transformer-benchmarks",
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4"),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "1.0.1",
      "org.scalaz" %% "scalaz-core" % "7.3.0-M19"
    ),
    scalacOptions += "-Ypartial-unification"
  )
