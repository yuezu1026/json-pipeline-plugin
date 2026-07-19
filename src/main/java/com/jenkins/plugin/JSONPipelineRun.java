package com.jenkins.plugin;

import hudson.model.BuildListener;
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
    public void run() {
        // 调用父类的 execute() 来正确初始化构建生命周期
        execute(new RunExecution() {
            @Override
            public void run(BuildListener listener) throws Exception {
                listener.getLogger().println("JSON Pipeline Build Started");

                JSONPipelineJob job = getParent();
                String script = job.getPipelineScript();
                if (script != null && !script.isEmpty()) {
                    listener.getLogger().println("Generated Pipeline Script:");
                    listener.getLogger().println(script);
                }
                listener.getLogger().println("Build completed successfully.");
            }

            @Override
            public void cleanUp(BuildListener listener) {
                // 清理资源（当前无需额外清理）
            }
        });
    }

    @Override
    public String getWhyKeepLog() {
        return null;
    }
}
