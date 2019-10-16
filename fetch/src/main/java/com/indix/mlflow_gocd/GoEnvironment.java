package com.indix.mlflow_gocd;

import org.apache.commons.lang3.BooleanUtils;

import java.util.*;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class GoEnvironment {
    public static final String AWS_USE_IAM_ROLE = "AWS_USE_IAM_ROLE";
    public static final String AWS_REGION = "AWS_REGION";
    public static final String AWS_SECRET_ACCESS_KEY = "AWS_SECRET_ACCESS_KEY";
    public static final String AWS_ACCESS_KEY_ID = "AWS_ACCESS_KEY_ID";

    private Pattern envPat = Pattern.compile("\\$\\{(\\w+)\\}");
    private Map<String, String> environment = new HashMap<>();

    public GoEnvironment() {
        this.environment.putAll(System.getenv());
    }

    public GoEnvironment(Map<String, String> defaultEnvironment) {
        this();
        this.environment.putAll(defaultEnvironment);
    }

    public GoEnvironment putAll(Map<String, String> existing) {
        environment.putAll(existing);
        return this;
    }

    public Map<String,String> asMap() { return environment; }

    public String get(String name) {
        return environment.get(name);
    }

    public String getOrElse(String name, String defaultValue) {
        if(has(name)) return get(name);
        else return defaultValue;
    }

    public String getByPattern(String patternStr) {
        String key = getEnvVarByPattern(patternStr);
        if(key == null) {
            return null;
        }
        return environment.get(key);
    }

    public String getEnvVarByPattern(String patternStr) {
        Pattern pattern = Pattern.compile(patternStr);
        for(String key: environment.keySet()) {
            if(pattern.matcher(key).matches()) {
                return key;
            }
        }
        return null;
    }

    public boolean has(String name) {
        return environment.containsKey(name) && isNotEmpty(get(name));
    }

    public boolean isAbsent(String name) {
        return !has(name);
    }

    private static final List<String> validUseIamRoleValues = new ArrayList<String>(Arrays.asList("true", "false", "yes", "no", "on", "off"));
    public boolean hasAWSUseIamRole() {
        if (!has(AWS_USE_IAM_ROLE)) {
            return false;
        }

        String useIamRoleValue = get(AWS_USE_IAM_ROLE);
        Boolean result = BooleanUtils.toBooleanObject(useIamRoleValue);
        if (result == null) {
            throw new IllegalArgumentException(getEnvInvalidFormatMessage(AWS_USE_IAM_ROLE,
                    useIamRoleValue, validUseIamRoleValues.toString()));
        }
        else {
            return result.booleanValue();
        }
    }

    private String getEnvInvalidFormatMessage(String environmentVariable, String value, String expected){
        return String.format(
                "Unexpected value in %s environment variable; was %s, but expected one of the following %s",
                environmentVariable, value, expected);
    }
}
