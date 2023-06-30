package io.miret.etienne.gradle.sass;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class MultiProjectTest {

  private static final int LOREM_COPIES = 100;

  @TempDir
  Path projectDir;

  private WireMockServer server;

  @BeforeEach
  void startServer() throws IOException {
    server = new WireMockServer(options().dynamicPort());
    server.start();

    ByteArrayOutputStream archive = new ByteArrayOutputStream();
    try (
        GZIPOutputStream gzip = new GZIPOutputStream(archive);
        TarArchiveOutputStream tar = new TarArchiveOutputStream(gzip);
    ) {
      TarArchiveEntry entry = new TarArchiveEntry("dart-sass/sass");
      entry.setSize(12908 * LOREM_COPIES + 111); // lorem ipsum size + script size
      entry.setMode(0755);
      tar.putArchiveEntry(entry);
      tar.write(("#!/bin/sh\n" +
          "\n" +
          "for LAST_ARG; do true; done\n" +
          "DIR=\"${LAST_ARG#*:}\"\n" +
          "\n" +
          "mkdir -p \"$DIR\"\n" +
          "\n" +
          "cat > \"$DIR/style.css\" <<EOF\n"
      ).getBytes(StandardCharsets.US_ASCII));
      for (int i = 0; i < LOREM_COPIES; i++) {
        try (InputStream loremIpsum = MultiProjectTest.class.getResourceAsStream("lorem-ipsum.txt")) {
          ByteStreams.copy(requireNonNull(loremIpsum, "lorem-ipsum.txt"), tar);
        }
      }
      tar.write("EOF\n".getBytes(StandardCharsets.US_ASCII));
      tar.closeArchiveEntry();
    }

    server.stubFor(get(urlMatching("/42.0/dart-sass-.*"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(archive.toByteArray())
        )
    );
  }

  @AfterEach
  void stopServer() {
    server.stop();
  }

  @BeforeEach
  void setupProject() throws IOException {
    List<String> files = ImmutableList.of(
        "app/build.gradle",
        "app/src/main/sass/foo.scss",
        "lib/build.gradle",
        "lib/src/main/sass/bar.scss",
        "build.gradle",
        "settings.gradle"
    );

    Utils.copy("/io/miret/etienne/gradle/sass/multi-project", files, projectDir);
  }

  @Test
  void should_install_and_run_sass() throws IOException {
    String expected;
    try (
        InputStream input = MultiProjectTest.class.getResourceAsStream("lorem-ipsum.txt");
        Reader readable = new InputStreamReader(input, StandardCharsets.US_ASCII)
    ) {
      String loremIpsum = CharStreams.toString(readable);
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < LOREM_COPIES; i++) {
        builder.append(loremIpsum);
      }
      expected = builder.toString();
    }

    GradleRunner runner = GradleRunner.create();
    runner.withPluginClasspath();
    runner.withEnvironment(singletonMap("URL", server.baseUrl()));
    runner.withArguments(":app:compileSass", ":lib:compileSass");
    runner.withProjectDir(projectDir.toFile());
    runner.build();

    assertThat(projectDir.resolve("app/build/sass/style.css"))
        .hasContent(expected);
    assertThat(projectDir.resolve("lib/build/sass/style.css"))
        .hasContent(expected);
  }

}
