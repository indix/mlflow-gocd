package com.indix.mlflow_gocd.mlfow;

import com.google.api.client.util.Key;

import java.util.Objects;

public class Run {
    @Key
    public RunInfo info;
    @Key
    public RunData data;

    public boolean hasTagOfValue(String key, String value) {
        if(data == null) return false;

        RunTag tag = data.getTag(key);
        return tag != null && Objects.equals(tag.value, value);
    }

    public boolean hasTag(String key) {
        if(data == null) return false;
        return data.getTag(key) != null;
    }

    public boolean isFinished() {
        return Objects.equals(info.status, "FINISHED");
    }
}
