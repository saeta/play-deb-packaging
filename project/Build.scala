import sbt._
import Keys._
import play.Project._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.SbtNativePackager._
import java.text.SimpleDateFormat

object ApplicationBuild extends Build {

  val appName         = "play-deb-packaging"
  val appVersion      = "1.0.0" // We use semantic versioning TODO: insert link

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm
  )

  lazy val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
    prodPort := 20000 // leagues under the sea
  ).settings(
    debSettings: _*
  )

  // Define a few keys that are used in the build.
  val prodPort = SettingKey[Int]("prod-port", "Port to run off of in production")
  val deb = TaskKey[File]("deb", "Make a debian package")  

  // Bind all the keys appropriately to make the build go!
  val debSettings = com.typesafe.sbt.SbtNativePackager.packagerSettings ++ Seq(
    maintainer in Debian := "Brennan Saeta <saeta@coursera.org>",
    name in Debian <<= name("coursera-" + _),  // We prefix the names so that they are all easily searched
    packageSummary in Debian <<= name(n => "Coursera's %s service" format n),
    packageDescription in Debian := """A sample service demonstrating how to package a Play! application
                                      |using SBT in Coursera's standard format.""".stripMargin,
    linuxPackageMappings in Debian <<= (dist, normalizedName, version) map { (dist, id, version) =>
      val tmpdir = IO.createTemporaryDirectory
      IO.unzip(dist, tmpdir)
      val base = tmpdir / (id + "-" + version)
      val destination = new File("/coursera") / id
      (for {
        jar <- IO.listFiles(base / "lib")
      } yield (packageMapping(jar -> (destination / "lib" / jar.getName).getAbsolutePath) withPerms "0440")).toSeq ++
        Seq(packageMapping((base / "start") -> (destination / "start").getAbsolutePath) withPerms "0744")
    },
    linuxPackageMappings in Debian <+= (normalizedName, version, prodPort) map { (id, version, port) =>
      val tmp = IO.createTemporaryDirectory
      val upstartFile = tmp / "upstart"
      val destination = new File("/etc") / "init" / (id + ".conf")
      // Note: chdir can't do environment variable interpolation
      // Note: jvmVars should be pulled out into its own settings key for customization.
      val upstartContents = """# This is the upstart script that boots a PlayCour service
         |
         |author "PlayCour SBT Plugin <saeta@coursera.org>"
         |description "A start-stop script for a PlayCour service"
         |version "1.0"
         |
         |env proj=%s
         |env port=%s
         |env jvmVars="-Xms256M -Xmx1024M -server -XX:+PrintCommandLineFlags -Xloggc:logs/gc.log \
         |-XX:+PrintGCDetails -XX:+PrintGCTimeStamps \
         |-XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=100M"
         |
         |start on runlevel [2345]
         |stop on runlevel [016]
         |chdir "/coursera/%s"
         |
         |exec su -c "/coursera/$proj/start $jvmVars -Dhttp.port=$port -Dlogger.resource=prod-logger.xml" $proj
         |
         |# Make sure the old PID file is removed,
         |# and create a few standard directories.
         |# and set up logging to ephemeral disks.
         |pre-start script
         |  rm -f /coursera/$proj/RUNNING_PID
         |  mkdir -p /coursera/$proj/var
         |  chown $proj:$proj -R /coursera/$proj/var
         |  mkdir -p /mnt/logs/$proj
         |  chown $proj:$proj /mnt/logs/$proj
         |  if test ! -e /coursera/$proj/logs; then
         |    ln -s /mnt/logs/$proj /coursera/$proj/logs
         |  fi
         |  mkdir -p /mnt/coursera/$proj
         |  chown $proj:$proj /mnt/coursera/$proj
         |end script
         |
         |respawn
         |# Stop respawning if we die 4 times within 20 seconds
         |respawn limit 4 20
         |""".stripMargin.format(id, port, id)
      IO.write(upstartFile, upstartContents)
      packageMapping(upstartFile -> destination.getAbsolutePath()) withPerms "0644"
    },
    debianMaintainerScripts <++= (normalizedName, version) map { (id, version) =>
      DebHelpers.writeStandardFiles(id)
      Seq()
    },
    // RPM muck
    rpmRelease := "1",
    rpmVendor := "Coursera",
    // Windows muck
    wixConfig <<= (sbtVersion, sourceDirectory in Windows) map ((a, b) => <Wix></Wix>),

    // Define the 'deb' command as essentially an alias for debian:package-bin
    deb <<= packageBin in Debian map { x =>
      println()
      println("Congratulations! Your debian package has been built!")
      println()
      println("Find it at: %s" format x.getCanonicalPath())
      println()
      x
    })

  // Record build-time parameters as a resource to be inspected and displayed at runtime.
  // Used in the extra goodies at /cour/build
  // For more details, see: https://docs.google.com/a/coursera.org/presentation/d/1iCV6-WpKQn6ek49EYxro98RPcsduOlD-48wh9AR3kfw
  val dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  val buildInfoSettings = Seq(
    resourceGenerators in Compile <+= (resourceManaged in Compile, name, version) map { (dir, n, v) =>
      val date = new java.util.Date()
      val file = dir / "playcour" / "build.properties"
      val hash = ("git rev-parse HEAD" !!).trim()
      val branch = ("git rev-parse --abbrev-ref HEAD" !!).trim()
      val contents = "name=%s\nversion=%s\nhash=%s\nbranch=%s\ntime=%s" format (n, v, hash, branch, dateFormatter.format(date))
      IO.write(file, contents)
      Seq(file)
    })

}
