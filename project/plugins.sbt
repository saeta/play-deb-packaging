// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("play" % "sbt-plugin" % "2.1.0")

// Depend on the sbt-native-packager plugin.
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.5.4")
