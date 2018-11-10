package com.indix.mlflow_gocd.models;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RevisionStatus {
    public String runId;
    public Date endTime;
    public String trackbackUrl;
    public String user;
    public String revisionLabel;
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public RevisionStatus(String runId, String endTime, String trackbackUrl, String user, String revisionLabel) {
        this.runId = runId;
        this.endTime = new Date(Long.parseLong(endTime));
        this.trackbackUrl = trackbackUrl;
        this.user = user;
        this.revisionLabel = revisionLabel;
    }

    public Map toMap() {
        final HashMap result = new HashMap();
        result.put("revision", runId);
        result.put("timestamp", new SimpleDateFormat(DATE_PATTERN).format(endTime));
        result.put("user", user);
        result.put("revisionComment", String.format("New model pushed in run: %s", runId));
        result.put("trackbackUrl", trackbackUrl);

        return result;
    }
}
