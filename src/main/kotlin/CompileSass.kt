package io.miret.etienne.gradle.sass

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
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
abstract class CompileSass
@Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  val executable: Provider<RegularFile>

  /**
   * List of paths where to load SASS modules from.
   * See [--load-path](https://sass-lang.com/documentation/cli/dart-sass/#load-path).
   */
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val loadPaths: ConfigurableFileCollection

  /**
   * Directory containing the SASS files to compile.
   * Defaults to `src/main/sass`.
   */
  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val sourceDir: DirectoryProperty

  /**
   * Sub path where to write generated CSS, e.g., relative to war root
   */
  @get:Input
  abstract val destPath: Property<String>

  /**
   * List of input files and their corresponding output files.
   */
  @get:Input
  @get:Optional
  abstract val entryPoints: ListProperty<Pair<String, String>>

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

  /**
   * Set whether to generate a CSS file describing the error
   * in case of compilation failure.
   * See [--error-css](https://sass-lang.com/documentation/cli/dart-sass/#error-css).
   * Defaults to `true`.
   */
  @get:Input
  abstract val errorCss: Property<Boolean>

  /**
   * Set whether to output warnings to the console.
   * See [--quiet](https://sass-lang.com/documentation/cli/dart-sass/#quiet).
   * Defaults to `false`.
   */
  @get:Input
  abstract val quiet: Property<Boolean>

  /**
   * Directory where to output generated CSS,
   * before copy to final destination (war, jar…).
   */
  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  /**
   * @deprecated Use [sourceMapLocation] instead.
   */
  @get:Internal
  @Deprecated(
    message = "Use sourceMapLocation instead.",
    replaceWith = ReplaceWith("sourceMapLocation"),
  )
  val sourceMap : Property<SourceMapLocation>
    get() = sourceMapLocation

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

  /**
   * @deprecated Use [embedded] instead.
   */
  @Deprecated(
    message = "Use embedded instead.",
    replaceWith = ReplaceWith("embedded"),
  )
  val embed: SourceMapLocation
    @Internal get() = SourceMapLocation.embedded

  enum class SourceMapUrls {
    relative,
    absolute,
  }

  val relative: SourceMapUrls
    @Internal get() = SourceMapUrls.relative

  val absolute: SourceMapUrls
    @Internal get() = SourceMapUrls.absolute

  /**
   * Add a [load path](https://sass-lang.com/documentation/at-rules/use/#load-paths).
   * @see loadPaths
   */
  fun loadPath(loadPath: File) {
    loadPaths.from(loadPath)
  }

  /**
   * Add an input and output file pair.
   * @param from The input file path, relative to [sourceDir].
   * @param to The output file path, relative to [destPath].
   */
  fun entryPoint(from: String, to: String) {
    entryPoints.add(from to to)
  }

  /**
   * Add an input and output file pair.
   */
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

  /**
   * Do not emit a CSS file if there is any compilation error.
   * @see errorCss
   */
  fun noErrorCss() {
    errorCss.set(false)
  }

  /**
   * Do not emit any warnings while compiling.
   */
  fun quiet() {
    quiet.set(true)
  }

  /**
   * A view of [entryPoints], using absolute paths.
   */
  private val fileEntryPoints: Map<File, File>
    get() {
      val result = mutableMapOf<File, File>()
      val fromDir = sourceDir.get()
      val toDir = outputDir.dir(destPath).get()
      for ((from, to) in entryPoints.orElse(listOf("." to ".")).get()) {
        val input = fromDir.file(from).asFile.absoluteFile.normalize()
        val new = toDir.file(to).asFile.absoluteFile.normalize()
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
    executable = sassExtension.directory
      .dir(sassExtension.version)
      .map { it.file("dart-sass/$command") }
    sourceDir.convention(project.layout.projectDirectory.dir("src/main/sass"))
    destPath.convention(".")
    entryPoints.convention(null)
    style.convention(Style.expanded)
    sourceMapLocation.convention(SourceMapLocation.file)
    sourceMapUrls.convention(SourceMapUrls.relative)
    watch.convention(false)
    charset.convention(true)
    errorCss.convention(true)
    quiet.convention(false)
    outputDir.convention(project.layout.buildDirectory.dir("sass"))
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
