package io.miret.etienne.gradle.sass

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * Test applying this plugin in multiple subprojects
 * instead of only the root project.
 */
@ExtendWith(SoftAssertionsExtension::class)
class MultipleApplyTest {

  @TempDir
  lateinit var projectDir: Path

  @InjectSoftAssertions
  private lateinit var softly: SoftAssertions

  private lateinit var server: WireMockServer

  @BeforeEach
  fun startAndSetupWiremockServer() {
    server = WireMockServer(options().dynamicPort())
    server.start()
    server.stubFor(
      get(urlMatching("/42.0/dart-sass-.*"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(createArchive())
        )
    )
  }

  @AfterEach
  fun stopWiremockServer() {
    server.stop()
  }

  @BeforeEach
  fun setupProject() {
    val projectResources = listOf(
      "a/build.gradle",
      "b/build.gradle",
      "c/build.gradle",
      "build.gradle",
      "settings.gradle",
    )
    copyResources("/io/miret/etienne/gradle/sass/multiple-apply", projectResources, projectDir)
  }

  @Test
  fun `should not create dot gradle directories in subprojects`() {
    GradleRunner.create()
      .withPluginClasspath()
      .withEnvironment(mapOf("URL" to server.baseUrl()))
      .withArguments("installSass")
      .withProjectDir(projectDir.toFile())
      .build()

    softly.assertThat(projectDir.resolve("a/.gradle")).doesNotExist()
    softly.assertThat(projectDir.resolve("b/.gradle")).doesNotExist()
    softly.assertThat(projectDir.resolve("c/.gradle")).doesNotExist()
  }

}
