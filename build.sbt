val scalaSettings = Seq(
  scalaVersion := "2.12.5",
  scalacOptions ++= compilerOptions
)

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-deprecation",
  "-language:implicitConversions",
  "-unchecked",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xlint",
  "-language:_",
  "-Ypartial-unification"/*,
  "-Xfatal-warnings"*/
)

lazy val dependencies = Seq(
  "org.typelevel" %% "cats-core" % "1.2.0",
  "org.typelevel" %% "cats-effect" % "1.0.0",
  "com.twitter" %% "finatra-jackson" % "19.2.0",
  "org.flywaydb" % "flyway-core" % "5.2.4",
  "org.postgresql" % "postgresql" % "42.1.4",
  "org.apache.commons" % "commons-dbcp2" % "2.6.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "me.scf37.config3" %% "config3" % "1.0.0"
)

lazy val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.0.5",
  "ru.yandex.qatools.embed" % "postgresql-embedded" % "2.4"
).map(_ % "test")



val fpscala2 = project.in(file("."))
    .settings(scalaSettings)
    .settings(resolvers += "Scf37" at "https://dl.bintray.com/scf37/maven/")
    .settings(libraryDependencies ++= dependencies)
    .settings(libraryDependencies ++= testDependencies)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7")
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)