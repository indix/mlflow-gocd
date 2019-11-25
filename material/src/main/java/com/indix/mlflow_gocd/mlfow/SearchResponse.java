package com.indix.mlflow_gocd.mlfow;

import com.google.api.client.util.Key;
import com.indix.mlflow_gocd.MLFlowPackagePlugin;
import com.indix.mlflow_gocd.utils.Functions;
import com.thoughtworks.go.plugin.api.logging.Logger;

import java.time.Instant;
import java.time.Period;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.indix.mlflow_gocd.utils.Lists.filter;

public class SearchResponse {
    @Key
    public ArrayList<Run> runs;

    public Run latestWithTag(String key, String value) {
        List<Run> promotedRuns = filter(runs, new Functions.Predicate<Run>() {
            @Override
            public Boolean execute(Run run) {
                return run.isFinished() && run.hasTagOfValue(key, value);
            }
        });
        promotedRuns.sort((o1, o2) -> Long.parseLong(o1.info.end_time) <= Long.parseLong(o2.info.end_time) ? 1 : -1);

        return promotedRuns.size() > 0 ? promotedRuns.get(0) : null;
    }

    public HashMap<String, Run> runsWithinDuration(String key, int withinDays) {

        Instant lastDate = new Date().toInstant().minus(Period.ofDays(withinDays));
        List<Run> prRuns = filter(runs, new Functions.Predicate<Run>() {
            @Override
            public Boolean execute(Run run) {
                if(!run.isFinished()) {
                    return false;
                }
                Instant end_time = Instant.ofEpochMilli(Long.parseLong(run.info.end_time));
                return run.hasTag(key) && lastDate.isBefore(end_time);
            }
        });
        prRuns.sort((o1, o2) -> Long.parseLong(o1.info.end_time) <= Long.parseLong(o2.info.end_time) ? 1 : -1);

        HashMap<String, Run> prIdMap = new HashMap<>();
        prRuns.forEach(run-> {
            String prId = run.data.getTag(key).value;
            if (!prIdMap.containsKey(prId)) {
                prIdMap.put(prId, run);
            }
        });
        return prIdMap;
    }
}
