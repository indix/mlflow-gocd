package com.indix.mlflow_gocd;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.gson.GsonBuilder;
import com.indix.mlflow_gocd.models.TaskExecutionResult;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.indix.mlflow_gocd.GoEnvironment.AWS_ACCESS_KEY_ID;
import static com.indix.mlflow_gocd.GoEnvironment.AWS_REGION;
import static com.indix.mlflow_gocd.GoEnvironment.AWS_SECRET_ACCESS_KEY;

@Extension
public class MLFLowFetchArtifactTask implements GoPlugin {

    public static final String REPO = "Repo";
    public static final String PACKAGE = "Package";
    public static final String ARTIFACT_PATTERN = "ArtifactPattern";
    public static final String DESTINATION = "Destination";
    public static final String DESTINATION_FILE = "DestinationFile";

    private static Logger logger = Logger.getLoggerFor(MLFLowFetchArtifactTask.class);

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {

    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest request) throws UnhandledRequestTypeException {
        if ("configuration".equals(request.requestName())) {
            return handleGetConfigRequest();
        } else if ("validate".equals(request.requestName())) {
            return handleValidation(request);
        } else if ("execute".equals(request.requestName())) {
            return handleTaskExecution(request);
        } else if ("view".equals(request.requestName())) {
            return handleTaskView();
        }
        throw new UnhandledRequestTypeException(request.requestName());
    }

    private GoPluginApiResponse handleTaskView() {
        int responseCode = DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE;
        Map view = new HashMap();
        view.put("displayValue", "Fetch artifacts from MLFlow run");
        try {
            view.put("template", IOUtils.toString(getClass().getResourceAsStream("/views/task.template.html"), "UTF-8"));
        } catch (Exception e) {
            responseCode = DefaultGoPluginApiResponse.INTERNAL_ERROR;
            String errorMessage = "Failed to find template: " + e.getMessage();
            view.put("exception", errorMessage);
            logger.error(errorMessage, e);
        }
        return createResponse(responseCode, view);
    }

    private GoPluginApiResponse handleTaskExecution(GoPluginApiRequest request) {
        TaskExecutionResult result;
        try {
            Map executionRequest = (Map) new GsonBuilder().create().fromJson(request.requestBody(), Object.class);
            Map configMap = (Map) executionRequest.get("config");
            TaskContext context = new TaskContext((Map) executionRequest.get("context"));
            FetchConfig config = new FetchConfig(configMap);
            final GoEnvironment env = new GoEnvironment(context.getEnvironmentVariables());
            final AmazonS3 client = getS3client(env);
            final String artifactsUri = env.get(String.format("GO_PACKAGE_%s_%s_ARTIFACT_URI", config.getRepo(), config.getPkg()));
            context.printMessage(String.format("Artifacts uri for %s - %s is %s", config.getRepo(), config.getPkg(), artifactsUri));
            final String bucketName = artifactsUri.split("//|/")[1];
            final String prefix = artifactsUri.replace(String.format("s3://%s/", bucketName), "");
            context.printMessage(String.format("Looking for artifact with pattern %s in prefix %s", config.getArtifactPattern(), prefix));
            final String s3Prefix = getPrefixS3(client, bucketName, prefix, config.getArtifactPattern());
            if (s3Prefix != null) {
                String destination = String.format("%s/%s", context.getWorkingDir(), config.getDestination());
                if (StringUtils.isNotBlank(config.getDestination())) {
                    setupDestinationDirectory(destination);
                }
                if (StringUtils.isNotBlank(config.getDestinationFile())) {
                    destination = String.format("%s/%s", destination.replaceFirst("/$", ""), config.getDestinationFile());
                }
                context.printMessage(String.format("Getting artifacts from s3://%s/%s to %s", bucketName, s3Prefix, destination));
                getS3(client, bucketName, s3Prefix, destination);
                result = new TaskExecutionResult(true, "Fetched all artifacts");
            } else {
                result = new TaskExecutionResult(false, "Specified artifacts not found");
            }


        } catch(Exception ex) {
            String message = String.format("Failure while downloading artifacts - %s", ex.getMessage());
            logger.error(message, ex);
            result = new TaskExecutionResult(false, message, ex);
        }

        return createResponse(result.responseCode(), result.toMap());

    }

