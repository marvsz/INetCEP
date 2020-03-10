name := "fraudDetectionSBT"

version := "0.1"

scalaVersion := "2.12.0"

libraryDependencies ++= Seq(
  "org.apache.flink" %% "flink-scala" % "1.10.0",
  "org.apache.flink" %% "flink-clients" % "1.10.0",
  "org.apache.flink" %% "flink-walkthrough-common" % "1.10.0",
  "org.apache.flink" %% "flink-streaming-scala" % "1.10.0",
  "org.apache.logging.log4j" % "log4j-api" % "2.13.1",
  "org.apache.logging.log4j" % "log4j-core" % "2.13.1",
  "org.slf4j" % "slf4j-jdk14" % "1.7.25"
)