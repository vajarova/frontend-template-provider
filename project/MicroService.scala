import sbt.Keys._
import sbt.Tests.{SubProcess, Group}
import sbt._
import play.routes.compiler.StaticRoutesGenerator
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import de.heikoseeberger.sbtheader.HeaderKey._
import de.heikoseeberger.sbtheader.HeaderPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion


trait MicroService {

  import uk.gov.hmrc._
  import DefaultBuildSettings._
  import uk.gov.hmrc.{SbtBuildInfo, ShellPrompt, SbtAutoBuildPlugin}
  import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
  import uk.gov.hmrc.versioning.SbtGitVersioning
  import uk.gov.hmrc.SbtArtifactory
  import play.sbt.routes.RoutesKeys.routesGenerator


  import TestPhases._

  val appName: String

  lazy val appDependencies : Seq[ModuleID] = ???
  lazy val plugins : Seq[Plugins] = Seq.empty
  lazy val playSettings : Seq[Setting[_]] = Seq.empty


  lazy val microservice = Project(appName, file("."))
    .enablePlugins(Seq(play.sbt.PlayScala,SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory) ++ plugins : _*)
    .settings(playSettings : _*)
    .settings(scalaSettings: _*)
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(
      scalaVersion := "2.11.11",
      libraryDependencies ++= appDependencies,
      retrieveManaged := true,
      evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
      routesGenerator := StaticRoutesGenerator,
      excludes := Seq(
        "resources/**"
      )
    )
    .settings(majorVersion := 0)
    .configs(IntegrationTest)
    .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
    .settings(unmanagedResourceDirectories in sbt.Compile += baseDirectory.value / "resources")
    .settings(
      Keys.fork in IntegrationTest := false,
      unmanagedSourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest)(base => Seq(base / "it")),
      addTestReportOption(IntegrationTest, "int-test-reports"),
      testGrouping in IntegrationTest := TestPhases.oneForkedJvmPerTest((definedTests in IntegrationTest).value),
      parallelExecution in IntegrationTest := false)
      .settings(resolvers ++= Seq(
        Resolver.bintrayRepo("hmrc", "releases"),
        Resolver.jcenterRepo
      ))
}

private object TestPhases {

  def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
    tests map {
      test => new Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
    }
}
