import sbt._
import java.io.File

/**
 * A helper object to write out standard debian package maintainer scripts
 * for Coursera's environment.
 *
 * @author Brennan Saeta <saeta@coursera.org>
 */
object DebHelpers {

  /**
   * Writes out all the standard files.
   */
  def writeStandardFiles(normalizedName: String, organizationName: String = "coursera"): Seq[(File, String)] = {
    val tmpDir = IO.createTemporaryDirectory
    Seq(
      postinst(normalizedName, organizationName, tmpDir),
      prerm(normalizedName, organizationName, tmpDir)
    )
  }


  /**
   * Creates a post install script in a temporary directory that will
   * handle all the appropriate actions.
   */
  def postinst(normalizedName: String, organizationName: String, tmpDir: File): (File, String) = {
    val tmpFile = tmpDir / "postinst"
    val contents =
      """#!/bin/sh
        |
        |set -e
        |
        |SERVICE=%s
        |ORG_NAME=%s
        |
        |# Test if the user already exists.
        |if id $SERVICE > /dev/null 2>&1; then
        |  echo "Skipping adding $SERVICE; already there."
        |else
        |  echo "Adding user $SERVICE"
        |  useradd -s /bin/sh -d /$ORG_NAME/$SERVICE $SERVICE
        |fi
        |
        |# Chown all files as appropriate
        |chown -R $SERVICE:$SERVICE /$ORG_NAME/$SERVICE
        |
        |# Start the newly installed service
        |service $SERVICE start
        |""".stripMargin.format(normalizedName, organizationName)
    IO.write(tmpFile, contents)
    (tmpFile, "postinst")
  }

  def prerm(normalizedName: String, organizationName: String, tmpDir: File): (File, String) = {
    val tmpFile = tmpDir / "prerm"
    val contents =
      """#!/bin/sh
        |
        |SERVICE=%s
        |ORG_NAME=%s
        |
        |service $SERVICE stop || echo "Already stopped."
        |""".stripMargin.format(normalizedName, organizationName)
    IO.write(tmpFile, contents)
    (tmpFile, "prerm")
  }

}
