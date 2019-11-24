package io.miret.etienne.gradle.sass;

import lombok.Getter;
import lombok.Setter;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class CompileSass extends DefaultTask {

  @Setter
  @Getter (onMethod_ = {@OutputDirectory})
  private File outputDir = new File (getProject ().getBuildDir (), "sass");

  @Setter
  private File sourceDir = new File (getProject ().getProjectDir (), "src/main/sass");

  private List<File> loadPaths = new ArrayList<> ();

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
      args.add (String.format ("%s:%s", sourceDir, outputDir));
      execSpec.args (args);
    });
  }

}
