package io.miret.etienne.gradle.sass;

import lombok.Getter;
import lombok.Setter;
import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Path;

@Getter
@Setter
public class SassGradlePluginExtension {

  private String version;

  private File directory;

  private String baseUrl;

  private boolean autoCopy;

  public SassGradlePluginExtension (Project project) {
    Path projectPath = project.getRootDir()
        .toPath()
        .relativize(project.getProjectDir().toPath());
    this.version = "1.54.0";
    this.directory = project.getRootDir()
        .toPath ()
        .resolve (".gradle/sass")
        .resolve(projectPath)
        .toFile ();
    this.baseUrl = "https://github.com/sass/dart-sass/releases/download";
    this.autoCopy = true;
  }

  public void noAutoCopy () {
    this.autoCopy = false;
  }

}
