package com.indix.mlflow_gocd.mlfow;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

import java.util.Map;

public class RunInfo {
    @Key
    public String run_uuid;
    @Key
    public long end_time;
}
