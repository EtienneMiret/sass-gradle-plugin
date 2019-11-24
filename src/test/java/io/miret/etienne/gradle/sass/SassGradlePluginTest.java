package io.miret.etienne.gradle.sass;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SassGradlePluginTest {

  @Test
  void pluginRegistersATask () {
    Project project = ProjectBuilder.builder ().build ();
    project.getPlugins ().apply ("io.miret.etienne.sass");

    assertNotNull (project.getTasks ().findByName ("greeting"));
  }

}
