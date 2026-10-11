package io.miret.etienne.gradle.sass

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream

class WithWarTest {

  @TempDir
  lateinit var projectDir: Path

  private lateinit var server: WireMockServer

  @BeforeEach
  fun startAndSetupServer() {
    server = startSassServer(urlMatching("/1.23.7/dart-sass-.*"), createArchive())
  }

  @AfterEach
  fun stopServer() {
    server.stop()
  }

  @BeforeEach
  fun setupProject() {
    Files.createDirectories(projectDir.resolve("src/main/sass"))
    WithWarTest::class.java.getResourceAsStream("settings-with-war.gradle").use { input ->
      Files.copy(input!!, projectDir.resolve("settings.gradle"))
    }
  }

  @Test
  fun `should include css in war`() {
    val entries = buildWar("build-with-war.gradle")

    assertThat(entries)
      .contains("style.css")
  }

  @Test
  fun `should put css in custom location`() {
    val entries = buildWar("war-custom-path.gradle")

    assertThat(entries)
      .contains("styles/style.css")
  }

  @Test
  fun `should not copy css to war`() {

    val entries = buildWar("war-no-auto-copy.gradle")

    assertThat(entries)
      .doesNotContain("style.css")
  }

  /**
   * Builds the war and returns the list of entries in the war.
   */
  private fun buildWar(buildGradle: String): List<String> {
    WithWarTest::class.java.getResourceAsStream(buildGradle).use { input ->
      Files.copy(input!!, projectDir.resolve("build.gradle"))
    }

    runGradle(projectDir, mapOf("URL" to server.baseUrl()), "assemble")

    Files.newInputStream(projectDir.resolve("build/libs/cool-webapp-1.0.0.war")).use { input ->
      ZipInputStream(input).use { zip ->
        return generateSequence { zip.getNextEntry() }
          .map { it.name }
          .toList()
      }
    }
  }

}
