package io.miret.etienne.gradle.sass

import de.undercouch.gradle.tasks.download.Download
import org.apache.tools.ant.taskdefs.condition.Os
import org.apache.tools.ant.taskdefs.condition.Os.FAMILY_MAC
import org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.War
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import java.nio.file.Files
import java.nio.file.Path

class SassGradlePlugin : Plugin<Project> {

  override fun apply(project: Project) {
    val extension = project.extensions
      .create<SassGradlePluginExtension>("sass", project)

    val downloadSass = project.tasks
      .register("downloadSass", Download::class) {
        val archive = extension.directory
          .zip(extension.version) { dir, version ->
            dir.file("archive/${archiveName(version)}")
          }
        val srcUrl = extension.baseUrl
          .zip(extension.version) { baseUrl, version ->
            "$baseUrl/$version/${archiveName(version)}"
          }
        description = "Download a sass archive."
        src(srcUrl)
        dest(archive)
        tempAndMove(true)
        overwrite(false)
        onlyIf { !(it as Download).dest.exists() }
        outputs.cacheIf { true }
      }
    val installSass = project.tasks
      .register("installSass", Copy::class) {
        val downloadedFiles = downloadSass
          .map { it.dest }
          .map {
            if (Os.isFamily(FAMILY_WINDOWS))
              project.zipTree(it)
            else
              project.tarTree(it)
          }
        description = "Unpack and install a sass archive."
        dependsOn(downloadSass)
        from(downloadedFiles)
        into(extension.directory.dir(extension.version))
        outputs.cacheIf { true }
      }
    compileSass(project, extension, installSass)
    project.subprojects.forEach {
      compileSass(it, extension, installSass)
    }
  }

  private fun compileSass(
    project: Project,
    extension: SassGradlePluginExtension,
    installSass: TaskProvider<*>,
  ) {
    val compileSass = project.tasks
      .register("compileSass", CompileSass::class) {
        description = "Compile sass and scss."
        dependsOn(installSass)
      }
    project.tasks
      .withType(War::class)
      .configureEach {
        val taskProvider = extension.autoCopy
          .zip(compileSass) { copy, sass ->
            if (copy) listOf(sass) else emptyList()
          }
        val fileProvider = taskProvider.map { tasks ->
          tasks.map { task -> task.outputDir }
        }
        dependsOn(taskProvider)
        from(fileProvider)
      }
  }

  private fun archiveName(version: String) =
    "dart-sass-$version-$osName-$archName$libcSuffix.$archiveExtension"

  private val osName: String
    get() = when {
      Os.isFamily(FAMILY_WINDOWS) -> "windows"
      Os.isFamily(FAMILY_MAC) -> "macos"
      isAndroid -> "android"
      else -> "linux"
    }

  /**
   * On Android (e.g. under Termux), the JVM reports itself as Linux, so
   * look for the Android runtime launcher instead.
   */
  private val isAndroid: Boolean
    get() = Files.exists(Path.of("/system/bin/app_process"))

  private val archName: String
    get() {
      val arch = System.getProperty("os.arch").lowercase()
      return when (arch) {
        "aarch64", "arm64" -> "arm64"
        "amd64", "x86_64", "x64" -> "x64"
        "x86", "i386", "i486", "i586", "i686", "ia32" -> "ia32"
        "riscv64" -> "riscv64"
        else -> when {
          arch.startsWith("arm") -> "arm"
          else -> throw GradleException(
            "Unsupported architecture for Dart Sass: $arch"
          )
        }
      }
    }

  /**
   * Dart Sass publishes separate Linux builds for musl-based distributions
   * (such as Alpine), whose dynamic loader is `/lib/ld-musl-<arch>.so.1`.
   */
  private val libcSuffix: String
    get() {
      val lib = Path.of("/lib")
      if (osName != "linux" || !Files.isDirectory(lib)) return ""
      val musl = Files.list(lib).use { paths ->
        paths.anyMatch { it.fileName.toString().startsWith("ld-musl-") }
      }
      return if (musl) "-musl" else ""
    }

  private val archiveExtension: String
    get() = if (Os.isFamily(FAMILY_WINDOWS)) "zip" else "tar.gz"

}
