package com.jenkins.plugin;

import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Descriptor;
import hudson.model.ItemGroup;
import hudson.model.Items;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * JSON Pipeline Job —— 自定义 Jenkins Job 类型。
 *
 * 用户创建 Job 时只需填写:
 *   1. sourceCodePath — 待构建工程的 GitHub 仓库地址
 *   2. configRepoUrl  — Git 仓库 URL (存放 JSON 配置)
 *   3. configPath     — JSON 文件在仓库中的路径
 *   4. configBranch   — 分支 (默认 main)
 *   5. credentialsId  — Git 凭证 (可选)
 *
 * Job 保存后自动生成 Pipeline 脚本:
 *   @Library('jenkins-json-pipeline') _
 *   jsonPipeline { configRepo = '...'; configPath = '...' }
 */
public class JSONPipelineJob extends Job<JSONPipelineJob, JSONPipelineRun> implements TopLevelItem, Queue.Task {

    private static final Logger LOGGER = Logger.getLogger(JSONPipelineJob.class.getName());

    private String sourceCodePath = "";
    private String configRepoUrl = "";
    private String configPath = "";
    private String configBranch = "main";
    private String credentialsId = "";
    private String pipelineScript;

    @DataBoundConstructor
    public JSONPipelineJob(ItemGroup parent, String name) {
        super(parent, name);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) Jenkins.get().getDescriptorOrDie(getClass());
    }

    public String getSourceCodePath() { return sourceCodePath; }
    public String getConfigRepoUrl()  { return configRepoUrl; }
    public String getConfigPath()     { return configPath; }
    public String getConfigBranch()   { return configBranch; }
    public String getCredentialsId()  { return credentialsId; }

    @DataBoundSetter
    public void setSourceCodePath(String v) { this.sourceCodePath = v; }

    @DataBoundSetter
    public void setConfigRepoUrl(String v) { this.configRepoUrl = v; }

    @DataBoundSetter
    public void setConfigPath(String v) { this.configPath = v; }

    @DataBoundSetter
    public void setConfigBranch(String v) {
        this.configBranch = (v != null && !v.isEmpty()) ? v : "main";
    }

    @DataBoundSetter
    public void setCredentialsId(String v) { this.credentialsId = v; }

    @Override
    public boolean isBuildable() {
        return true;
    }

    @Override
    protected java.util.SortedMap<Integer, JSONPipelineRun> _getRuns() {
        return _allRuns();
    }

    @Override
    protected void removeRun(JSONPipelineRun run) {
        _allRuns().remove(run.getNumber());
    }

    private synchronized java.util.SortedMap<Integer, JSONPipelineRun> _allRuns() {
        if (runs == null) {
            runs = new java.util.TreeMap<>();
        }
        return runs;
    }
    private transient java.util.SortedMap<Integer, JSONPipelineRun> runs;

    /**
     * 自动生成 Pipeline 脚本
     */
    private void regenerateDefinition() {
        if (configRepoUrl == null || configRepoUrl.isEmpty()
                || configPath == null || configPath.isEmpty()) {
            LOGGER.warning("JSONPipelineJob [" + getName() + "]: 配置未完成，跳过生成");
            return;
        }

        this.pipelineScript = "@Library('jenkins-json-pipeline') _\n\n"
                + "jsonPipeline {\n"
                + (sourceCodePath != null && !sourceCodePath.isEmpty()
                        ? "    sourceCodePath = '" + escape(sourceCodePath) + "'\n"
                        : "")
                + "    configRepo = '" + escape(configRepoUrl) + "'\n"
                + "    configPath = '" + escape(configPath) + "'\n"
                + (configBranch != null && !configBranch.isEmpty()
                        ? "    configBranch = '" + escape(configBranch) + "'\n"
                        : "")
                + "}\n";

        LOGGER.info("JSONPipelineJob [" + getName() + "]: Pipeline 定义已生成");
    }

    public String getPipelineScript() {
        if (pipelineScript == null) {
            regenerateDefinition();
        }
        return pipelineScript;
    }

    // ============================================================
    // Build 支持 —— 提供 /build 端点
    // ============================================================

    /**
     * Queue.Task 接口 —— 创建可执行体
     */
    @Override
    public Queue.Executable createExecutable() throws IOException {
        return new JSONPipelineRun(this);
    }

    /**
     * 响应 "立即构建" 请求。
     */
    public void doBuild(StaplerRequest2 req, StaplerResponse2 rsp,
            @QueryParameter int delay) throws IOException, ServletException {
        Jenkins.get().getQueue().schedule(this, delay);
        rsp.sendRedirect2(req.getContextPath() + "/");
    }

    @Override
    protected void submit(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException, ServletException, Descriptor.FormException {
        super.submit(req, rsp);
        // 从请求参数中手动读取表单数据
        this.sourceCodePath = req.getParameter("_.sourceCodePath");
        this.configRepoUrl = req.getParameter("_.configRepoUrl");
        this.configPath = req.getParameter("_.configPath");
        this.configBranch = req.getParameter("_.configBranch");
        this.credentialsId = req.getParameter("_.credentialsId");
        if (this.configBranch == null || this.configBranch.isEmpty()) {
            this.configBranch = "main";
        }
        regenerateDefinition();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }

    // ============================================================
    // Descriptor —— 注册到 "New Item" 列表
    // ============================================================
    @Extension
    public static final class DescriptorImpl extends TopLevelItemDescriptor {

        static {
            // 注册到 XStream 白名单，避免序列化安全异常
            Jenkins.XSTREAM2.addCompatibilityAlias("com.jenkins.plugin.JSONPipelineJob", JSONPipelineJob.class);
            Items.XSTREAM2.addCompatibilityAlias("com.jenkins.plugin.JSONPipelineJob", JSONPipelineJob.class);
        }

        @Initializer(after = InitMilestone.PLUGINS_STARTED)
        public static void registerXStreamAliases() {
            Jenkins.XSTREAM2.addCompatibilityAlias("com.jenkins.plugin.JSONPipelineJob", JSONPipelineJob.class);
            Items.XSTREAM2.addCompatibilityAlias("com.jenkins.plugin.JSONPipelineJob", JSONPipelineJob.class);
        }

        @Override
        public String getDisplayName() {
            return "rick-json-pipeline";
        }

        @Override
        public String getDescription() {
            return "通过外部 Git 仓库的 JSON 配置文件定义流水线（需预装 jenkins-json-pipeline 共享库）";
        }

        @Override
        public TopLevelItem newInstance(ItemGroup parent, String name) {
            return new JSONPipelineJob(parent, name);
        }

        @Override
        public boolean isApplicableIn(ItemGroup parent) {
            return parent instanceof Jenkins;
        }

        // ---- 表单校验 ----
        public FormValidation doCheckConfigRepoUrl(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty())
                return FormValidation.error("请输入配置仓库 URL");
            if (!value.startsWith("http") && !value.startsWith("git@"))
                return FormValidation.warning("建议以 https:// 或 git@ 开头");
            return FormValidation.ok();
        }

        public FormValidation doCheckConfigPath(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty())
                return FormValidation.error("请输入 JSON 配置文件路径");
            if (!value.endsWith(".json"))
                return FormValidation.warning("配置文件通常以 .json 结尾");
            return FormValidation.ok();
        }
    }
}
