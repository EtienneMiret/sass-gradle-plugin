package io.miret.etienne.gradle.sass

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.matching.UrlPattern
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private val pkg = object {}.javaClass

/**
 * File mode for executable files.
 * Readable and executable by everyone.
 * Writeable by owner only.
 */
val EXEC_FILE_MODE = "755".toInt(8)

/**
 * Copies classpath resources to a directory.
 *
 * Each resource will be searched in the classpath from `base` and
 * copied to `destination`. That is, for `base = "/io/miret"`,
 * `destination = "/var/tmp"` and a `foo/bar.txt` resource,
 * the `/io/miret/foo/bar.txt` classpath resource will be copied
 * to `/var/tmp/foo/bar.txt`.
 *
 * Intermediate directories will be created as needed.
 *
 * @param base base name for the resources, must include the starting '/'.
 * @param resources the set of file resources to copy, relative to base.
 * @param destination a path that points to an existing directory.
 */
fun copyResources(
  base: String,
  resources: Collection<String>,
  destination: Path,
) {
  require(base.startsWith("/")) { "Base resource name must start with a '/'" }
  for (resourceStr in resources) {
    val resourcePath = Paths.get(resourceStr)
    require(!resourcePath.isAbsolute) { "Absolute path provided: $resourceStr" }
    val target = destination.resolve(resourcePath)
    Files.createDirectories(target.parent)
    pkg.getResourceAsStream("$base/$resourceStr").use { input ->
      requireNotNull(input) { "No such resource: $resourceStr" }
      Files.copy(input, target)
    }
  }
}

/**
 * Creates an archive with the dummy sass executable from classpath.
 * On Windows, this is a ZIP archive with `sass.bat`.
 * Elsewhere, this is a gzipped, tar archive with `sass.sh`.
 */
fun createArchive(): ByteArray {
  val bytes = ByteArrayOutputStream()
  if (Os.isFamily(Os.FAMILY_WINDOWS)) {
    ZipOutputStream(bytes).use { zip ->
      pkg.getResourceAsStream("sass.bat").use { sass ->
        checkNotNull(sass)
        zip.putNextEntry(ZipEntry("dart-sass/sass.bat"))
        sass.copyTo(zip)
      }
    }
  } else {
    GZIPOutputStream(bytes).use { gz ->
      TarArchiveOutputStream(gz).use { tgz ->
        pkg.getResourceAsStream("sass.sh").use { sass ->
          checkNotNull(sass)
          val entry = TarArchiveEntry("dart-sass/sass")
          entry.size = sass.available().toLong()
          entry.mode = EXEC_FILE_MODE
          tgz.putArchiveEntry(entry)
          sass.copyTo(tgz)
          tgz.closeArchiveEntry()
        }
      }
    }
  }
  return bytes.toByteArray()
}

/**
 * Starts a server, on a dynamic port, that stands in for the
 * Dart Sass release host.
 *
 * The caller is responsible for stopping the returned server.
 *
 * @param url the URLs for which the archive is served.
 * @param archive the content of the archive to serve.
 */
fun startSassServer(url: UrlPattern, archive: ByteArray): WireMockServer {
  val server = WireMockServer(options().dynamicPort())
  server.start()
  server.stubFor(
    get(url)
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(archive)
      )
  )
  return server
}

/**
 * Runs Gradle, with this plugin on the classpath, and expects
 * the build to succeed.
 *
 * @param projectDir the directory of the project to build.
 * @param environment the whole environment of the build.
 * @param arguments the command line arguments for Gradle.
 */
fun runGradle(
  projectDir: Path,
  environment: Map<String, String>,
  vararg arguments: String,
): BuildResult =
  GradleRunner.create()
    .withPluginClasspath()
    .withEnvironment(environment)
    .withArguments(*arguments)
    .withProjectDir(projectDir.toFile())
    .build()
