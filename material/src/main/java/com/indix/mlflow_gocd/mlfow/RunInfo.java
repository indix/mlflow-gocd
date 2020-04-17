package com.indix.mlflow_gocd.mlfow;

import com.google.api.client.util.Key;

public class RunInfo {
    @Key
    public String run_uuid;
    @Key
    public String end_time;
    @Key
    public String artifact_uri;
    @Key
    public String status;
}
