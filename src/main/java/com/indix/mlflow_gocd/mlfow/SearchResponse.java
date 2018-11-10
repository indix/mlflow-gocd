package com.indix.mlflow_gocd.mlfow;

import com.google.api.client.util.Key;
import com.indix.mlflow_gocd.utils.Functions;

import java.util.ArrayList;
import java.util.List;

import static com.indix.mlflow_gocd.utils.Lists.filter;

public class SearchResponse {
    @Key
    public ArrayList<Run> runs;

    public Run latestWithTag(String key, String value) {
        List<Run> promotedRuns = filter(runs, new Functions.Predicate<Run>() {
            @Override
            public Boolean execute(Run run) {
                return run.hasTagOfValue(key, value);
            }
        });
        promotedRuns.sort((o1, o2) -> Long.parseLong(o1.info.end_time) > Long.parseLong(o2.info.end_time) ? 1 : 0);

        return promotedRuns.size() > 0 ? promotedRuns.get(0) : null;
    }
}
