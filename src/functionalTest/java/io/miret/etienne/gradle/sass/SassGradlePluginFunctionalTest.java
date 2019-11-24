package io.miret.etienne.gradle.sass;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SassGradlePluginFunctionalTest {

  @TempDir
  Path projectDir;

  @Test
  void canRunTask () throws IOException {
    Files.createFile (projectDir.resolve ("settings.gradle"));
    try (InputStream input = SassGradlePluginFunctionalTest.class.getResourceAsStream ("build.gradle")) {
      Files.copy (input, projectDir.resolve ("build.gradle"));
    }

    GradleRunner runner = GradleRunner.create ();
    runner.withPluginClasspath ();
    runner.withArguments ("installSass");
    runner.withProjectDir (projectDir.toFile ());
    runner.build ();

    assertThat (projectDir.resolve (".gradle/sass/archive"))
        .isNotEmptyDirectory ();
    assertThat (projectDir.resolve (".gradle/sass/dart-sass"))
        .isNotEmptyDirectory ();
  }

}
