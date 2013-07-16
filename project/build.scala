import sbt._,Keys._
import com.typesafe.sbt.SbtStartScript.startScriptForClassesSettings

object build extends Build{

  lazy val buildSettings =
    Defaults.defaultSettings ++ Seq(
      organization := "com.herokuapp.lombok",
      version := "0.1.0-SNAPSHOT",
      scalacOptions := Seq("-deprecation", "-unchecked"),
      scalaVersion := "2.10.2",
      resolvers ++= Seq(
        Opts.resolver.sonatypeReleases,
        Classpaths.typesafeResolver
      ),
      licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php"))
    )

  lazy val root = Project(
    "root",
    file("."),
    settings = buildSettings ++ startScriptForClassesSettings ++ Seq(
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
        "org.scalaz" %% "scalaz-core" % "7.1.0-M1"
      )
    )
  )

  val u = "0.6.8"
  val lombokVersion = "0.12.0"

  lazy val server = Project(
    "server",
    file("server"),
    settings = buildSettings ++ startScriptForClassesSettings ++ Seq(
      libraryDependencies ++= Seq("filter", "jetty", "json4s").map{n=>
        "net.databinder" %% ("unfiltered-"+n) % u
      },
      libraryDependencies ++= Seq(
        "net.databinder" %% "unfiltered-spec" % u % "test",
        "org.projectlombok" % "lombok" % lombokVersion
      ),
      libraryDependencies += "org.scala-sbt" % "sbt" % "0.13.0-RC2",
      sourceGenerators in Compile <+= (sourceManaged in Compile).map{lombokVersionInfoGenerate},
      retrieveManaged := true
    )
  )dependsOn(common)

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
        "org.scalaj" %% "scalaj-http" % "0.3.9" exclude("junit", "junit"),
        "org.json4s" %% "json4s-native" % "3.2.4"
      )
    )
  )dependsOn(common)

}

