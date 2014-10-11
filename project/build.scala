import sbt._,Keys._
import com.heroku.sbt.HerokuPlugin
import com.typesafe.sbt.SbtNativePackager._

object build extends Build{

  lazy val buildSettings =
    Seq(
      organization := "com.herokuapp.lombok",
      version := "0.1.0-SNAPSHOT",
      scalacOptions := Seq("-deprecation", "-unchecked", "-language:_", "-Xlint"),
      scalaVersion := "2.11.3",
      resolvers ++= Seq(
        Opts.resolver.sonatypeReleases,
        Classpaths.typesafeReleases
      ),
      licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php"))
    )

  lazy val root = Project(
    "root",
    file("."),
    settings = buildSettings ++ Seq(
    )
  )aggregate(server,client,common,jointest)

  lazy val jointest = Project(
    "jointest",
    file("jointest"),
    settings = buildSettings ++ Seq(
    )
  )dependsOn(server,client)

  lazy val common = Project(
    "common",
    file("common"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= Seq(
        "org.scalaz" %% "scalaz-core" % "7.1.0"
      )
    )
  )

  val u = "0.8.2"
  val lombokVersion = "1.14.8"

  lazy val server = Project(
    "server",
    file("server")
  ).settings(buildSettings ++ packageArchetype.java_application: _*).enablePlugins(HerokuPlugin).settings(
    HerokuPlugin.autoImport.herokuAppName in Compile := "lombok",
    libraryDependencies ++= Seq("filter", "jetty", "json4s").map{n=>
      "net.databinder" %% ("unfiltered-"+n) % u
    },
    libraryDependencies ++= Seq(
      "org.projectlombok" % "lombok" % lombokVersion,
      "org.scala-sbt" %% "io" % sbtVersion.value
    ),
    sourceGenerators in Compile <+= (sourceManaged in Compile).map{lombokVersionInfoGenerate}
  ).dependsOn(common)

  def lombokVersionInfoGenerate(dir:File):Seq[File] = {
    val src =
      """package com.herokuapp.lombok
        |
        |object lombokVersion{
        |  def apply() = "%s"
        |}""".format(lombokVersion).stripMargin
    println(src)
    val file = dir / "lombokVersion.scala"
    IO.write(file,src)
    Seq(file)
  }

  lazy val client = Project(
    "client",
    file("client"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= Seq(
        "org.scalaj" %% "scalaj-http" % "0.3.16",
        "org.json4s" %% "json4s-native" % "3.2.10"
      )
    )
  )dependsOn(common)

}

