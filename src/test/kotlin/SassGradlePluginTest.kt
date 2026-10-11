package io.miret.etienne.gradle.sass

import de.undercouch.gradle.tasks.download.Download
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.tasks.Copy
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class SassGradlePluginTest {

  @Test
  fun pluginRegistersATask() {
    val project = ProjectBuilder.builder().build()

    project.plugins.apply("io.miret.etienne.sass")

    assertThat(project.tasks.findByName("downloadSass"))
      .isInstanceOf(Download::class.java)
    assertThat(project.tasks.findByName("installSass"))
      .isInstanceOf(Copy::class.java)
    assertThat(project.tasks.findByName("compileSass"))
      .isInstanceOf(CompileSass::class.java)
  }

}
