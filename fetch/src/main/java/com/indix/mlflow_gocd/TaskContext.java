package com.indix.mlflow_gocd;

import com.thoughtworks.go.plugin.api.task.JobConsoleLogger;

import java.nio.file.Paths;
import java.util.Map;

public class TaskContext {
    private final Map environmentVariables;
    private final String workingDir;
    private final JobConsoleLogger console;

    public TaskContext(Map context) {
        environmentVariables = (Map) context.get("environmentVariables");
        workingDir = (String) context.get("workingDirectory");
        console = new JobConsoleLogger() {};
    }

    public void printMessage(String message) {
        console.printLine(message);
    }

    public void printEnvironment() {
        console.printEnvironment(environmentVariables);
    }

    public Map getEnvironmentVariables() {
        return environmentVariables;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public String getAbsoluteWorkingDir() {
        return Paths.get("").toAbsolutePath().resolve(workingDir).toString();
    }
}
