package com.indix.mlflow_gocd;

import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.util.Arrays;

@Extension
public class MLFlowPackagePlugin implements GoPlugin {
    public static final String REQUEST_REPOSITORY_CONFIGURATION = "repository-configuration";
    public static final String REQUEST_PACKAGE_CONFIGURATION = "package-configuration";
    public static final String REQUEST_VALIDATE_REPOSITORY_CONFIGURATION = "validate-repository-configuration";
    public static final String REQUEST_VALIDATE_PACKAGE_CONFIGURATION = "validate-package-configuration";
    public static final String REQUEST_CHECK_REPOSITORY_CONNECTION = "check-repository-connection";
    public static final String REQUEST_CHECK_PACKAGE_CONNECTION = "check-package-connection";
    public static final String REQUEST_LATEST_REVISION = "latest-revision";
    public static final String REQUEST_LATEST_REVISION_SINCE = "latest-revision-since";

    private static Logger logger = Logger.getLoggerFor(MLFlowPackagePlugin.class);

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
            return handlePackageValidation();
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
        return null;
    }

    private GoPluginApiResponse handleGetLatestRevision(GoPluginApiRequest goPluginApiRequest) {
        return null;
    }

    private GoPluginApiResponse handlePackageCheckConnection(GoPluginApiRequest goPluginApiRequest) {
        return null;
    }

    private GoPluginApiResponse handleRepositoryCheckConnection(GoPluginApiRequest goPluginApiRequest) {
        return null;
    }

    private GoPluginApiResponse handlePackageValidation() {
        return null;
    }

    private GoPluginApiResponse handleRepositoryValidation(GoPluginApiRequest goPluginApiRequest) {
        return null;
    }

    private GoPluginApiResponse handlePackageConfiguration() {
        return null;
    }

    private GoPluginApiResponse handleRepositoryConfiguration() {
        return null;
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier("package-repository", Arrays.asList("1.0"));
    }
}