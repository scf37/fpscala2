
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
  "com.tethys-json" %% "tethys" % "0.21.0" cross(CrossVersion.for3Use2_13),
)

val fpscala2 = project.in(file("."))
    .settings(scalaSettings)
    .enablePlugins(PackPlugin)
    .settings(packMain := Map("fpscala2" -> "me.scf37.fpscala2.Main"))
    .settings(resolvers += "Scf37" at "https://dl.bintray.com/scf37/maven/")
    .settings(libraryDependencies ++= dependencies)

//addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7")
//addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)