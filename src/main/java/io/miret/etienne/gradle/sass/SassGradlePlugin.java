package io.miret.etienne.gradle.sass;

import de.undercouch.gradle.tasks.download.Download;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.War;

import java.io.File;

public class SassGradlePlugin implements Plugin<Project> {

  public void apply (Project project) {
    SassGradlePluginExtension extension = project.getExtensions ()
        .create ("sass", SassGradlePluginExtension.class, project);

    TaskProvider<Download> downloadSass = project.getTasks ()
        .register ("downloadSass", Download.class, task -> {
          String archiveName = archiveName (extension.getVersion ());
          File archive = extension.getDirectory ()
              .toPath ()
              .resolve ("archive")
              .resolve (archiveName)
              .toFile ();
          task.setDescription ("Download a sass archive.");
          task.src (String.format ("%s/%s/%s", extension.getBaseUrl (), extension.getVersion (), archiveName));
          task.dest (archive);
          task.tempAndMove (true);
          task.overwrite (false);
        });
    TaskProvider<Copy> installSass = project.getTasks ()
        .register ("installSass", Copy.class, task -> {
          Provider<FileTree> downloadedFiles = downloadSass.map (Download::getDest)
              .map (archive -> Os.isFamily (Os.FAMILY_WINDOWS)
                  ? project.zipTree (archive)
                  : project.tarTree (archive));
          task.setDescription ("Unpack and install a sass archive.");
          task.dependsOn (downloadSass);
          task.from (downloadedFiles);
          task.into (new File (extension.getDirectory (), extension.getVersion ()));
        });
    compileSass(project, extension, installSass);
  }

  private void compileSass(
      Project project,
      SassGradlePluginExtension extension,
      TaskProvider<?> installSass
  ) {
    TaskProvider<CompileSass> compileSass = project.getTasks ()
        .register ("compileSass", CompileSass.class, task -> {
          task.setDescription ("Compile sass and scss.");
          task.dependsOn (installSass);
        });
    project.getTasks ()
        .withType (War.class)
        .configureEach (task -> {
          if (extension.isAutoCopy ()) {
            task.dependsOn (compileSass);
            task.from (compileSass.map (CompileSass::getOutputDir));
          }
        });
    project.getSubprojects().forEach(
        subProject -> compileSass(subProject, extension, installSass)
    );
  }

  private String archiveName (String version) {
    return String.format ("dart-sass-%s-%s-%s.%s",
        version, buildOperatingSystem (), buildArchitecture (), archiveExtension ());
  }

  private String buildOperatingSystem () {
    if (Os.isFamily (Os.FAMILY_WINDOWS)) {
      return "windows";
    } else if (Os.isFamily (Os.FAMILY_MAC)) {
      return "macos";
    } else {
      return "linux";
    }
  }

  private String buildArchitecture () {
    String arch = System.getProperty("os.arch");
    if (arch.contains("arm") || arch.contains("aarch64")) {
      return "arm64";
    } else if (arch.contains("64")) {
      return "x64";
    } else {
      return "ia32";
    }
  }

  private String archiveExtension () {
    return Os.isFamily (Os.FAMILY_WINDOWS) ? "zip" : "tar.gz";
  }

}
