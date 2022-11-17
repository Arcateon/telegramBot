import sbt._

object Dependencies {

  lazy val botCore = "com.bot4s" %% "telegram-core" % "5.6.1"
  lazy val mongodb = "org.mongodb.scala" %% "mongo-scala-driver" % "4.7.1"
  lazy val jsonData = "org.json4s" %% "json4s-native" % "4.0.5"
  lazy val jacksonData = "org.json4s" %% "json4s-jackson" % "4.0.5"

}
