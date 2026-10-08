package io.miret.etienne.gradle.sass

import de.undercouch.gradle.tasks.download.Download
import org.apache.tools.ant.taskdefs.condition.Os
import org.apache.tools.ant.taskdefs.condition.Os.FAMILY_MAC
import org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.War
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import java.io.File

class SassGradlePlugin : Plugin<Project> {

  override fun apply(project: Project) {
    val extension = project.extensions
      .create<SassGradlePluginExtension>("sass", project)

    val downloadSass = project.tasks
      .register("downloadSass", Download::class) {
        val archiveName = archiveName(extension.actualVersion)
        val archive = extension.actualDirectory
          .toPath()
          .resolve("archive")
          .resolve(archiveName)
          .toFile()
        description = "Download a sass archive."
        src("${extension.actualBaseUrl}/${extension.actualVersion}/$archiveName")
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
        into(File(extension.actualDirectory, extension.actualVersion))
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
        if (extension.actualAutoCopy) {
          dependsOn(compileSass)
          from(compileSass.map { it.outputDir })
        }
      }
  }

  private fun archiveName(version: String) =
    "dart-sass-$version-$osName-$archName.$archiveExtension"

  private val osName: String
    get() = when {
      Os.isFamily(FAMILY_WINDOWS) -> "windows"
      Os.isFamily(FAMILY_MAC) -> "macos"
      else -> "linux"
    }

  private val archName: String
    get() {
      val arch = System.getProperty("os.arch")
      return when {
        arch.contains("arm") || arch.contains("aarch64")
          -> "arm64"
        arch.contains("64") -> "x64"
        else -> "ia32"
      }
    }

  private val archiveExtension: String
    get() = if (Os.isFamily(FAMILY_WINDOWS)) "zip" else "tar.gz"

}
