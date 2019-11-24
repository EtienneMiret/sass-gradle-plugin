package io.miret.etienne.gradle.sass;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SassGradlePluginFunctionalTest {

  @Test
  void canRunTask () throws IOException {
    File projectDir = new File ("build/functionalTest");
    Files.createDirectories (projectDir.toPath ());
    writeString (new File (projectDir, "settings.gradle"), "");
    writeString (new File (projectDir, "build.gradle"),
        "plugins {" +
            "  id('io.miret.etienne.sass')" +
            "}");

    GradleRunner runner = GradleRunner.create ();
    runner.forwardOutput ();
    runner.withPluginClasspath ();
    runner.withArguments ("greeting");
    runner.withProjectDir (projectDir);
    BuildResult result = runner.build ();

    assertTrue (result.getOutput ().contains ("Hello from plugin 'io.miret.etienne.gradle.sass.greeting'"));
  }

  private void writeString (File file, String string) throws IOException {
    try (Writer writer = new FileWriter (file)) {
      writer.write (string);
    }
  }

}
