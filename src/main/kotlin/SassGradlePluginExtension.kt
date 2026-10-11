package io.miret.etienne.gradle.sass

import org.gradle.api.Project
import java.io.File

open class SassGradlePluginExtension(project: Project) {

  var version: String? = "1.54.0"

  internal val actualVersion: String
    get() = checkNotNull(version) {
      "Use the default sass.version or set it to a non-null value."
    }

  var directory: File?

  internal val actualDirectory: File
    get() = checkNotNull(directory) {
      "Use the default sass.directory or set it to a non-null value."
    }

  var baseUrl: String? = "https://github.com/sass/dart-sass/releases/download"

  internal val actualBaseUrl: String
    get() = checkNotNull(baseUrl) {
      "Use the default sass.baseUrl or set it to a non-null value."
    }

  var autoCopy: Boolean? = true

  internal val actualAutoCopy: Boolean
    get() = checkNotNull(autoCopy) {
      "Use the default sass.autoCopy or set it to a non-null value."
    }

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
