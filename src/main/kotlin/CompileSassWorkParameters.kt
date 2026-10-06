package io.miret.etienne.gradle.sass

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkParameters
import java.io.File

interface CompileSassWorkParameters : WorkParameters {

  val executable: RegularFileProperty
  val loadPaths: ConfigurableFileCollection

  val entryPoints: MapProperty<File, File>

  val style: Property<CompileSass.Style>
  val sourceMap: Property<CompileSass.SourceMap>
  val sourceMapUrls: Property<CompileSass.SourceMapUrls>

  val watch: Property<Boolean>
  val charset: Property<Boolean>
  val errorCss: Property<Boolean>
  val quiet: Property<Boolean>

}
