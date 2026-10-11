package io.miret.etienne.gradle.sass

import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import javax.inject.Inject

abstract class CompileSassWorkAction
@Inject constructor(private val execOperations: ExecOperations) :
  WorkAction<CompileSassWorkParameters> {

  override fun execute() {
    execOperations.exec {
      executable(parameters.executable.get())
      val arguments = mutableListOf<String>()
      parameters.loadPaths.files
        .mapTo(arguments) { "--load-path=$it" }
      arguments.add("--style=${parameters.style.get()}")
      if (parameters.watch.get()) {
        arguments.add("--watch")
      }
      if (!parameters.charset.get()) {
        arguments.add("--no-charset")
      }
      if (!parameters.errorCss.get()) {
        arguments.add("--no-error-css")
      }
      if (parameters.quiet.get()) {
        arguments.add("--quiet")
      }
      val sourceMap = parameters.sourceMap.get()
      when (sourceMap) {
        CompileSass.SourceMap.none -> arguments.add("--no-source-map")
        CompileSass.SourceMap.embed -> arguments.add("--embed-source-map")
        CompileSass.SourceMap.file -> {}
      }
      if (sourceMap != CompileSass.SourceMap.none) {
        arguments.add("--source-map-urls=${parameters.sourceMapUrls.get()}")
      }
      parameters.entryPoints.get().forEach { (from, to) ->
        arguments.add("$from:$to")
      }
      args(arguments)
    }
  }

}
