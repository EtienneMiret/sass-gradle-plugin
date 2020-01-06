package io.miret.etienne.gradle.sass;

import lombok.Getter;
import lombok.Setter;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class CompileSass extends DefaultTask {

  enum Style {
    expanded,
    compressed
  }

  enum SourceMap {
    none,
    embed,
    file
  }

  enum SourceMapUrls {
    relative,
    absolute
  }

  @Setter
  @Getter (onMethod_ = {@OutputDirectory})
  private File outputDir = new File (getProject ().getBuildDir (), "sass");

  @Setter
  private File sourceDir = new File (getProject ().getProjectDir (), "src/main/sass");

  private List<File> loadPaths = new ArrayList<> ();

  @Setter
  @Getter (onMethod_ = {@Input})
  private Style style = Style.expanded;

  @Getter (onMethod_ = {@Input})
  private boolean watch = false;

  @Getter (onMethod_ = {@Input})
  private boolean charset = true;

  @Getter (onMethod_ = {@Input})
  private boolean errorCss = true;

  @Setter
  @Getter (onMethod_ = {@Input})
  private SourceMap sourceMap = SourceMap.file;

  @Setter
  @Getter (onMethod_ = {@Input})
  private SourceMapUrls sourceMapUrls = SourceMapUrls.relative;

  @InputFiles
  public FileCollection getInputFiles () {
    return getProject ().files (
        getProject ().fileTree (sourceDir),
        loadPaths.stream ()
          .map (getProject ()::fileTree)
          .collect (toList ())
    );
  }

  public void loadPath (File loadPath) {
    loadPaths.add (loadPath);
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

  @TaskAction
  public void compileSass () {
    String command = Os.isFamily (Os.FAMILY_WINDOWS) ? "sass.bat" : "sass";
    File executable = getProject ()
        .getExtensions ()
        .findByType (SassGradlePluginExtension.class)
        .getDirectory ()
        .toPath ()
        .resolve ("dart-sass")
        .resolve (command)
        .toFile ();

    getProject ().exec (execSpec -> {
      execSpec.executable (executable);

      List<String> args = new ArrayList<> ();
      loadPaths.stream ()
          .map (File::toString)
          .map ("--load-path="::concat)
          .forEach (args::add);
      args.add (String.format ("--style=%s", style));
      if (watch) {
        args.add("--watch");
      }
      if (!charset) {
        args.add ("--no-charset");
      }
      if (!errorCss) {
        args.add ("--no-error-css");
      }
      switch (sourceMap) {
        case none:
          args.add ("--no-source-map");
          break;
        case embed:
          args.add ("--embed-source-map");
          break;
        default:
          // nothing to do.
      }
      if (sourceMap != SourceMap.none) {
        args.add (String.format ("--source-map-urls=%s", sourceMapUrls));
      }
      args.add (String.format ("%s:%s", sourceDir, outputDir));
      execSpec.args (args);
    });
  }

}
