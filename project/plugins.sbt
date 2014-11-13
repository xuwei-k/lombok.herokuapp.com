resolvers += Resolver.url("heroku-sbt-plugin-releases",
  url("http://dl.bintray.com/heroku/sbt-plugins/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.heroku" % "sbt-heroku" % "0.2.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.7.6")

scalacOptions := Seq("-deprecation", "-unchecked", "-language:_", "-Xlint")
