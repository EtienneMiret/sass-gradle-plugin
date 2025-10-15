package io.miret.etienne.gradle.sass;

import org.gradle.process.ExecOperations;
import org.gradle.workers.WorkAction;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class CompileSassWorkAction implements WorkAction<CompileSassWorkParameters> {
  private final ExecOperations execOperations;

  @Inject
  public CompileSassWorkAction(ExecOperations execOperations) {
    this.execOperations = execOperations;
  }

  @Override
  public void execute() {
    CompileSassWorkParameters parameters = getParameters();

    execOperations.exec (execSpec -> {
      execSpec.executable (parameters.getExecutable().get());

      List<String> args = new ArrayList<>();
      parameters.getLoadPaths ().getFiles ().stream ()
          .map (File::toString)
          .map ("--load-path="::concat)
          .forEach (args::add);
      args.add (String.format ("--style=%s", parameters.getStyle().get()));
      if (parameters.getWatch().get()) {
        args.add("--watch");
      }
      if (!parameters.getCharset().get()) {
        args.add ("--no-charset");
      }
      if (!parameters.getErrorCss().get()) {
        args.add ("--no-error-css");
      }
      if (parameters.getQuiet().get()) {
        args.add ("--quiet");
      }
      CompileSass.SourceMap sourceMap = parameters.getSourceMap().get();
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
      if (sourceMap != CompileSass.SourceMap.none) {
        args.add (String.format ("--source-map-urls=%s", parameters.getSourceMapUrls().get()));
      }
      parameters.getEntryPoints().get().forEach((from, to) -> {
        args.add (String.format ("%s:%s", from, to));
      });
      execSpec.args (args);
    });
  }
}
