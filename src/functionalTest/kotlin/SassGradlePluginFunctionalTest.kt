package io.miret.etienne.gradle.sass

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.StringWriter
import java.io.Writer
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.PosixFilePermissions
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal class SassGradlePluginFunctionalTest {

  @TempDir
  lateinit var projectDir: Path

  private lateinit var server: WireMockServer

  @BeforeEach
  fun startServer() {
    server = startSassServer(anyUrl(), createArchive())
  }

  @AfterEach
  fun stopServer() {
    server.stop()
  }

  @BeforeEach
  fun setupProject() {
    Files.createFile(projectDir.resolve("settings.gradle"))
    SassGradlePluginFunctionalTest::class.java.getResourceAsStream("build.gradle").use { input ->
      Files.copy(input!!, projectDir.resolve("build.gradle"))
    }
    SassGradlePluginFunctionalTest::class.java.getResourceAsStream("gradle.properties").use { input ->
      Files.copy(input!!, projectDir.resolve("gradle.properties"))
    }
  }

  @Test
  fun `should install sass`() {
    val expected = StringWriter().let { writer ->
      writeExecutable(writer)
      writer.toString()
    }

    runGradle("installSass")

    assertThat(projectDir.resolve(".gradle/sass/archive"))
      .isNotEmptyDirectory()
    val file = if (OS.current() == OS.WINDOWS) {
      "sass.bat"
    } else {
      "sass"
    }
    assertThat(projectDir.resolve(".gradle/sass/some.specific.version/dart-sass/$file"))
      .hasContent(expected)
  }

  @Test
  fun `should download specified version`() {
    runGradle("downloadSass")

    server.verify(getRequestedFor(urlMatching("/some\\.specific\\.version/dart-sass-some\\.specific\\.version-.*")))
  }

  @Test
  fun `should re-run on version change`() {
    server.stubFor(
      get(urlMatching("/v1/.*"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(createArchive("v1"))
        )
    )
    server.stubFor(
      get(urlMatching("/v2/.*"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(createArchive("v2"))
        )
    )

    runGradle(projectDir, mapOf(
      "URL" to server.baseUrl(),
      "VERSION" to "v1",
    ), "compileSass")
    runGradle(projectDir, mapOf(
      "URL" to server.baseUrl(),
      "VERSION" to "v2",
    ), "compileSass")

    server.verify(getRequestedFor(urlMatching("/v1/dart-sass-v1-.*")))
    server.verify(getRequestedFor(urlMatching("/v2/dart-sass-v2-.*")))
    assertThat(commandHistory)
      .content()
      .hasLineCount(2)
  }

  /**
   * The version change, so sass is downloaded again.
   * But both version resolve to the same binary, so compilation is done once.
   */
  @Test
  fun `should re-download on version change`() {
    runGradle(projectDir, mapOf(
      "URL" to server.baseUrl(),
      "VERSION" to "v1",
    ), "compileSass")
    runGradle(projectDir, mapOf(
      "URL" to server.baseUrl(),
      "VERSION" to "v2",
    ), "compileSass")

    server.verify(getRequestedFor(urlMatching("/v1/.*")))
    server.verify(getRequestedFor(urlMatching("/v2/.*")))
    assertThat(commandHistory)
      .content()
      .hasLineCount(1)
  }

  @Test
  fun `should compile sass`() {
    runGradle("compileCustomSass")

    assertThat(commandHistory)
      .hasContent("sass --style=expanded --source-map-urls=relative $dir/src/main/sass:$dir/build/sass")
  }

  @Test
  fun `should use sass configured after the task`() {
    SassGradlePluginFunctionalTest::class.java.getResourceAsStream("build-late-configuration.gradle").use { input ->
      Files.copy(input!!, projectDir.resolve("build.gradle"), StandardCopyOption.REPLACE_EXISTING)
    }

    runGradle("compileCustomSass")

    assertThat(commandHistory)
      .hasContent("sass --style=expanded --source-map-urls=relative $dir/src/main/sass:$dir/build/sass")
  }

  @Test
  fun `should use entry point`() {
    runGradle("compileEntryPoint")

    assertThat(commandHistory)
      .hasContent("sass --style=expanded --source-map-urls=relative $dir/src/main/sass/main.scss:$dir/build/sass/style.css")
  }

  @Test
  fun `should use all entry points`() {
    runGradle("compileSeveralEntryPoints")

    assertThat(commandHistory).content()
      .hasLineCount(1)
      .startsWith("sass --style=expanded --source-map-urls=relative")
      .contains("$dir/src/main/sass/main.scss:$dir/build/sass/style.css")
      .contains("$dir/src/main/sass/errors.scss:$dir/build/sass/errors.css")
  }

  @Test
  fun `should use custom paths`() {
    Files.createDirectories(projectDir.resolve("src/main/scss"))

    runGradle("compileCustomPaths")

    assertThat(commandHistory)
      .hasContent("sass --style=expanded --source-map-urls=relative $dir/src/main/scss/main.scss:$dir/build/css/styles/main.css")
  }

  @Test
  fun `should set loadPaths`() {
    runGradle("compileWithLoadPath")

    assertThat(commandHistory)
      .hasContent("sass --load-path=$dir/sass-lib --load-path=/var/lib/compass --style=expanded --source-map-urls=relative $dir/src/main/sass:$dir/build/sass")
  }

  @Test
  fun `should rerun on loadPath change`() {
    runGradle("compileWithLoadPath")
    Files.createDirectories(projectDir.resolve("sass-lib"))
    Files.createFile(projectDir.resolve("sass-lib/foo.scss"))
    runGradle("compileWithLoadPath")

    assertThat(commandHistory).content().hasLineCount(2)
  }

  @Test
  fun `should use compressed style`() {
    runGradle("compileCompressed")

    assertThat(commandHistory)
      .hasContent("sass --style=compressed --source-map-urls=relative $dir/src/main/sass:$dir/build/sass")
  }

  @Test
  fun `should watch`() {
    runGradle("watch")

    assertThat(commandHistory)
      .hasContent("sass --style=expanded --watch --source-map-urls=relative $dir/src/main/sass:$dir/build/sass")
  }

  @Test
  fun `should disable charset output`() {
    runGradle("compileNoCharset")

    assertThat(commandHistory)
      .hasContent("sass --style=expanded --no-charset --source-map-urls=relative $dir/src/main/sass:$dir/build/sass")
  }

  @Test
  fun `should disable error css output`() {
    runGradle("compileNoErrorCss")

    assertThat(commandHistory)
      .hasContent("sass --style=expanded --no-error-css --source-map-urls=relative $dir/src/main/sass:$dir/build/sass")
  }

  @Test
  fun `should disable source map with absolute urls`() {
    runGradle("compileNoSourceMapAbsolute")

    assertThat(commandHistory)
      .hasContent("sass --style=expanded --no-source-map $dir/src/main/sass:$dir/build/sass")
  }

  @Test
  fun `should disable source map with relative urls`() {
    runGradle("compileNoSourceMapRelative")

    assertThat(commandHistory)
      .hasContent("sass --style=expanded --no-source-map $dir/src/main/sass:$dir/build/sass")
  }

  @Test
  fun `should create embed source map with absolute urls`() {
    runGradle("compileEmbedSourceMapAbsolute")

    assertThat(commandHistory)
      .hasContent("sass --style=expanded --embed-source-map --source-map-urls=absolute $dir/src/main/sass:$dir/build/sass")
  }

  @Test
  fun `should create embed source map with relative urls`() {
    runGradle("compileEmbedSourceMapRelative")

    assertThat(commandHistory)
      .hasContent("sass --style=expanded --embed-source-map --source-map-urls=relative $dir/src/main/sass:$dir/build/sass")
  }

  @Test
  fun `should create file source map with absolute urls`() {
    runGradle("compileFileSourceMapAbsolute")

    assertThat(commandHistory)
      .hasContent("sass --style=expanded --source-map-urls=absolute $dir/src/main/sass:$dir/build/sass")
  }

  @Test
  fun `should create file source map with relative urls`() {
    runGradle("compileFileSourceMapRelative")

    assertThat(commandHistory)
      .hasContent("sass --style=expanded --source-map-urls=relative $dir/src/main/sass:$dir/build/sass")
  }

  @Test
  fun `should be quiet`() {
    runGradle("quiet")

    assertThat(commandHistory)
      .hasContent("sass --style=expanded --quiet --source-map-urls=relative $dir/src/main/sass:$dir/build/sass")
  }

  @Test
  fun `should support Gradle configuration cache`() {
    runGradle("--configuration-cache", "compileCustomSass", "compileWithLoadPath")
    Files.createDirectories(projectDir.resolve("sass-lib"))
    Files.createFile(projectDir.resolve("sass-lib/foo.scss"))
    val result = runGradle("--configuration-cache", "compileCustomSass", "compileWithLoadPath")

    assertThat(result.output).contains("Reusing configuration cache.")
    assertThat(result.task(":compileCustomSass")!!.outcome)
      .isEqualTo(TaskOutcome.UP_TO_DATE)
    assertThat(result.task(":compileWithLoadPath")!!.outcome)
      .isEqualTo(TaskOutcome.SUCCESS)
    assertThat(commandHistory).content().hasLineCount(3)
  }

  @Test
  fun `should support build cache when installing sass`() {
    runGradle("--build-cache", "installSass")
    deleteDirectory(projectDir.resolve(".gradle/sass"))
    runGradle("--build-cache", "installSass")

    server.verify(1, getRequestedFor(anyUrl()))
  }

  @Test
  fun `should support build cache when compiling sass`() {
    runGradle("--build-cache", "compileCustomSass")
    deleteDirectory(projectDir.resolve("build"))
    runGradle("--build-cache", "compileCustomSass")

    assertThat(commandHistory).content().hasLineCount(1)
  }

  @Test
  fun `should support legacy names`() {
    runGradle("legacyNames")
    assertThat(commandHistory)
      .hasContent("sass --style=expanded --embed-source-map --source-map-urls=relative $dir/src/main/sass:$dir/build/sass")
  }

  /**
   * Creates a fake sass executable that will log its command line to
   * [commandHistory].
   */
  @BeforeEach
  fun createExecutable() {
    val sassDir = projectDir.resolve(".gradle/sass/some.specific.version/dart-sass")
    Files.createDirectories(sassDir)
    Files.createDirectories(projectDir.resolve("src/main/sass"))
    if (OS.current() == OS.WINDOWS) {
      val sass = sassDir.resolve("sass.bat")
      Files.newBufferedWriter(sass, StandardOpenOption.CREATE).use { writer ->
        writeExecutable(writer)
      }
    } else {
      val sass = sassDir.resolve("sass")
      Files.createFile(
        sass, PosixFilePermissions.asFileAttribute(
          PosixFilePermissions.fromString("rwxr-xr-x")
        )
      )
      Files.newBufferedWriter(sass).use { writer ->
        writeExecutable(writer)
      }
    }
  }

  private fun createArchive(comment: String? = null): ByteArray {
    val archive = ByteArrayOutputStream()
    if (OS.current() == OS.WINDOWS) {
      ZipOutputStream(archive).use { zip ->
        zip.putNextEntry(ZipEntry("dart-sass/sass.bat"))
        OutputStreamWriter(zip).use { writer ->
          writeExecutable(writer, comment)
        }
      }
    } else {
      GZIPOutputStream(archive).use { gzip ->
        TarArchiveOutputStream(gzip).use { tar ->
          val entry = TarArchiveEntry("dart-sass/sass")
          val data = ByteArrayOutputStream().let { exec ->
            OutputStreamWriter(exec).use { writer ->
              writeExecutable(writer, comment)
            }
            exec.toByteArray()
          }
          entry.mode = EXEC_FILE_MODE
          entry.size = data.size.toLong()
          tar.putArchiveEntry(entry)
          tar.write(data)
          tar.closeArchiveEntry()
        }
      }
    }
    return archive.toByteArray()
  }

  private fun writeExecutable(writer: Writer, comment: String? = null) {
    if (OS.current() == OS.WINDOWS) {
      comment?.let { writer.write("REM $it\n") }
      writer.write("@echo sass %* >> $commandHistory\n")
    } else {
      writer.write("#!/bin/sh\n")
      comment?.let { writer.write("# $it\n") }
      writer.write("echo sass $@ >> $commandHistory\n")
    }
  }

  /**
   * The real path of the project directory, as seen by the sass executable.
   */
  private val dir: Path
    get() = projectDir.toRealPath()

  /**
   * File containing the history of sass commands executed, one per line.
   */
  private val commandHistory: Path
    get() = projectDir.resolve("sass-history.txt")

  /**
   * Runs Gradle in the project directory with the given arguments.
   */
  private fun runGradle(vararg arguments: String): BuildResult =
    runGradle(projectDir, mapOf("URL" to server.baseUrl()), *arguments)

  /**
   * Deletes the given directory and all its contents.
   */
  private fun deleteDirectory(directory: Path) {
    Files.walkFileTree(directory, object : FileVisitor<Path> {
      override fun preVisitDirectory(
        dir: Path,
        attrs: BasicFileAttributes
      ): FileVisitResult {
        return FileVisitResult.CONTINUE
      }

      override fun visitFile(
        file: Path,
        attrs: BasicFileAttributes
      ): FileVisitResult {
        Files.delete(file)
        return FileVisitResult.CONTINUE
      }

      override fun visitFileFailed(
        file: Path,
        e: IOException
      ): FileVisitResult {
        throw e
      }

      override fun postVisitDirectory(
        dir: Path,
        e: IOException?
      ): FileVisitResult {
        if (e != null) {
          throw e
        }
        Files.delete(dir)
        return FileVisitResult.CONTINUE
      }
    })
  }

}
