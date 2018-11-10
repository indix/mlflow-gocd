package com.indix.mlflow_gocd.mlfow;

import com.google.api.client.util.Key;
import com.indix.mlflow_gocd.utils.Functions;

import java.util.ArrayList;

import static com.indix.mlflow_gocd.utils.Lists.filter;

public class SearchResponse {
    @Key
    public ArrayList<Run> runs;

    public Run latestWithTag(String key, String value) {
        filter(runs, new Functions.Predicate<Run>() {
            @Override
            public Boolean execute(Run run) {
                return run.hasTagOfValue(key, value);
            }
        }).sort((o1, o2) -> o1.info.end_time > o2.info.end_time ? 1 : 0);

        return null;
    }
}
