package io.miret.etienne.gradle.sass;

import kotlin.Pair;
import lombok.Getter;
import lombok.Setter;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@CacheableTask
public class CompileSass extends DefaultTask {

  private final WorkerExecutor workerExecutor;

  private final ObjectFactory objects;

  private final Provider<RegularFile> executable;

  @Getter (onMethod_ = @Input)
  private final List<Pair<String, String>> entryPoints = new ArrayList<>();

  public enum Style {
    expanded,
    compressed
  }

  public enum SourceMap {
    none,
    embed,
    file
  }

  public enum SourceMapUrls {
    relative,
    absolute
  }

  /**
   * Directory where to output generated CSS. Lazy counterpart of {@link #getOutputDir()}, which reads and writes
   * through it.
   */
  @Getter (onMethod_ = {@OutputDirectory})
  private final DirectoryProperty outputDirectory;

  /**
   * Source directory containing sass to compile. Lazy counterpart of {@link #getSourceDir()}, which reads and
   * writes through it.
   */
  @Getter (onMethod_ = {@InputDirectory, @PathSensitive(PathSensitivity.RELATIVE)})
  private final DirectoryProperty sourceDirectory;

  /** Directories added to the sass load path. Fingerprinted through {@link #getInputFiles()}. */
  @Getter (onMethod_ = {@Internal})
  private final ConfigurableFileCollection loadPaths;

  @Setter
  @Getter (onMethod_ = {@Input})
  private String destPath = ".";

  @Setter
  @Getter (onMethod_ = {@Input})
  private Style style = Style.expanded;

  @Getter (onMethod_ = {@Input})
  private boolean watch = false;

  @Getter (onMethod_ = {@Input})
  private boolean charset = true;

  @Getter (onMethod_ = {@Input})
  private boolean errorCss = true;

  @Getter (onMethod_ = {@Input})
  private boolean quiet = false;

  @Setter
  @Getter (onMethod_ = {@Input})
  private SourceMap sourceMap = SourceMap.file;

  @Setter
  @Getter (onMethod_ = {@Input})
  private SourceMapUrls sourceMapUrls = SourceMapUrls.relative;

  @Internal
  public File getOutputDir () {
    return outputDirectory.get ().getAsFile ();
  }

  public void setOutputDir (File outputDir) {
    outputDirectory.set (outputDir);
  }

  @Internal
  public File getSourceDir () {
    return sourceDirectory.get ().getAsFile ();
  }

  public void setSourceDir (File sourceDir) {
    sourceDirectory.set (sourceDir);
  }

  /**
   * Gradle calls this getter while the task executes (to fingerprint the inputs), so it must not touch
   * {@link #getProject()}: the configuration cache forbids that at execution time.
   */
  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  public FileCollection getInputFiles () {
    return objects.fileCollection ()
        .from (sourceDirectory.getAsFileTree (), loadPaths.getAsFileTree ());
  }

  @InputFile
  @PathSensitive(PathSensitivity.NONE)
  public File getExecutable () {
    return executable.get ().getAsFile ();
  }

  private SassGradlePluginExtension findExtension() {
    Project project = getProject();
    SassGradlePluginExtension extension = null;
    while (extension == null && project != null) {
      extension = project.getExtensions()
          .findByType(SassGradlePluginExtension.class);
      project = project.getParent();
    }
    if (extension == null) {
      throw new IllegalStateException(
          "SassGradlePluginExtension wasn't registered in any parent project."
      );
    }
    return extension;
  }

  public void loadPath (File loadPath) {
    loadPaths.from (loadPath);
  }

  /**
   * Adds a load path given as anything Gradle can resolve to a directory: a {@code File}, a {@code Directory},
   * a {@code Provider} of either, or a path string.
   */
  public void loadPath (Object loadPath) {
    loadPaths.from (loadPath);
  }

  public void entryPoint(String from, String to) {
    entryPoints.add(new Pair<>(from, to));
  }

  public void entryPoint(Pair<String, String> entryPoint) {
    entryPoints.add(entryPoint);
  }

  private Map<File, File> fileEntryPoints() {
    Path source = getSourceDir ().toPath();
    Path output = getOutputDir ().toPath().resolve(destPath);
    if (entryPoints.isEmpty()) {
      return Collections.singletonMap(
          source.toAbsolutePath().normalize().toFile(),
          output.toAbsolutePath().normalize().toFile()
      );
    }
    return entryPoints.stream()
        .map(pair -> {
          Path from = source.resolve(pair.component1());
          Path to = output.resolve(pair.component2());
          return new Pair<>(
              from.toAbsolutePath().normalize().toFile(),
              to.toAbsolutePath().normalize().toFile());
        })
        .collect(toMap(Pair::component1, Pair::component2, (a, b) -> {
          if (a.equals(b)) {
            return a;
          }
          throw new IllegalStateException("Two different outputs for the same input: " + a + " and " + b + ".");
        }));
  }

  public void noCharset () {
    charset = false;
  }

  public void noErrorCss () {
    errorCss = false;
  }

  public void watch () {
    watch = true;
  }

  public void quiet () {
    quiet = true;
  }

  @Internal
  public Style getExpanded () {
    return Style.expanded;
  }

  @Internal
  public Style getCompressed () {
    return Style.compressed;
  }

  @Internal
  public SourceMap getNone () {
    return SourceMap.none;
  }

  @Internal
  public SourceMap getEmbed () {
    return SourceMap.embed;
  }

  @Internal
  public SourceMap getFile () {
    return SourceMap.file;
  }

  @Internal
  public SourceMapUrls getRelative () {
    return SourceMapUrls.relative;
  }

  @Internal
  public SourceMapUrls getAbsolute () {
    return SourceMapUrls.absolute;
  }

  @Inject
  public CompileSass (WorkerExecutor workerExecutor, ObjectFactory objects, ProjectLayout layout) {
    super();
    this.workerExecutor = workerExecutor;
    this.objects = objects;
    this.outputDirectory = objects.directoryProperty ()
        .convention (layout.getBuildDirectory ().dir ("sass"));
    this.sourceDirectory = objects.directoryProperty ()
        .convention (layout.getProjectDirectory ().dir ("src/main/sass"));
    this.loadPaths = objects.fileCollection ();

    String command = Os.isFamily (Os.FAMILY_WINDOWS) ? "sass.bat" : "sass";
    SassGradlePluginExtension sassExtension = findExtension();
    Provider<String> version = getProject ().provider (sassExtension::getVersion);
    executable = sassExtension.getInstallDirectory ()
        .dir (version)
        .map (installed -> installed.dir ("dart-sass").file (command));
  }

  @TaskAction
  public void compileSass () {
    WorkQueue workQueue = workerExecutor.noIsolation();
    workQueue.submit(CompileSassWorkAction.class, compileSassWorkParameters -> {
      compileSassWorkParameters.getExecutable ().set (getExecutable ());
      compileSassWorkParameters.getLoadPaths ().setFrom (loadPaths);
      compileSassWorkParameters.getEntryPoints().set(fileEntryPoints());
      compileSassWorkParameters.getStyle ().set (style);
      compileSassWorkParameters.getSourceMap ().set (sourceMap);
      compileSassWorkParameters.getSourceMapUrls ().set (sourceMapUrls);
      compileSassWorkParameters.getWatch ().set (watch);
      compileSassWorkParameters.getCharset ().set (charset);
      compileSassWorkParameters.getErrorCss ().set (errorCss);
      compileSassWorkParameters.getQuiet ().set (quiet);
    });
  }

}
