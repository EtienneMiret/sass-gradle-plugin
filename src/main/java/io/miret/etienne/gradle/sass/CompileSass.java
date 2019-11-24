package io.miret.etienne.gradle.sass;

import lombok.Getter;
import lombok.Setter;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CompileSass extends DefaultTask {

  @Setter
  @Getter (onMethod_ = {@OutputDirectory})
  private File outputDir = new File (getProject ().getBuildDir (), "sass");

  @Setter
  @Getter (onMethod_ = {@InputDirectory})
  private File sourceDir = new File (getProject ().getProjectDir (), "src/main/sass");

  private List<File> loadPaths = new ArrayList<> ();

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
