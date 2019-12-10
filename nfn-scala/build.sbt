import sbt.Keys._
import sbt._

// PROJECTS

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= Seq(
  dependencies.akkaActor,
  dependencies.logback,
  dependencies.logging,
  dependencies.scalatest,
  dependencies.slf4j,
  dependencies.xml,
  dependencies.akkaTest,
  dependencies.scalactic,
  dependencies.config,
  dependencies.lift,
  dependencies.bcel,
  dependencies.scopt,
  dependencies.shttp,
  dependencies.jupiter,
  dependencies.collectionCompat)

lazy val lambdaCalculus = Project(
  "lambdacalc",
  file("lambdacalc")
).settings(buildSettings ++ Seq(
  libraryDependencies ++= commonDependencies)
)

lazy val nfn = Project(
  "nfn",
  file(".")).settings(buildSettings ++ Seq(
  libraryDependencies ++= commonDependencies ++ Seq (
    dependencies.akkaActor,
    dependencies.akkaTest,
    dependencies.scalactic,
    dependencies.config,
    dependencies.lift,
    dependencies.bcel,
    dependencies.scopt,
    dependencies.shttp,
    dependencies.junitJupterEngine,
    dependencies.jupiter,
    dependencies.xml
  ),
  mainClass in assembly := Some("runnables.production.ComputeServerStarter")//,
  //run in Compile := run in Compile dependsOn compileCCNLiteTask
)).dependsOn(lambdaCalculus)

lazy val testservice : Project = Project(
  "testservice",
  file("testservice")).settings(buildSettings).dependsOn(nfn)

lazy val compilerOptions = Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-encoding",
  "UTF-8",
  "-language:implicitConversions"
)

lazy val commonSettings = Seq(
  version       := "0.2.2",
  scalaVersion  := "2.13.0",
  scalacOptions ++= compilerOptions,
  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("public"),
    "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"
  ),
  test in assembly := {},
  compileCCNLite
)



lazy val dependencies =
  new {
    val akkaActorV = "2.5.26"
    val akkaTestkitV = "2.5.26"
    val scalacticV = "3.0.8"
    val scalatestV = "3.0.8"
    val logbackClassicV = "1.2.3"
    val scalaLoggingV = "3.9.2"
    val configV = "1.3.3"
    val slf4jV = "1.7.28"
    val liftV = "3.4.0"
    val bcelV = "6.4.1"
    val scoptV = "3.7.1"
    val shttpV = "2.4.2"
    val junitPlattformLauncherV = "1.5.2"
    val jupiterV = "5.5.2"

    val xmlV = "1.2.0"
    //val xmlV = "2.0.0-M1"
    val collectionCompatV = "2.1.2"
    val parserV = "1.1.2"

    val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaActorV
    val akkaTest = "com.typesafe.akka" %% "akka-testkit" % akkaTestkitV
    val scalactic = "org.scalactic" %% "scalactic" % scalacticV
    val scalatest = "org.scalatest" %% "scalatest" % scalatestV % "test"
    val logback = "ch.qos.logback" % "logback-classic" % logbackClassicV
    val logging = "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingV
    val config = "com.typesafe" % "config" % configV
    val slf4j = "org.slf4j" % "slf4j-api" % slf4jV
    val lift = "net.liftweb" %% "lift-json" % liftV
    val bcel = "org.apache.bcel" % "bcel" % bcelV
    val scopt = "com.github.scopt" %% "scopt" % scoptV
    val shttp = "org.scalaj" %% "scalaj-http" % shttpV
    val junitPlattformLauncher = "org.junit.platform" %% "junit-platform-launcher"% junitPlattformLauncherV % "test"
    val junitJupterEngine = "org.junit.jupiter" % "junit-jupiter-engine" % jupiterV % Test
    val jupiter = "org.junit.jupiter" % "junit-jupiter-api" % jupiterV % Test
    //val xml = "org.scala-lang.modules" %% "scala-xml" % xmlV
    val xml = "org.scala-lang.modules" %% "scala-xml" % xmlV
    val collectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % collectionCompatV
    val parser = "org.scala-lang.modules" %% "scala-parser-combinators" % parserV
  }

lazy val commonDependencies = Seq(
  dependencies.logback,
  dependencies.logging,
  dependencies.scalatest,
  dependencies.slf4j,
  dependencies.parser
)

lazy val buildSettings =
  commonSettings /* ++
  scalafmtSettings*/

lazy val compileCCNLiteTask = taskKey[Unit]("Compiles CCN Lite")
val compileCCNLite = compileCCNLiteTask := {
  val ccnlPath = {
    if(new File("./ccn-lite-nfn/bin").exists()){
      new File("./ccn-lite-nfn").getCanonicalPath
    }
    else {
      val p = System.getenv("CCNL_HOME")
      if(p == null || p == ""){
        throw new Exception("nfn scala ccn-lite submodule was not initialized")
      }
    }
  }
  val processBuilder = {
    val cmds = List("make", "clean", "all")
    new java.lang.ProcessBuilder(cmds:_*)
  }

  val ccnlPathFile = new File(s"§ccnlPath/src/build")
  println(s"Building CCN-Lite in directory $ccnlPathFile")
  processBuilder.directory(ccnlPathFile)
  val e = processBuilder.environment()
  e.put("USE_NFN", "1")
  e.put("USE_NACK", "1")
  val process = processBuilder.start()
  val processOutputReaderPrinter = new InputStreamToStdOut(process.getInputStream)
  val t = new Thread(processOutputReaderPrinter).start()
  process.waitFor()
  val resValue = process.exitValue()
  if(resValue == 0)
    println(s"Compiled ccn-lite with return value ${process.exitValue()}")
  else{
    throw new Exception("Error during compilation fo ccn-lite")
  }
  process.destroy()
}
/*val compileCCNLiteTask = TaskKey[Unit]("compileCCNLite")

compileCCNLite := {
  compileCCNLitePerform()
}

run := {
  compileCCNLite.value
  (run in Compile).value
}*/

/*lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true,
    scalafmtTestOnCompile := true,
    scalafmtVersion := "1.2.0"
  )*/