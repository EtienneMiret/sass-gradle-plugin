package io.miret.etienne.gradle.sass

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.submit
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class CompileSass
@Inject constructor(
  private val workerExecutor: WorkerExecutor,
  private val objects: ObjectFactory,
) : DefaultTask() {

  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  val executable: Provider<RegularFile>

  /**
   * Set the [style](https://sass-lang.com/documentation/cli/dart-sass/#style)
   * of the generated CSS. Either [expanded] (default) or [compressed].
   */
  @get:Input
  abstract val style: Property<Style>

  /**
   * Define where to output the source map.
   * Either [file] (default), [none] or [embedded].
   */
  @get:Input
  abstract val sourceMapLocation: Property<SourceMapLocation>

  /**
   * Define how to output source map URLs.
   * Either [relative] (default) or [absolute].
   */
  @get:Input
  abstract val sourceMapUrls: Property<SourceMapUrls>

  /**
   * Set whether to watch the source directory for changes.
   * See [--watch](https://sass-lang.com/documentation/cli/dart-sass/#watch).
   * Defaults to `false`.
   */
  @get:Input
  abstract val watch: Property<Boolean>

  /**
   * Set whether to insert a byte-order mark or `@charset` in the generated CSS.
   * See [--no-charset](https://sass-lang.com/documentation/cli/dart-sass/#no-charset).
   * Defaults to `true`.
   */
  @get:Input
  abstract val charset: Property<Boolean>

  enum class Style {
    expanded,
    compressed,
  }

  /**
   * Writes each selector and declaration on its own line.
   */
  val expanded: Style
    @Internal get() = Style.expanded

  /**
   * Removes as many extra characters as possible,
   * and writes the entire stylesheet on a single line.
   */
  val compressed: Style
    @Internal get() = Style.compressed

  enum class SourceMapLocation {
    none,
    embedded,
    file,
  }

  /**
   * Do not output any source map.
   * See [--no-source-map](https://sass-lang.com/documentation/cli/dart-sass/#no-source-map).
   */
  val none: SourceMapLocation
    @Internal get() = SourceMapLocation.none

  /**
   * Embed the source map in the CSS file.
   * See [--embed-source-map](https://sass-lang.com/documentation/cli/dart-sass/#embed-source-map).
   */
  val embedded: SourceMapLocation
    @Internal get() = SourceMapLocation.embedded

  /**
   * Write the source map in a separate file and link it from the CSS.
   */
  val file: SourceMapLocation
    @Internal get() = SourceMapLocation.file

  enum class SourceMapUrls {
    relative,
    absolute,
  }

  val relative: SourceMapUrls
    @Internal get() = SourceMapUrls.relative

  val absolute: SourceMapUrls
    @Internal get() = SourceMapUrls.absolute

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
  var errorCss = true
    private set

  @get:Input
  var quiet = false
    private set

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

  /**
   * Keep running after the initial build and watch for changes.
   */
  fun watch() {
    watch.set(true)
  }

  /**
   * Do not emit a byte-order mark nor a `@charset` declaration,
   * even if the CSS file contains non-ASCII characters.
   * @see charset
   */
  fun noCharset() {
    charset.set(false)
  }

  fun noErrorCss() {
    errorCss = false
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
    executable = objects.fileProperty().fileProvider(project.provider {
      sassExtension.actualDirectory
        .toPath()
        .resolve(sassExtension.actualVersion)
        .resolve("dart-sass")
        .resolve(command)
        .toFile()
    })
    style.convention(Style.expanded)
    sourceMapLocation.convention(SourceMapLocation.file)
    sourceMapUrls.convention(SourceMapUrls.relative)
    watch.convention(false)
    charset.convention(true)
  }

  @TaskAction
  fun compileSass() {
    val workQueue = workerExecutor.noIsolation()
    workQueue.submit(CompileSassWorkAction::class) {
      executable.set(this@CompileSass.executable)
      loadPaths.setFrom(this@CompileSass.loadPaths)
      entryPoints.set(this@CompileSass.fileEntryPoints)
      style.set(this@CompileSass.style)
      sourceMapLocation.set(this@CompileSass.sourceMapLocation)
      sourceMapUrls.set(this@CompileSass.sourceMapUrls)
      watch.set(this@CompileSass.watch)
      charset.set(this@CompileSass.charset)
      errorCss.set(this@CompileSass.errorCss)
      quiet.set(this@CompileSass.quiet)
    }
  }

}
