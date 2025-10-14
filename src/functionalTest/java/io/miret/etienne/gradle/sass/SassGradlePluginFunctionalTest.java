package io.miret.etienne.gradle.sass;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.common.io.ByteStreams;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.util.Collections.singletonMap;
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

  @BeforeEach
  void setupProject () throws IOException {
    Files.createFile (projectDir.resolve ("settings.gradle"));
    try (InputStream input = SassGradlePluginFunctionalTest.class.getResourceAsStream ("build.gradle")) {
      Files.copy (input, projectDir.resolve ("build.gradle"));
    }
  }

  @Test
  void should_install_sass () throws IOException {
    String archive = Os.isFamily (Os.FAMILY_WINDOWS) ? "archive.zip" : "archive.tgz";
    try (InputStream input = SassGradlePluginFunctionalTest.class.getResourceAsStream (archive)) {
      server.stubFor (get (urlMatching ("/some.specific.version/dart-sass-.*"))
          .willReturn (aResponse ()
              .withStatus (200)
              .withBody (ByteStreams.toByteArray (input))
          ));
    }

    GradleRunner runner = GradleRunner.create ();
    runner.withPluginClasspath ();
    runner.withEnvironment (singletonMap ("URL", server.baseUrl ()));
    runner.withArguments ("installSass");
    runner.withProjectDir (projectDir.toFile ());
    runner.build ();

    assertThat (projectDir.resolve (".gradle/sass/archive"))
        .isNotEmptyDirectory ();
    assertThat (projectDir.resolve (".gradle/sass/some.specific.version/dart-sass/sass"))
        .hasContent ("foo\nbar\nbaz\n");
  }

  @Test
  void should_download_specified_version () throws IOException {
    String archive = Os.isFamily (Os.FAMILY_WINDOWS) ? "archive.zip" : "archive.tgz";
    try (InputStream input = SassGradlePluginFunctionalTest.class.getResourceAsStream (archive)) {
      server.stubFor (get (anyUrl ())
          .willReturn (ok ()
              .withStatus (200)
              .withBody (ByteStreams.toByteArray (input))
          ));
    }

    GradleRunner.create ()
        .withPluginClasspath ()
        .withProjectDir (projectDir.toFile ())
        .withEnvironment (singletonMap ("URL", server.baseUrl ()))
        .withArguments ("downloadSass")
        .build ();

    server.verify (getRequestedFor (urlMatching ("/some\\.specific\\.version/dart-sass-some\\.specific\\.version-.*")));
  }

  @Test
  void should_compile_sass () throws IOException {
    GradleRunner.create ()
        .withPluginClasspath ()
        .withArguments ("compileCustomSass")
        .withProjectDir (projectDir.toFile ())
        .build ();

    assertThat(commandHistory()).hasContent(String.format(
        "sass --style=expanded --source-map-urls=relative %1$s/src/main/sass:%1$s/build/sass",
        projectDir.toRealPath ()
    ));
  }

  @Test
  void should_set_loadPaths () throws IOException {
    GradleRunner.create ()
        .withPluginClasspath ()
        .withArguments ("compileWithLoadPath")
        .withProjectDir (projectDir.toFile ())
        .build ();

    assertThat(commandHistory()).hasContent(String.format(
        "sass --load-path=%1$s/sass-lib --load-path=/var/lib/compass --style=expanded --source-map-urls=relative %1$s/src/main/sass:%1$s/build/sass",
        projectDir.toRealPath ()
    ));
  }

  @Test
  void should_use_compressed_style () throws IOException {
    GradleRunner.create ()
        .withPluginClasspath ()
        .withArguments ("compileCompressed")
        .withProjectDir (projectDir.toFile ())
        .build ();

    assertThat(commandHistory()).hasContent(String.format(
       "sass --style=compressed --source-map-urls=relative %1$s/src/main/sass:%1$s/build/sass",
        projectDir.toRealPath ()
    ));
  }

  @Test
  void should_watch () throws IOException {
    GradleRunner.create ()
        .withPluginClasspath ()
        .withArguments ("watch")
        .withProjectDir (projectDir.toFile ())
        .build();

    assertThat(commandHistory()).hasContent(String.format(
       "sass --style=expanded --watch --source-map-urls=relative %1$s/src/main/sass:%1$s/build/sass",
       projectDir.toRealPath ()
    ));
  }

  @Test
  void should_disable_charset_output () throws IOException {
    GradleRunner.create ()
        .withPluginClasspath ()
        .withArguments ("compileNoCharset")
        .withProjectDir (projectDir.toFile ())
        .build ();

    assertThat(commandHistory()).hasContent(String.format(
        "sass --style=expanded --no-charset --source-map-urls=relative %1$s/src/main/sass:%1$s/build/sass",
        projectDir.toRealPath ()
    ));
  }

  @Test
  void should_disable_error_css_output () throws IOException {
    GradleRunner.create ()
        .withPluginClasspath ()
        .withArguments ("compileNoErrorCss")
        .withProjectDir (projectDir.toFile ())
        .build ();

    assertThat(commandHistory()).hasContent(String.format(
       "sass --style=expanded --no-error-css --source-map-urls=relative %1$s/src/main/sass:%1$s/build/sass",
       projectDir.toRealPath ()
    ));
  }

  @Test
  void should_disable_source_map_with_absolute_urls () throws IOException {
    GradleRunner.create ()
        .withPluginClasspath ()
        .withArguments ("compileNoSourceMapAbsolute")
        .withProjectDir (projectDir.toFile ())
        .build ();

    assertThat(commandHistory()).hasContent(String.format(
       "sass --style=expanded --no-source-map %1$s/src/main/sass:%1$s/build/sass",
       projectDir.toRealPath ()
    ));
  }

  @Test
  void should_disable_source_map_with_relative_urls () throws IOException {
    GradleRunner.create ()
        .withPluginClasspath ()
        .withArguments ("compileNoSourceMapRelative")
        .withProjectDir (projectDir.toFile ())
        .build ();

    assertThat(commandHistory()).hasContent(String.format(
        "sass --style=expanded --no-source-map %1$s/src/main/sass:%1$s/build/sass",
        projectDir.toRealPath ()
    ));
  }

  @Test
  void should_create_embed_source_map_with_absolute_urls () throws IOException {
    GradleRunner.create ()
        .withPluginClasspath ()
        .withArguments ("compileEmbedSourceMapAbsolute")
        .withProjectDir (projectDir.toFile ())
        .build ();

    assertThat(commandHistory()).hasContent(String.format(
       "sass --style=expanded --embed-source-map --source-map-urls=absolute %1$s/src/main/sass:%1$s/build/sass",
       projectDir.toRealPath ()
    ));
  }

  @Test
  void should_create_embed_source_map_with_relative_urls () throws Exception {
    GradleRunner.create ()
        .withPluginClasspath ()
        .withArguments ("compileEmbedSourceMapRelative")
        .withProjectDir (projectDir.toFile ())
        .build ();

    assertThat(commandHistory()).hasContent(String.format(
        "sass --style=expanded --embed-source-map --source-map-urls=relative %1$s/src/main/sass:%1$s/build/sass",
        projectDir.toRealPath ()
    ));
  }

  @Test
  void should_create_file_source_map_with_absolute_urls () throws IOException {
    GradleRunner.create ()
        .withPluginClasspath ()
        .withArguments ("compileFileSourceMapAbsolute")
        .withProjectDir (projectDir.toFile ())
        .build ();

    assertThat(commandHistory()).hasContent(String.format(
        "sass --style=expanded --source-map-urls=absolute %1$s/src/main/sass:%1$s/build/sass",
        projectDir.toRealPath ()
    ));
  }

  @Test
  void should_create_file_source_map_with_relative_urls () throws IOException {
    GradleRunner.create ()
        .withPluginClasspath ()
        .withArguments ("compileFileSourceMapRelative")
        .withProjectDir (projectDir.toFile ())
        .build ();

    assertThat(commandHistory()).hasContent(String.format(
        "sass --style=expanded --source-map-urls=relative %1$s/src/main/sass:%1$s/build/sass",
        projectDir.toRealPath ()
    ));
  }

  @Test
  void should_be_quiet () throws IOException {
    GradleRunner.create ()
        .withPluginClasspath ()
        .withArguments ("quiet")
        .withProjectDir (projectDir.toFile ())
        .build();

    assertThat(commandHistory()).hasContent(String.format(
        "sass --style=expanded --quiet --source-map-urls=relative %1$s/src/main/sass:%1$s/build/sass",
        projectDir.toRealPath ()
    ));
  }

  @Test
  void should_support_Gradle_configuration_cache() {
    GradleRunner.create()
        .withPluginClasspath()
        .withArguments("--configuration-cache", "compileCustomSass")
        .withProjectDir(projectDir.toFile())
        .build();
  }

  /**
   * Creates a fake sass executable, that will log it’s command line to
   * {@link #commandHistory()}.
   */
  @BeforeEach
  void createExecutable() throws IOException {
    Path out = commandHistory();
    Path sassDir = projectDir.resolve (".gradle/sass/some.specific.version/dart-sass");
    Files.createDirectories (sassDir);
    Files.createDirectories (projectDir.resolve ("src/main/sass"));
    if (Os.isFamily (Os.FAMILY_WINDOWS)) {
      Path sass = sassDir.resolve ("sass.bat");
      try (Writer writer = Files.newBufferedWriter (sass, StandardOpenOption.CREATE)) {
        writer.write(String.format("@echo sass %%* >> %s\n", out));
      }
    } else {
      Path sass = sassDir.resolve ("sass");
      Files.createFile (sass, PosixFilePermissions.asFileAttribute (
          PosixFilePermissions.fromString ("rwxr-xr-x")
      ));
      try (Writer writer = Files.newBufferedWriter (sass)) {
        writer.write ("#!/bin/sh\n");
        writer.write(String.format("echo sass $@ >> %s\n", out));
      }
    }
  }

  /**
   * @return A file containing the history of sass commands executed, one
   * per line.
   */
  private Path commandHistory() {
    return projectDir.resolve("build/sass/history");
  }

}
