lazy val scalaVersionsJS  = Seq("2.13.4", "2.12.12", "2.11.12")
lazy val scalaVersionsJVM = "3.0.0-M2" +: scalaVersionsJS
lazy val scalaVersionsSN  = Seq("2.11.12")

lazy val scalaTestVersion = "3.2.3"

lazy val commonSettings = Seq(
  organization := "org.rogach",
  name := "scallop",
  version := {
    val versionRegexp = """[0-9]+\.[0-9]+\.[0-9]+""".r
    val libraryDependenciesString: String = {
      val io = scala.io.Source.fromFile("README.md")
      try {
        io.getLines.filter(_.contains("libraryDependencies")).mkString
      } finally {
        io.close()
      }
    }
    versionRegexp.findFirstIn(libraryDependenciesString).get
  },
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-feature",
    "-language:postfixOps,reflectiveCalls,existentials,implicitConversions",
    "-Xlint",
  ),
  unmanagedSourceDirectories in Compile += {
    val base = baseDirectory.value.getParentFile / "src" / "main"
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) =>
        base / "scala-2.13+"
      case Some((2, v)) if v >= 13 =>
        base / "scala-2.13+"
      case _ =>
        base / "scala-2.13-"
    }
  },
  licenses := Seq(
    "MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")
  ),
  homepage := Some(url("https://github.com/scallop/scallop")),
  scmInfo := Some(
    ScmInfo(
      browseUrl = url("http://github.com/scallop/scallop"),
      connection = "scm:git:git@github.com:scallop/scallop.git"
    )
  ),
  boilerplateSource in Compile := baseDirectory.value.getParentFile / "src" / "main" / "boilerplate",
  pomExtra := (
    <developers>
      <developer>
        <id>rogach</id>
        <name>Platon Pronko</name>
        <url>http://rogach.org</url>
      </developer>
    </developers>
  ),
  pomIncludeRepository := { x => false },
  publishTo := {
    val snapshot = false
    if (snapshot)
      Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
    else
      Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
  },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  scalacOptions in (Compile, doc) ++= {
    if (isDotty.value) Nil
    else Opts.doc.sourceUrl("https://github.com/scallop/scallop/blob/develop/€{FILE_PATH}.scala")
  },
  parallelExecution in Test := false,
  siteSubdirName in SiteScaladoc := "",
  git.remoteRepo := "git@github.com:scallop/scallop.git"
)

lazy val scallop =
  crossProject(JVMPlatform, NativePlatform, JSPlatform)
  .crossType(new sbtcrossproject.CrossType { // like CrossType.Full, but with shared sources in root
    def projectDir(crossBase: File, projectType: String): File =
      crossBase / projectType
    def projectDir(crossBase: File, platform: sbtcrossproject.Platform): File =
      crossBase / platform.identifier
    def sharedSrcDir(projectBase: File, conf: String): Option[File] =
      Some(projectBase.getParentFile / "src" / conf / "scala")
  })
  .in(file("."))
  .settings(commonSettings)
  .enablePlugins(SiteScaladocPlugin, GhpagesPlugin)
  .configure(_.enablePlugins(spray.boilerplate.BoilerplatePlugin))
  .jvmSettings(
    crossScalaVersions  := scalaVersionsJVM,
    scalaVersion        := scalaVersionsJVM.head,
    libraryDependencies ++= {
      if (scalaVersion.value.startsWith("0")) {
        Seq()
      } else {
        Seq(
          "org.scalatest" %%% "scalatest" % scalaTestVersion % Test
        )
      }
    },
    // fix for paths to source files in scaladoc
    doc in Compile := {
      import sys.process._
      Seq("bash","-c",""" for x in $(find jvm/target/scala-2.13/api/ -type f); do sed -i "s_`pwd`/__" $x; done """).!
      (doc in Compile).value
    },
    scalacOptions in Test -= "-Xlint"
  )
  .nativeSettings(
    crossScalaVersions := scalaVersionsSN,
    scalaVersion       := scalaVersionsSN.head,
  )
  .jsSettings(
    crossScalaVersions := scalaVersionsJS,
    scalaVersion       := scalaVersionsJS.head,
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % scalaTestVersion % Test
    ),
  )
