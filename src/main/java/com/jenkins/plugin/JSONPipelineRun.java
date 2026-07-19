package com.jenkins.plugin;

import hudson.model.BuildListener;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import java.io.File;
import java.io.IOException;

public class JSONPipelineRun extends Run<JSONPipelineJob, JSONPipelineRun> implements Queue.Executable {

    public JSONPipelineRun(JSONPipelineJob job) throws IOException {
        super(job);
    }

    public JSONPipelineRun(JSONPipelineJob job, File buildDir) throws IOException {
        super(job, buildDir);
    }

    public void run() {
        execute(new RunExecution() {
            @Override
            public Result run(BuildListener listener) throws Exception {
                listener.getLogger().println("JSON Pipeline Build Started");

                JSONPipelineJob job = getParent();
                String script = job.getPipelineScript();
                if (script != null && !script.isEmpty()) {
                    listener.getLogger().println("Generated Pipeline Script:");
                    listener.getLogger().println(script);
                }
                listener.getLogger().println("Build completed successfully.");
                return Result.SUCCESS;
            }

            @Override
            public void post(BuildListener listener) {
                // 构建后处理
            }

            @Override
            public void cleanUp(BuildListener listener) {
                // 清理
            }
        });
    }

    @Override
    public String getWhyKeepLog() {
        return null;
    }
}
