package com.indix.mlflow_gocd;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Strings;
import com.google.gson.GsonBuilder;
import com.indix.mlflow_gocd.mlfow.Run;
import com.indix.mlflow_gocd.mlfow.SearchResponse;
import com.indix.mlflow_gocd.models.MaterialResult;
import com.indix.mlflow_gocd.models.RevisionStatus;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;

@Extension
public class MLFlowPackagePlugin implements GoPlugin {
    public static String MLFLOW_URL = "MLFLOW_URL";
    public static String PROMOTION_TAG_NAME = "PROMOTION_TAG_NAME";
    public static String PROMOTION_TAG_VALUE = "PROMOTION_TAG_VALUE";
    public static String EXPERIMENT_ID = "EXPERIMENT_ID";
    public static String PR_ID_TAG_NAME = "PR_ID_TAG_NAME";
    public static String PR_RUNS_WITHIN_DAYS = "PR_RUNS_WITHIN_DAYS";

    public static final String REQUEST_REPOSITORY_CONFIGURATION = "repository-configuration";
    public static final String REQUEST_PACKAGE_CONFIGURATION = "package-configuration";
    public static final String REQUEST_VALIDATE_REPOSITORY_CONFIGURATION = "validate-repository-configuration";
    public static final String REQUEST_VALIDATE_PACKAGE_CONFIGURATION = "validate-package-configuration";
    public static final String REQUEST_CHECK_REPOSITORY_CONNECTION = "check-repository-connection";
    public static final String REQUEST_CHECK_PACKAGE_CONNECTION = "check-package-connection";
    public static final String REQUEST_LATEST_REVISION = "latest-revision";
    public static final String REQUEST_LATEST_REVISION_SINCE = "latest-revision-since";
    public static final String REQUEST_PREVIOUS_REVISION= "previous-revision";

    private static final String MLFLOW_GET_EXPERIMENT_ENDPOINT="/api/2.0/preview/mlflow/experiments/get";
    private static final String MLFLOW_SEARCH_RUNS_ENDPOINT="/api/2.0/preview/mlflow/runs/search";

    private static Logger logger = Logger.getLoggerFor(MLFlowPackagePlugin.class);

    private final HttpRequestFactory requestFactory;
    private final JsonFactory jsonFactory;

    public MLFlowPackagePlugin() {
        NetHttpTransport.Builder builder = new NetHttpTransport.Builder();
        this.jsonFactory = new GsonFactory();
        this.requestFactory = builder.build().createRequestFactory();

    }

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {

    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest goPluginApiRequest) throws UnhandledRequestTypeException {
        if (goPluginApiRequest.requestName().equals(REQUEST_REPOSITORY_CONFIGURATION)) {
            return handleRepositoryConfiguration();
        } else if (goPluginApiRequest.requestName().equals(REQUEST_PACKAGE_CONFIGURATION)) {
            return handlePackageConfiguration();
        } else if (goPluginApiRequest.requestName().equals(REQUEST_VALIDATE_REPOSITORY_CONFIGURATION)) {
            return handleRepositoryValidation(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_VALIDATE_PACKAGE_CONFIGURATION)) {
            return handlePackageValidation(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_CHECK_REPOSITORY_CONNECTION)) {
            return handleRepositoryCheckConnection(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_CHECK_PACKAGE_CONNECTION)) {
            return handlePackageCheckConnection(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_LATEST_REVISION)) {
            return handleGetLatestRevision(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_LATEST_REVISION_SINCE)) {
            return handleLatestRevisionSince(goPluginApiRequest);
        }
        return null;
    }

