package io.miret.etienne.gradle.sass;

import lombok.Getter;
import lombok.Setter;
import org.gradle.api.Project;

import java.io.File;

@Getter
@Setter
public class SassGradlePluginExtension {

  private String version;

  private File directory;

  private String baseUrl;

  private boolean autoCopy;

  public SassGradlePluginExtension (Project project) {
    this.version = "1.54.0";
    this.directory = project.getProjectDir ()
        .toPath ()
        .resolve (".gradle/sass")
        .toFile ();
    this.baseUrl = "https://github.com/sass/dart-sass/releases/download";
    this.autoCopy = true;
  }

  public void noAutoCopy () {
    this.autoCopy = false;
  }

}
