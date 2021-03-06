import play.sbt.PlayScala
import sbtbuildinfo.BuildInfoPlugin.autoImport._
import sbtassembly.AssemblyPlugin.autoImport._
import com.typesafe.sbt.SbtNativePackager.Universal

licenses := Seq("MIT-License" -> url("https://opensource.org/licenses/MIT"))

lazy val Versions = new {
  val scala = "2.11.11"
  val version = "0.1"
  val scapegoatVersion = "1.1.0"
  val util = "0.27.8"
  val hbase = "1.3.1" // was 1.3.1
  val hadoop = "2.8.1"
}

lazy val Constant = new {
  val appName = "sbr-admin-data-api"
  val detail = Versions.version
  val organisation = "ons"
  val team = "sbr"
}

lazy val commonSettings = Seq (
  scalaVersion := Versions.scala,
  scalacOptions in ThisBuild ++= Seq(
    "-language:experimental.macros",
    "-target:jvm-1.8",
    "-encoding", "UTF-8",
    "-language:reflectiveCalls",
    "-language:experimental.macros",
    "-language:implicitConversions",
    "-language:higherKinds",
    "-language:postfixOps",
    "-deprecation", // warning and location for usages of deprecated APIs
    "-feature", // warning and location for usages of features that should be imported explicitly
    "-unchecked", // additional warnings where generated code depends on assumptions
    "-Xlint", // recommended additional warnings
    "-Xcheckinit", // runtime error when a val is not initialized due to trait hierarchies (instead of NPE somewhere else)
    "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
    //"-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver
    "-Ywarn-value-discard", // Warn when non-Unit expression results are unused
    "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures
    "-Ywarn-dead-code", // Warn when dead code is identified
    "-Ywarn-unused", // Warn when local and private vals, vars, defs, and types are unused
    // "-Ywarn-unused-import", //  Warn when imports are unused (don't want IntelliJ to do it automatically)
    "-Ywarn-numeric-widen" // Warn when numerics are widened
  ),
  resolvers ++= Seq(
    Resolver.typesafeRepo("releases")
  ),
  coverageExcludedPackages := ".*Routes.*;.*ReverseRoutes.*;.*javascript.*"
)

lazy val api = (project in file("."))
  .enablePlugins(BuildInfoPlugin, GitVersioning, GitBranchPrompt, PlayScala)
  .settings(commonSettings: _*)
  .settings(
    scalaVersion := Versions.scala,
    name := Constant.appName,
    moduleName := "sbr-admin-data-api",
    version := Versions.version,
    buildInfoPackage := "controllers",
    buildInfoKeys := Seq[BuildInfoKey](
      organization,
      name,
      version,
      scalaVersion,
      sbtVersion,
      BuildInfoKey.action("gitVersion") {
      git.formattedShaVersion.?.value.getOrElse(Some("Unknown")).getOrElse("Unknown") +"@"+ git.formattedDateVersion.?.value.getOrElse("")
    }),
    // di router -> swagger
    routesGenerator := InjectedRoutesGenerator,
    buildInfoOptions += BuildInfoOption.ToMap,
    buildInfoOptions += BuildInfoOption.ToJson,
    buildInfoOptions += BuildInfoOption.BuildTime,
    buildInfoPackage := "controllers",
    // Run with proper default env vars set for hbaseInMemory
    javaOptions in Universal ++= Seq(
      "-Dsource=hbaseInMemory",
      "-Dsbr.hbase.inmemory=true"
    ),
    libraryDependencies ++= Seq (
      filters,
      jdbc,
      "org.webjars"                  %%    "webjars-play"        %    "2.5.0-3",
      "com.typesafe.scala-logging"   %%    "scala-logging"       %    "3.5.0",
      "org.scalatestplus.play"       %%    "scalatestplus-play"  %    "2.0.0"           % Test,
      "io.swagger"                   %%    "swagger-play2"       %    "1.5.3",
      "org.webjars"                  %     "swagger-ui"          %    "2.2.10-1",
      "org.apache.hive"              %     "hive-jdbc"           %    "1.2.1",
      "org.apache.spark"             %     "spark-hive_2.11"     %    "2.1.0",
      "mysql"                        %     "mysql-connector-java" %   "5.1.35"
    ),
    assemblyJarName in assembly := "sbr-admin-data-api.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    },
    mainClass in assembly := Some("play.core.server.ProdServerStart"),
    fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value),
    /// TEST STUFF ////
    testForkedParallel in Test := true
  )
