package io.miret.etienne.gradle.sass;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

class SassGradlePluginFunctionalTest {

  @TempDir
  Path projectDir;

  private WireMockServer server;

  @BeforeEach
  void startServer () {
    server = new WireMockServer (options ().dynamicPort ());
    server.start ();
  }

  @AfterEach
  void stopServer () {
    server.stop ();
  }

  @Test
  void canRunTask () throws IOException {
    Files.createFile (projectDir.resolve ("settings.gradle"));
    try (InputStream input = SassGradlePluginFunctionalTest.class.getResourceAsStream ("build.gradle")) {
      Files.copy (input, projectDir.resolve ("build.gradle"));
    }
    String archive = Os.isFamily (Os.FAMILY_WINDOWS) ? "archive.zip" : "archive.tgz";
    try (InputStream input = SassGradlePluginFunctionalTest.class.getResourceAsStream (archive)) {
      server.stubFor (get (urlMatching ("/some.specific.version/dart-sass-.*"))
          .willReturn (aResponse ()
              .withStatus (200)
              .withBody (input.readAllBytes ())
          ));
    }

    GradleRunner runner = GradleRunner.create ();
    runner.withPluginClasspath ();
    runner.withEnvironment (Map.of ("URL", server.baseUrl ()));
    runner.withArguments ("installSass");
    runner.withProjectDir (projectDir.toFile ());
    runner.build ();

    assertThat (projectDir.resolve (".gradle/sass/archive"))
        .isNotEmptyDirectory ();
    assertThat (projectDir.resolve (".gradle/sass/dart-sass/sass"))
        .hasContent ("foo\nbar\nbaz\n");
  }

}
