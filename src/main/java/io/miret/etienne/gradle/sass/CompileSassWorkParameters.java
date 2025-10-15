package io.miret.etienne.gradle.sass;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkParameters;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public interface CompileSassWorkParameters extends WorkParameters {
  RegularFileProperty getExecutable();
  ConfigurableFileCollection getLoadPaths();

  MapProperty<File, File> getEntryPoints();

  Property<CompileSass.Style> getStyle();
  Property<CompileSass.SourceMap> getSourceMap();
  Property<CompileSass.SourceMapUrls> getSourceMapUrls();

  Property<Boolean> getWatch();
  Property<Boolean> getCharset();
  Property<Boolean> getErrorCss();
  Property<Boolean> getQuiet();
}
