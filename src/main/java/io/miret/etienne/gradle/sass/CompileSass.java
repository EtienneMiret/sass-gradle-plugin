package io.miret.etienne.gradle.sass;

import lombok.Getter;
import lombok.Setter;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;

public class CompileSass extends DefaultTask {

  @Setter
  @Getter (onMethod_ = {@OutputDirectory})
  private File outputDir = new File (getProject ().getBuildDir (), "sass");

  @Setter
  @Getter (onMethod_ = {@InputDirectory})
  private File sourceDir = new File (getProject ().getProjectDir (), "src/main/sass");

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
      execSpec.args (
          String.format ("%s:%s", sourceDir, outputDir)
      );
    });
  }

}
