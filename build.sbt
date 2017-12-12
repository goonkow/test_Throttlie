
name := "test_Throttlie"

scalaVersion := "2.11.8"

lazy val root = (project in file(".")).settings(
  commonSettings
)

lazy val commonSettings = Seq(
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.0.11",
// Only when running against Akka 2.5 explicitly depend on akka-streams in same version as akka-actor
  "com.typesafe.akka" %% "akka-stream" % "2.5.7", // or whatever the latest version is
  "com.typesafe.akka" %% "akka-actor"  % "2.5.7",// or whatever the latest version is
  "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.2.2" % "test",
  "io.gatling"            % "gatling-test-framework"    % "2.2.2" % "test"
//  "org.scalatest" % "scalatest_2.11" % "3.0.1" % "test"
)
)


enablePlugins(GatlingPlugin)