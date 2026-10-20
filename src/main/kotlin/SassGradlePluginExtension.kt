package io.miret.etienne.gradle.sass

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

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
    val dir = project.layout
      .projectDirectory
      .dir("${project.rootDir}/.gradle/sass/$projectPath")
    version.convention("1.105.2")
    directory.convention(dir)
    baseUrl.convention("https://github.com/sass/dart-sass/releases/download")
    autoCopy.convention(true)
  }

}
