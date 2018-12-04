name := "final"

version := "0.1"

scalaVersion := "2.12.7"

lazy val akkaVersion = "2.5.18"
lazy val akkaHttpVersion = "10.1.5"

libraryDependencies ++= Seq(
  // aws s3 SDK
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.452",
  // akka actors
  "com.typesafe.akka" %% "akka-actor"   % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  // akka stream
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  // akka http
  "com.typesafe.akka" %% "akka-http"         % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  // spray json for akka-http
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
)