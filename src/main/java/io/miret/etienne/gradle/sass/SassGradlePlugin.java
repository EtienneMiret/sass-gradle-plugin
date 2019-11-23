package io.miret.etienne.gradle.sass;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * A simple 'hello world' plugin.
 */
public class SassGradlePlugin implements Plugin<Project> {

  public void apply (Project project) {
    // Register a task
    project.getTasks ().register ("greeting", task -> {
      task.doLast (s -> System.out.println ("Hello from plugin 'io.miret.etienne.gradle.sass.greeting'"));
    });
  }

}