    private GoPluginApiResponse handleValidation(GoPluginApiRequest request) {
        Map configMap = (Map) new GsonBuilder().create().fromJson(request.requestBody(), Object.class);
        FetchConfig config = new FetchConfig(configMap);

        Map<String, String> errors = new HashMap<>();
        if (StringUtils.isBlank(config.getRepo())) {
            errors.put(REPO, "This field is required");
        }
        if (StringUtils.isBlank(config.getPkg())) {
            errors.put(PACKAGE, "This field is required");
        }

        if (StringUtils.isBlank(config.getArtifactPattern())) {
            errors.put(ARTIFACT_PATTERN, "This field is required");
        }
        final HashMap validationResult = new HashMap();
        if (!errors.isEmpty()) {
            validationResult.put("errors", errors);
        }

        return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, validationResult);
    }

    private GoPluginApiResponse handleGetConfigRequest() {
        HashMap config = new HashMap();

        HashMap repo = new HashMap();
        repo.put("default-value", "");
        repo.put("required", true);
        config.put(REPO, repo);

        HashMap pkg = new HashMap();
        pkg.put("default-value", "");
        pkg.put("required", true);
        config.put(PACKAGE, pkg);

        HashMap sourcePrefix = new HashMap();
        sourcePrefix.put("default-value", "");
        sourcePrefix.put("required", true);
        config.put(ARTIFACT_PATTERN, sourcePrefix);

        HashMap destination = new HashMap();
        destination.put("default-value", "");
        destination.put("required", false);
        config.put(DESTINATION, destination);

        HashMap destinationFile = new HashMap();
        destinationFile.put("default-value", "");
        destinationFile.put("required", false);
        config.put(DESTINATION_FILE, destinationFile);

        return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, config);
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier("task", Arrays.asList("1.0"));
    }

    private GoPluginApiResponse createResponse(int responseCode, Map body) {
        final DefaultGoPluginApiResponse response = new DefaultGoPluginApiResponse(responseCode);
        response.setResponseBody(new GsonBuilder().serializeNulls().create().toJson(body));
        return response;
    }

    private void setupDestinationDirectory(String destination) {
        File destinationDirectory = new File(destination);
        try {
            if(!destinationDirectory.exists()) {
                FileUtils.forceMkdir(destinationDirectory);
            }
        } catch (IOException ioe) {
            logger.error(String.format("Error while setting up destination - %s", ioe.getMessage()), ioe);
        }
    }

    private static AmazonS3 getS3client(GoEnvironment env) {
        AmazonS3ClientBuilder amazonS3ClientBuilder = AmazonS3ClientBuilder.standard();

        if (env.has(AWS_REGION)) {
            amazonS3ClientBuilder.withRegion(env.get(AWS_REGION));
        }
        if (env.hasAWSUseIamRole()) {
            amazonS3ClientBuilder.withCredentials(new InstanceProfileCredentialsProvider(false));
        } else if (env.has(AWS_ACCESS_KEY_ID) && env.has(AWS_SECRET_ACCESS_KEY)) {
            BasicAWSCredentials basicCreds = new BasicAWSCredentials(env.get(AWS_ACCESS_KEY_ID), env.get(AWS_SECRET_ACCESS_KEY));
            amazonS3ClientBuilder.withCredentials(new AWSStaticCredentialsProvider(basicCreds));
        }

        return amazonS3ClientBuilder.build();
    }

    public String getPrefixS3(AmazonS3 client, String bucket, String prefix, String artifactPattern) {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(bucket)
                .withPrefix(prefix);

        ObjectListing objectListing;
        do {
            objectListing = client.listObjects(listObjectsRequest);
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                if (objectSummary.getSize() > 0 && objectSummary.getKey().matches(String.format("%s/%s", prefix, artifactPattern))) {
                    return objectSummary.getKey();
                }
            }
            listObjectsRequest.setMarker(objectListing.getNextMarker());
        } while (objectListing.isTruncated());

        return null;
    }

    public void getS3(AmazonS3 client, String bucket, String from, String to) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, from);
        File destinationFile = new File(to);
        destinationFile.getParentFile().mkdirs();
        client.getObject(getObjectRequest, destinationFile);
    }

}
