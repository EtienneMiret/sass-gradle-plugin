package io.miret.etienne.gradle.sass

import org.gradle.api.Project
import java.io.File

open class SassGradlePluginExtension(project: Project) {

  var version: String? = "1.54.0"

  var directory: File?

  var baseUrl: String? = "https://github.com/sass/dart-sass/releases/download"

  var autoCopy: Boolean? = true

  init {
    val projectPath = project.rootDir
      .toPath()
      .relativize(project.projectDir.toPath())
    this.directory = project.rootDir
      .toPath()
      .resolve(".gradle/sass")
      .resolve(projectPath)
      .toFile()
  }

  fun noAutoCopy() {
    this.autoCopy = false
  }

}
