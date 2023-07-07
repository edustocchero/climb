ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.0"

val AkkaVersion = "2.8.0"
val AkkaHttpVersion = "10.5.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
)

lazy val root = (project in file("."))
  .settings(
    name := "climb",
    idePackagePrefix := Some("com.github.edustocchero.climb")
  )
