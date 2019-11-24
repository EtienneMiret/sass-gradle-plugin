package io.miret.etienne.gradle.sass;

import de.undercouch.gradle.tasks.download.Download;
import org.gradle.api.Project;
import org.gradle.api.tasks.Copy;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SassGradlePluginTest {

  @Test
  void pluginRegistersATask () {
    Project project = ProjectBuilder.builder ().build ();
    project.getPlugins ().apply ("io.miret.etienne.sass");

    assertThat (project.getTasks ().findByName ("downloadSass"))
        .isInstanceOf (Download.class);
    assertThat (project.getTasks ().findByName ("installSass"))
        .isInstanceOf (Copy.class);
    assertThat (project.getTasks ().findByName ("compileSass"))
        .isInstanceOf (CompileSass.class);
  }

}
