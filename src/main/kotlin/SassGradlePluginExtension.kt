package io.miret.etienne.gradle.sass

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import java.io.File

abstract class SassGradlePluginExtension(project: Project) {

  /**
   * The Dart Sass version to use.
   */
  abstract val version: Property<String>

  /**
   * The directory where to install Dart Sass.
   */
  abstract val directory: DirectoryProperty

  /**
   * The base URL to use when downloading Dart Sass.
   */
  abstract val baseUrl: Property<String>

  /**
   * Whether to automatically copy generated CSS files to WARs.
   */
  abstract val autoCopy: Property<Boolean>

  /**
   * Do not automatically copy generated CSS files to WARs.
   */
  fun noAutoCopy() {
    autoCopy.set(false)
  }

  init {
    val projectPath = project.rootDir
      .toPath()
      .relativize(project.projectDir.toPath())
    val dir = project.rootDir
      .toPath()
      .resolve(".gradle/sass")
      .resolve(projectPath)
      .toFile()
    version.convention("1.54.0")
    directory.convention(project.layout.dir(project.provider { dir }))
    baseUrl.convention("https://github.com/sass/dart-sass/releases/download")
    autoCopy.convention(true)
  }

}
