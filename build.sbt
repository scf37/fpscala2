
val scalaSettings = Seq(
  scalaVersion := "3.0.0-RC3",
  scalacOptions ++= compilerOptions
)

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked",
  "-language:_",
  "-Ykind-projector"
)

lazy val dependencies = Seq(
  "org.typelevel" %% "cats-effect" % "3.1.0" cross(CrossVersion.for3Use2_13),
  "com.twitter" %% "finagle-http" % "21.3.0" cross(CrossVersion.for3Use2_13),
  "com.tethys-json" %% "tethys" % "0.21.0" cross(CrossVersion.for3Use2_13),
  "org.flywaydb" % "flyway-core" % "5.2.4",
  "org.postgresql" % "postgresql" % "42.1.4",
  "org.apache.commons" % "commons-dbcp2" % "2.6.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "me.scf37" %% "config3" % "1.0.5" cross(CrossVersion.for3Use2_13)
)

lazy val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.2.8",
  "org.testcontainers" % "postgresql" % "1.15.3"
).map(_ % "test")

val fpscala2 = project.in(file("."))
    .settings(scalaSettings)
    .enablePlugins(PackPlugin)
    .settings(packMain := Map("fpscala2" -> "me.scf37.fpscala2.Main"))
    .settings(resolvers += "Scf37" at "https://dl.bintray.com/scf37/maven/")
    .settings(libraryDependencies ++= dependencies)
    .settings(libraryDependencies ++= testDependencies)

//addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7")
//addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)