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
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

class SassGradlePluginFunctionalTest {

  private static final Set<PosixFilePermission> EXECUTABLE_PERMISSIONS = Set.of (
      PosixFilePermission.OWNER_READ,
      PosixFilePermission.OWNER_WRITE,
      PosixFilePermission.OWNER_EXECUTE
  );

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

  @Test
  void should_compile_sass () throws IOException {
    Path out = createExecutable ();

    GradleRunner.create ()
        .withPluginClasspath ()
        .withArguments ("compileCustomSass")
        .withProjectDir (projectDir.toFile ())
        .build ();

    assertThat (out).hasContent (
        String.format ("sass --style=expanded %1$s/src/main/sass:%1$s/build/sass", projectDir.toRealPath ())
    );
  }

  @Test
  void should_set_loadPaths () throws IOException {
    Path out = createExecutable ();

    GradleRunner.create ()
        .withPluginClasspath ()
        .withArguments ("compileWithLoadPath")
        .withProjectDir (projectDir.toFile ())
        .build ();

    assertThat (out).hasContent (String.format (
        "sass --load-path=%1$s/sass-lib --load-path=/var/lib/compass --style=expanded %1$s/src/main/sass:%1$s/build/sass",
        projectDir.toRealPath ()
    ));
  }

  @Test
  void should_use_compressed_style () throws IOException {
    Path out = createExecutable ();

    GradleRunner.create ()
        .withPluginClasspath ()
        .withArguments ("compileCompressed")
        .withProjectDir (projectDir.toFile ())
        .build ();

    assertThat (out).hasContent (String.format (
       "sass --style=compressed %1$s/src/main/sass:%1$s/build/sass",
        projectDir.toRealPath ()
    ));
  }

  private Path createExecutable () throws IOException {
    Path out = projectDir.resolve ("build/out");
    Path sassDir = projectDir.resolve (".gradle/sass/dart-sass");
    Files.createDirectories (sassDir);
    Files.createDirectories (projectDir.resolve ("src/main/sass"));
    if (Os.isFamily (Os.FAMILY_WINDOWS)) {
      Path sass = sassDir.resolve ("sass.bat");
      try (Writer writer = Files.newBufferedWriter (sass, StandardOpenOption.CREATE)) {
        writer.write (String.format ("@echo sass %%* > %s\n", out));
      }
    } else {
      Path sass = sassDir.resolve ("sass");
      Files.createFile (sass, PosixFilePermissions.asFileAttribute (
          PosixFilePermissions.fromString ("rwxr-xr-x")
      ));
      try (Writer writer = Files.newBufferedWriter (sass)) {
        writer.write ("#!/bin/sh\n");
        writer.write (String.format ("echo sass $@ > %s\n", out));
      }
    }
    return out;
  }

}