    private GoPluginApiResponse handleLatestRevisionSince(GoPluginApiRequest goPluginApiRequest) {
        final Map<String, String> repositoryConfig = keyValuePairs(goPluginApiRequest, REQUEST_REPOSITORY_CONFIGURATION);
        final Map<String, String> packageConfig = keyValuePairs(goPluginApiRequest, REQUEST_PACKAGE_CONFIGURATION);
        final Map<String, Object> previousRevision = getMapFor(goPluginApiRequest, REQUEST_PREVIOUS_REVISION);

        String mlflowUrl = repositoryConfig.get(MLFLOW_URL);
        Integer prRunsWithin = getPRValidityDays(repositoryConfig);
        Integer experimentId = Integer.parseInt(packageConfig.get(EXPERIMENT_ID));
        String promoteTagKey = packageConfig.get(PROMOTION_TAG_NAME);
        String promoteTagValue = packageConfig.get(PROMOTION_TAG_VALUE);
        String prIdTagName = packageConfig.get(PR_ID_TAG_NAME);

        try {
            RevisionStatus status = getLatestPromotedRun(mlflowUrl, experimentId, promoteTagKey, promoteTagValue,
                    prIdTagName, prRunsWithin);
            if(!Objects.equals(previousRevision.get("revision"), status.runId)) {
                return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, status.toMap());
            }

            return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, null);
        }
        catch(Exception ex) {
            logger.error("Error while trying to get newer promoted run info from MLFlow", ex);
            return createResponse(DefaultGoPluginApiResponse.INTERNAL_ERROR, null);
        }

    }

    private GoPluginApiResponse handleGetLatestRevision(GoPluginApiRequest goPluginApiRequest) {
        final Map<String, String> repositoryConfig = keyValuePairs(goPluginApiRequest, REQUEST_REPOSITORY_CONFIGURATION);
        final Map<String, String> packageConfig = keyValuePairs(goPluginApiRequest, REQUEST_PACKAGE_CONFIGURATION);

        String mlflowUrl = repositoryConfig.get(MLFLOW_URL);
        Integer prRunsWithin = getPRValidityDays(repositoryConfig);
        Integer experimentId = Integer.parseInt(packageConfig.get(EXPERIMENT_ID));
        String promoteTagKey = packageConfig.get(PROMOTION_TAG_NAME);
        String promoteTagValue = packageConfig.get(PROMOTION_TAG_VALUE);
        String prIdTagName = packageConfig.get(PR_ID_TAG_NAME);

        try {
            RevisionStatus status = getLatestPromotedRun(mlflowUrl, experimentId, promoteTagKey, promoteTagValue,
                    prIdTagName, prRunsWithin);
            return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, status.toMap());
        }
        catch(Exception ex) {
            logger.error("Error while trying to get latest promoted run info from MLFlow", ex);
            return createResponse(DefaultGoPluginApiResponse.INTERNAL_ERROR, null);
        }
    }

    private RevisionStatus getLatestPromotedRun(String mlflowUrl, Integer experimentId, String promoteTagKey,
                                                String promoteTagValue, String prIdTagName, Integer prRunsWithin) throws IOException {
        Map<String, Object> payload = new HashMap<>();
        List<Integer> experimentIds = new ArrayList<>();
        experimentIds.add(experimentId);
        payload.put("experiment_ids", experimentIds);
        payload.put("run_view_type", "ACTIVE_ONLY");
        HttpResponse response = requestFactory.buildPostRequest(
                new GenericUrl(String.format("%s%s", mlflowUrl, MLFLOW_SEARCH_RUNS_ENDPOINT)),
                new JsonHttpContent(this.jsonFactory, payload))
                .setParser(new JsonObjectParser(this.jsonFactory))
                .execute();
        SearchResponse searchResponse = response.parseAs(SearchResponse.class);
        Run latestPromotedRun = searchResponse.latestWithTag(promoteTagKey, promoteTagValue);

        Map<String, String> additionalRevisionData = new HashMap<>();
        additionalRevisionData.put("ARTIFACT_URI", latestPromotedRun.info.artifact_uri);
        String revision = latestPromotedRun.info.run_uuid;

        if(!Strings.isNullOrEmpty(prIdTagName)) {
            HashMap<String, Run> runsWithPRTag = searchResponse.runsWithinDuration(prIdTagName, prRunsWithin);
            runsWithPRTag.forEach((prId, run) -> additionalRevisionData.put("PR_ID_ARTIFACT_URI_" + prId, run.info.artifact_uri));
            revision = getAllRunIdsString(revision, runsWithPRTag.values());
        }
        return new RevisionStatus(
                revision,
                latestPromotedRun.info.end_time,
                String.format("%s#/experiments/%s/runs/%s", mlflowUrl, experimentId, latestPromotedRun.info.run_uuid),
                latestPromotedRun.data.getUser(),
                latestPromotedRun.info.run_uuid,
                additionalRevisionData);
    }

    private String getAllRunIdsString(String run_uuid, Collection<Run> prRuns) {
        List<Run> list = new ArrayList<>(prRuns);
        list.sort(Comparator.comparing(o -> o.info.run_uuid));
        StringBuilder allRuns = new StringBuilder(run_uuid);
        list.forEach(run-> allRuns.append(";").append(run.info.run_uuid));
        return allRuns.toString();
    }

    private GoPluginApiResponse handlePackageCheckConnection(GoPluginApiRequest goPluginApiRequest) {
        final Map<String, String> repositoryConfig = keyValuePairs(goPluginApiRequest, REQUEST_REPOSITORY_CONFIGURATION);
        final Map<String, String> packageConfig = keyValuePairs(goPluginApiRequest, REQUEST_PACKAGE_CONFIGURATION);

        String mlflowUrl = repositoryConfig.get(MLFLOW_URL);
        String experimentId = packageConfig.get(EXPERIMENT_ID);

        MaterialResult result;
        if(StringUtils.isBlank(mlflowUrl)) {
            result = new MaterialResult(false, "Experiment id must be specified");
        } else {
            try {
                GenericUrl getExperimentUrl = new GenericUrl(String.format("%s%s?experiment_id=%s", mlflowUrl, MLFLOW_GET_EXPERIMENT_ENDPOINT, experimentId));
                HttpResponse response = requestFactory.buildGetRequest(getExperimentUrl).execute();
                if (response.getStatusCode() != 200) {
                    result = new MaterialResult(false, String.format("Experiment %s not found", experimentId));
                } else {
                    result = new MaterialResult(true, "Success");
                }
            } catch(IOException ex) {
                result = new MaterialResult(false, String.format("Unable to reach MLFlow at %s - %s", mlflowUrl, ex.getMessage()));
                logger.error("Unable to reach mlflow", ex);
            }
        }

        return createResponse(result.responseCode(), result.toMap());
    }

    private GoPluginApiResponse handleRepositoryCheckConnection(GoPluginApiRequest goPluginApiRequest) {
        final Map<String, String> repositoryConfig = keyValuePairs(goPluginApiRequest, REQUEST_REPOSITORY_CONFIGURATION);

        MaterialResult result;
        String mlflowUrl = repositoryConfig.get(MLFLOW_URL);

        if(StringUtils.isBlank(mlflowUrl)) {
            result = new MaterialResult(false, "MLFlow url must be specified");
        } else {
            try {
                HttpResponse response = requestFactory.buildGetRequest(new GenericUrl(mlflowUrl)).execute();
                if (response.getStatusCode() != 200) {
                    result = new MaterialResult(false, String.format("Unable to reach MLFlow at %s", mlflowUrl));
                } else {
                    result = new MaterialResult(true, "Success");
                }
            } catch(IOException ex) {
                result = new MaterialResult(false, String.format("Unable to reach MLFlow at %s - %s", mlflowUrl, ex.getMessage()));
                logger.error("Unable to reach mlflow", ex);
            }
        }

        return createResponse(result.responseCode(), result.toMap());
    }

    private GoPluginApiResponse handlePackageValidation(GoPluginApiRequest goPluginApiRequest) {
        List<Map<String, Object>> validationResult = new ArrayList<>();
        final Map<String, String> packageConfig = keyValuePairs(goPluginApiRequest, REQUEST_PACKAGE_CONFIGURATION);

        String experimentId = packageConfig.get(EXPERIMENT_ID);
        if(StringUtils.isBlank(experimentId)) {
            addError(EXPERIMENT_ID, "Experiment ID must be specified", validationResult);
        }

        return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, validationResult);
    }

    private GoPluginApiResponse handleRepositoryValidation(GoPluginApiRequest goPluginApiRequest) {
        List<Map<String, Object>> validationResult = new ArrayList<>();
        final Map<String, String> repositoryConfig = keyValuePairs(goPluginApiRequest, REQUEST_REPOSITORY_CONFIGURATION);

        String mlflowUrl = repositoryConfig.get(MLFLOW_URL);
        if(StringUtils.isBlank(mlflowUrl)) {
            addError(MLFLOW_URL, "MLFlow URL must be specified", validationResult);
        }

        return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, validationResult);
    }

    private GoPluginApiResponse handlePackageConfiguration() {
        Map<String, Object> response = new HashMap<>();
        response.put(EXPERIMENT_ID,
                createField("Experiment ID", null, true, true, false, "1")
        );
        response.put(PROMOTION_TAG_NAME,
                createField("Promotion Tag Name", "promote", true, false, false, "2")
        );
        response.put(PROMOTION_TAG_VALUE,
                createField("Promotion Tag Value", "true", true, false, false, "3")
        );
        response.put(PR_ID_TAG_NAME,
                createField("PR ID Tag Name", "", true, false, false, "4")
        );
        return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, response);
    }

    private GoPluginApiResponse handleRepositoryConfiguration() {
        Map<String, Object> response = new HashMap<>();
        response.put(MLFLOW_URL,
                createField("MLFlow URL", null, true, true, false, "1")
        );
        response.put(PR_RUNS_WITHIN_DAYS,
                createField("PR runs within (days)", "0", true, false, false, "2"));
        return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, response);
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier("package-repository", Arrays.asList("1.0"));
    }

    private List<Map<String, Object>> addError(String key, String message, List<Map<String, Object>> validationResult) {
        HashMap<String, Object> errorMap = new HashMap<>();
        errorMap.put("key", key);
        errorMap.put("message", message);
        validationResult.add(errorMap);
        return validationResult;
    }

    private GoPluginApiResponse createResponse(int responseCode, Object body) {
        final DefaultGoPluginApiResponse response = new DefaultGoPluginApiResponse(responseCode);
        response.setResponseBody(new GsonBuilder().serializeNulls().create().toJson(body));
        return response;
    }

    private Map<String, Object> createField(String displayName, String defaultValue, boolean isPartOfIdentity, boolean isRequired, boolean isSecure, String displayOrder) {
        Map<String, Object> fieldProperties = new HashMap<>();
        fieldProperties.put("display-name", displayName);
        fieldProperties.put("default-value", defaultValue);
        fieldProperties.put("part-of-identity", isPartOfIdentity);
        fieldProperties.put("required", isRequired);
        fieldProperties.put("secure", isSecure);
        fieldProperties.put("display-order", displayOrder);
        return fieldProperties;
    }

    private Map<String, Object> getMapFor(GoPluginApiRequest goPluginApiRequest, String field) {
        Map<String, Object> map = (Map<String, Object>) new GsonBuilder().create().fromJson(goPluginApiRequest.requestBody(), Object.class);
        Map<String, Object> fieldProperties = (Map<String, Object>) map.get(field);
        return fieldProperties;
    }

    private Map<String, String> keyValuePairs(GoPluginApiRequest goPluginApiRequest, String mainKey) {
        Map<String, String> keyValuePairs = new HashMap<>();
        Map<String, Object> map = (Map<String, Object>) new GsonBuilder().create().fromJson(goPluginApiRequest.requestBody(), Object.class);
        Map<String, Object> fieldsMap = (Map<String, Object>) map.get(mainKey);
        for (String field : fieldsMap.keySet()) {
            Map<String, Object> fieldProperties = (Map<String, Object>) fieldsMap.get(field);
            String value = (String) fieldProperties.get("value");
            keyValuePairs.put(field, value);
        }
        return keyValuePairs;
    }

    private Integer getPRValidityDays(Map<String, String> repositoryConfig) {
        String within = repositoryConfig.get(PR_RUNS_WITHIN_DAYS);
        if(!Strings.isNullOrEmpty(within)) {
            return Integer.parseInt(repositoryConfig.get(PR_RUNS_WITHIN_DAYS));
        }
        return 0;
    }
}