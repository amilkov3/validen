
name := "validen"

lazy val scalaV = "2.12.2"
lazy val scalaCheckVersion = "3.0.1"

scalaVersion in Global := scalaV

lazy val validen = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "io.voklim",
      scalaVersion := scalaV,
      version      := "0.1.0"
    )),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats" % "0.9.0",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6",
      "com.chuusai" %% "shapeless" % "2.3.2",

      "org.scalatest" %% "scalatest" % scalaCheckVersion % "test"
    )
  ).configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(
    scalaSource in IntegrationTest := baseDirectory.value / "src" / "it" / "scala"
  )

lazy val IntegrationTest = config("it").extend(Test)

scalacOptions in Global ++= Seq(
  "-Xfatal-warnings",
  "-unchecked",
  "-feature",
  "-deprecation",
  "-language:higherKinds",
  "-language:implicitConversions"
)
