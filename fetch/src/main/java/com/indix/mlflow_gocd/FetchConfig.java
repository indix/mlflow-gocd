package com.indix.mlflow_gocd;


import java.util.Map;

public class FetchConfig {

    private final String repo;
    private final String pkg;
    private final String artifactPattern;
    private final String destination;
    private final String destinationFile;

    public String getRepo() {
        return escapeEnvironmentVariable(repo);
    }

    public String getPkg() {
        return escapeEnvironmentVariable(pkg);
    }

    public String getArtifactPattern() {
        return artifactPattern;
    }

    public String getDestination() {
        return destination;
    }

    public String getDestinationFile() {
        return destinationFile;
    }

    public FetchConfig(Map config) {
        repo = getValue(config, MLFLowFetchArtifactTask.REPO);
        pkg = getValue(config, MLFLowFetchArtifactTask.PACKAGE);
        artifactPattern = getValue(config, MLFLowFetchArtifactTask.ARTIFACT_PATTERN);
        destination = getValue(config, MLFLowFetchArtifactTask.DESTINATION);
        destinationFile = getValue(config, MLFLowFetchArtifactTask.DESTINATION_FILE);
    }

    private String escapeEnvironmentVariable(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^A-Za-z0-9_]", "_").toUpperCase();
    }

    private String getValue(Map config, String property) {
        Map propertyMap = ((Map) config.get(property));
        if (propertyMap != null) {
            return (String) propertyMap.get("value");
        }
        return null;
    }
}