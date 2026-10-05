package io.miret.etienne.gradle.sass

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.zip.GZIPOutputStream

class MultiProjectTest {

  companion object {
    private const val LOREM_COPIES = 100

    private val LOREM_IPSUM: ByteArray =
      checkNotNull(MultiProjectTest::class.java.getResourceAsStream("lorem-ipsum.txt")) {
        "lorem-ipsum.txt not found"
      }.use { it.readAllBytes() }
  }

  @TempDir
  lateinit var projectDir: Path

  private lateinit var server: WireMockServer

  @BeforeEach
  fun startServer() {
    server = WireMockServer(options().dynamicPort())
    server.start()

    val header = """
      #!/bin/sh
      
      for LAST_ARG; do true; done
      DIR="${"$"}{LAST_ARG#*:}"
      
      mkdir -p "${"$"}DIR"
      
      cat > "${"$"}DIR/style.css" <<EOF
      
      """.trimIndent().toByteArray(StandardCharsets.US_ASCII)
    val footer = "EOF\n".toByteArray(StandardCharsets.US_ASCII)

    val archive = ByteArrayOutputStream()
    GZIPOutputStream(archive).use { gzip ->
      TarArchiveOutputStream(gzip).use { tar ->
        val entry = TarArchiveEntry("dart-sass/sass")
        entry.size = header.size + LOREM_IPSUM.size.toLong() * LOREM_COPIES + footer.size
        entry.mode = EXEC_FILE_MODE
        tar.putArchiveEntry(entry)
        tar.write(header)
        repeat(LOREM_COPIES) {
          tar.write(LOREM_IPSUM)
        }
        tar.write(footer)
        tar.closeArchiveEntry()
      }
    }
    server.stubFor(
      get(urlMatching("/42.0/dart-sass-.*"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(archive.toByteArray())
        )
    )
  }

  @AfterEach
  fun stopServer() {
    server.stop()
  }

  @BeforeEach
  fun setupProject() {
    val files = listOf(
      "app/build.gradle",
      "app/src/main/sass/foo.scss",
      "lib/build.gradle",
      "lib/src/main/sass/bar.scss",
      "build.gradle",
      "settings.gradle",
    )

    copyResources("/io/miret/etienne/gradle/sass/multi-project", files, projectDir)
  }

  @Test
  fun `should install and run sass`() {
    val expected = String(LOREM_IPSUM, StandardCharsets.US_ASCII)
      .repeat(LOREM_COPIES)

    GradleRunner.create()
      .withPluginClasspath()
      .withEnvironment(mapOf("URL" to server.baseUrl()))
      .withArguments(":app:compileSass", ":lib:compileSass")
      .withProjectDir(projectDir.toFile())
      .build()

    assertThat(projectDir.resolve("app/build/sass/style.css"))
      .hasContent(expected)
    assertThat(projectDir.resolve("lib/build/sass/style.css"))
      .hasContent(expected)
  }

}
