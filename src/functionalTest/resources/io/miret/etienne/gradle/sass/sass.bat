@for %%a in (%*) do @set LAST_ARG=%%a
@for /f "tokens=2 delims=:" %%a in ("%LAST_ARG%") do @set DIR=%%a
@md %DIR
@echo. > %DIR/style.css
