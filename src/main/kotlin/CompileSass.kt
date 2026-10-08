package io.miret.etienne.gradle.sass

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.submit
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
open class CompileSass
@Inject constructor(
  private val workerExecutor: WorkerExecutor,
  private val objects: ObjectFactory,
) : DefaultTask() {

  enum class Style {
    expanded,
    compressed,
  }

  enum class SourceMap {
    none,
    embed,
    file,
  }

  enum class SourceMapUrls {
    relative,
    absolute,
  }

  val expanded: Style
    @Internal get() = Style.expanded

  val compressed: Style
    @Internal get() = Style.compressed

  val none: SourceMap
    @Internal get() = SourceMap.none

  val embed: SourceMap
    @Internal get() = SourceMap.embed

  val file: SourceMap
    @Internal get() = SourceMap.file

  val relative: SourceMapUrls
    @Internal get() = SourceMapUrls.relative

  val absolute: SourceMapUrls
    @Internal get() = SourceMapUrls.absolute

  /**
   * Resolved lazily, so that the `sass` extension can still be configured
   * after this task is created.
   */
  private val executableProvider: Provider<File>

  val executable: File
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    get() = executableProvider.get()

  @get:Input
  val entryPoints: MutableList<Pair<String, String>> = mutableListOf()

  @get:OutputDirectory
  var outputDir: File = project.layout.buildDirectory.dir("sass").get().asFile

  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  var sourceDir = File(project.projectDir, "src/main/sass")

  private val loadPaths: MutableList<File> = mutableListOf()

  @get:Input
  var destPath = "."

  @get:Input
  var style = Style.expanded

  @get:Input
  var watch = false
    private set

  @get:Input
  var charset = true
    private set

  @get:Input
  var errorCss = true
    private set

  @get:Input
  var quiet = false
    private set

  @get:Input
  var sourceMap = SourceMap.file

  @get:Input
  var sourceMapUrls = SourceMapUrls.relative

  /**
   * Gradle calls this getter while the task executes (to fingerprint the inputs),
   * so it must not touch [project]:
   * the configuration cache forbids that at execution time.
   */
  val inputFiles: FileCollection
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    get() {
      val files = objects.fileCollection()
      files.from(objects.fileTree().from(sourceDir))
      for (loadPath in loadPaths) {
        files.from(objects.fileTree().from(loadPath))
      }
      return files
    }

  fun loadPath(loadPath: File) {
    loadPaths.add(loadPath)
  }

  fun entryPoint(from: String, to: String) {
    entryPoints.add(from to to)
  }

  fun entryPoint(entryPoint: Pair<String, String>) {
    entryPoints.add(entryPoint)
  }

  fun noCharset() {
    charset = false
  }

  fun noErrorCss() {
    errorCss = false
  }

  fun watch() {
    watch = true
  }

  fun quiet() {
    quiet = true
  }

  private val fileEntryPoints: Map<File, File>
    get() {
      val source = sourceDir.toPath()
      val output = outputDir.toPath().resolve(destPath)
      if (entryPoints.isEmpty()) {
        return mapOf(
          source.toAbsolutePath().normalize().toFile() to
              output.toAbsolutePath().normalize().toFile()
        )
      }
      val result = mutableMapOf<File, File>()
      for ((from, to) in entryPoints) {
        val input = source.resolve(from).toAbsolutePath().normalize().toFile()
        val new = output.resolve(to).toAbsolutePath().normalize().toFile()
        val old = result.put(input, new)
        check(old == null || old == new) {
          "Two different outputs for input $input: $old and $new."
        }
      }
      return result
    }

  init {
    val command = if (Os.isFamily(Os.FAMILY_WINDOWS)) "sass.bat" else "sass"
    val sassExtension = generateSequence(project) { it.parent }
      .firstNotNullOfOrNull { it.extensions.findByType(SassGradlePluginExtension::class) }
    checkNotNull(sassExtension) {
      "SassGradlePluginExtension wasn't registered in any parent project."
    }
    this.executableProvider = project.provider {
      sassExtension.actualDirectory
        .toPath()
        .resolve(sassExtension.actualVersion)
        .resolve("dart-sass")
        .resolve(command)
        .toFile()
    }
  }

  @TaskAction
  fun compileSass() {
    val workQueue = workerExecutor.noIsolation()
    workQueue.submit(CompileSassWorkAction::class) {
      executable.set(this@CompileSass.executable)
      loadPaths.setFrom(this@CompileSass.loadPaths)
      entryPoints.set(this@CompileSass.fileEntryPoints)
      style.set(this@CompileSass.style)
      sourceMap.set(this@CompileSass.sourceMap)
      sourceMapUrls.set(this@CompileSass.sourceMapUrls)
      watch.set(this@CompileSass.watch)
      charset.set(this@CompileSass.charset)
      errorCss.set(this@CompileSass.errorCss)
      quiet.set(this@CompileSass.quiet)
    }
  }

}
