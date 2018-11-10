package com.indix.mlflow_gocd.mlfow;

import com.google.api.client.util.Key;
import com.indix.mlflow_gocd.utils.Functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.indix.mlflow_gocd.utils.Lists.filter;

public class RunData {
    @Key
    public ArrayList<RunTag> tags;

    public RunTag getTag(String key) {
        if(tags == null) return null;

        List<RunTag> filtered = filter(tags, new Functions.Predicate<RunTag>() {
            @Override
            public Boolean execute(RunTag tag) {
                return Objects.equals(tag.key, key);
            }
        });
        if(filtered.size() > 0) {
            return filtered.get(0);
        }

        return null;
    }
}
