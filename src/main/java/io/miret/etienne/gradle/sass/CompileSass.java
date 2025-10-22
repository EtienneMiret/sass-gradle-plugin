package io.miret.etienne.gradle.sass;

import kotlin.Pair;
import lombok.Getter;
import lombok.Setter;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@CacheableTask
public class CompileSass extends DefaultTask {

  private final WorkerExecutor workerExecutor;

  private final File sassExecutable;

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

  @Setter
  @Getter (onMethod_ = {@OutputDirectory})
  private File outputDir = new File (getProject ().getBuildDir (), "sass");

  @Setter
  @Getter (onMethod_ = {@InputDirectory, @PathSensitive(PathSensitivity.RELATIVE)})
  private File sourceDir = new File (getProject ().getProjectDir (), "src/main/sass");

  private final List<File> loadPaths = new ArrayList<>();

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

  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  public FileCollection getInputFiles () {
    return getProject().files(
        getProject().fileTree(sourceDir),
        loadPaths.stream()
            .map(getProject()::fileTree)
            .collect(toList())
    );
  }

  @InputFile
  @PathSensitive(PathSensitivity.NONE)
  public File getExecutable () {
    return sassExecutable;
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
    loadPaths.add (loadPath);
  }

  public void entryPoint(String from, String to) {
    entryPoints.add(new Pair<>(from, to));
  }

  public void entryPoint(Pair<String, String> entryPoint) {
    entryPoints.add(entryPoint);
  }

  private Map<File, File> fileEntryPoints() {
    Path source = sourceDir.toPath();
    Path output = outputDir.toPath().resolve(destPath);
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
  public CompileSass (WorkerExecutor workerExecutor) {
    super();
    this.workerExecutor = workerExecutor;

    String command = Os.isFamily (Os.FAMILY_WINDOWS) ? "sass.bat" : "sass";
    SassGradlePluginExtension sassExtension = findExtension();
    sassExecutable = sassExtension.getDirectory ()
        .toPath ()
        .resolve (sassExtension.getVersion ())
        .resolve ("dart-sass")
        .resolve (command)
        .toFile ();
  }

  @TaskAction
  public void compileSass () {
    WorkQueue workQueue = workerExecutor.noIsolation();
    workQueue.submit(CompileSassWorkAction.class, compileSassWorkParameters -> {
      compileSassWorkParameters.getExecutable ().set (sassExecutable);
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
