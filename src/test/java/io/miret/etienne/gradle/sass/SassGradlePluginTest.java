package io.miret.etienne.gradle.sass;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class SassGradlePluginTest {

  @Test
  public void pluginRegistersATask () {
    Project project = ProjectBuilder.builder ().build ();
    project.getPlugins ().apply ("io.miret.etienne.sass");

    assertNotNull (project.getTasks ().findByName ("greeting"));
  }

}
