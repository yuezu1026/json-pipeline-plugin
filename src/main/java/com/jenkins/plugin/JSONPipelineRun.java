package com.jenkins.plugin;

import hudson.model.Run;
import java.io.File;
import java.io.IOException;

public class JSONPipelineRun extends Run<JSONPipelineJob, JSONPipelineRun> {

    public JSONPipelineRun(JSONPipelineJob job) throws IOException {
        super(job);
    }

    public JSONPipelineRun(JSONPipelineJob job, File buildDir) throws IOException {
        super(job, buildDir);
    }

    @Override
    public String getWhyKeepLog() {
        return null;
    }
}
