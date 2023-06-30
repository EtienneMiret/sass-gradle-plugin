package io.miret.etienne.gradle.sass;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.common.collect.ImmutableList;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.miret.etienne.gradle.sass.Utils.copy;
import static io.miret.etienne.gradle.sass.Utils.createArchive;
import static java.util.Collections.singletonMap;

/**
 * Test applying this plugin in multiple subprojects
 * instead of only the root project.
 */
@ExtendWith(SoftAssertionsExtension.class)
public class MultipleApplyTest {

  @TempDir
  Path projectDir;

  @InjectSoftAssertions
  private SoftAssertions softly;

  private WireMockServer server;

  @BeforeEach
  void startAndSetupWiremockServer () throws IOException {
    server = new WireMockServer(options().dynamicPort());
    server.start();
    server.stubFor(get(urlMatching("/42.0/dart-sass-.*"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(createArchive())
        )
    );
  }

  @BeforeEach
  void setupProject() throws IOException {
    List<String> projectResources = ImmutableList.of(
        "a/build.gradle",
        "b/build.gradle",
        "c/build.gradle",
        "build.gradle",
        "settings.gradle"
    );
    copy("/io/miret/etienne/gradle/sass/multiple-apply", projectResources, projectDir);
  }

  @Test
  void should_not_create_dot_gradle_directories_in_subprojects() {
    GradleRunner runner = GradleRunner.create();
    runner.withPluginClasspath();
    runner.withEnvironment(singletonMap("URL", server.baseUrl()));
    runner.withArguments("installSass");
    runner.withProjectDir(projectDir.toFile());
    runner.build();

    softly.assertThat(projectDir.resolve("a/.gradle")).doesNotExist();
    softly.assertThat(projectDir.resolve("b/.gradle")).doesNotExist();
    softly.assertThat(projectDir.resolve("c/.gradle")).doesNotExist();
  }

}
