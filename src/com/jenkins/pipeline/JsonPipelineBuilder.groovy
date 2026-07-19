package com.jenkins.pipeline

/**
 * JSON Pipeline 构建器 —— 将 PipelineConfig 对象组装成声明式 Jenkins Pipeline
 *
 * 核心思路：通过在 Groovy 闭包中动态调用 pipeline { ... } DSL。
 * 由于 Jenkins 声明式 pipeline 的 stages/steps 必须在编译期确定，
 * 我们使用 script 块来桥接 JSON 配置和实际的 Jenkins step 调用。
 */
class JsonPipelineBuilder implements Serializable {

    def pipelineScript       // Jenkinsfile 的 this 引用

    JsonPipelineBuilder(def pipelineScript) {
        this.pipelineScript = pipelineScript
    }

    /**
     * 根据 PipelineConfig 构建并运行完整 pipeline
     */
    void build(PipelineConfig config) {
        // 配置 pipeline 级别的 agent
        pipelineScript.node(config.agent ?: 'any') {
            // 设置环境变量
            if (config.environment) {
                config.environment.each { k, v ->
                    pipelineScript.env."$k" = v.toString()
                }
            }

            // 配置 tools
            config.tools?.each { name, version ->
                pipelineScript.tool(name: version, type: name)
            }

            try {
                // 执行所有 stages
                config.stages.each { stageConfig ->
                    executeStage(stageConfig, config)
                }
            } catch (e) {
                // 如果配置了 post/failure，执行失败后处理
                if (config.post) {
                    StepExecutor.executePost(pipelineScript, config.post, 'failure')
                }
                throw e
            } finally {
                // 执行 post/always
                if (config.post) {
                    StepExecutor.executePost(pipelineScript, config.post, 'always')
                }
            }
        }
    }

    /**
     * 执行单个 stage
     */
    private void executeStage(StageConfig stageConfig, PipelineConfig pipelineConfig) {
        // 检查 when 条件
        if (!shouldExecute(stageConfig.when)) {
            pipelineScript.echo("[SKIP] Stage '${stageConfig.name}' skipped by when condition")
            return
        }

        // 确定 agent（stage 级别可覆盖）
        def agent = stageConfig.agent ?: pipelineConfig.agent ?: 'any'

        pipelineScript.node(agent) {
            def stageName = stageConfig.name ?: 'Unnamed Stage'
            pipelineScript.stage(stageName) {

                // 设置 stage 级别环境变量
                stageConfig.environment?.each { k, v ->
                    pipelineScript.env."$k" = v.toString()
                }

                try {
                    // 处理并行 stages
                    if (stageConfig.parallel) {
                        executeParallel(stageConfig)
                    }

                    // 执行 steps
                    stageConfig.steps?.each { step ->
                        StepExecutor.execute(pipelineScript, step)
                    }

                } catch (e) {
                    if (stageConfig.post) {
                        StepExecutor.executePost(pipelineScript, stageConfig.post, 'failure')
                    }
                    throw e
                } finally {
                    if (stageConfig.post) {
                        StepExecutor.executePost(pipelineScript, stageConfig.post, 'always')
                    }
                }
            }
        }
    }

    /**
     * 执行并行 stages
     */
    private void executeParallel(StageConfig stageConfig) {
        def parallelBranches = [:]

        stageConfig.parallel.each { parallelStage ->
            parallelBranches[parallelStage.name] = {
                parallelStage.steps?.each { step ->
                    StepExecutor.execute(pipelineScript, step)
                }
            }
        }

        pipelineScript.parallel(parallelBranches)
    }

    /**
     * 检查 when 条件是否满足
     */
    private boolean shouldExecute(WhenConfig whenConfig) {
        if (!whenConfig) return true

        // branch 条件
        if (whenConfig.branch) {
            def currentBranch = pipelineScript.env.BRANCH_NAME ?: ''
            if (currentBranch != whenConfig.branch) return false
        }

        // environment 条件
        if (whenConfig.environment) {
            def envName = whenConfig.environment
            if (!pipelineScript.env."$envName") return false
        }

        // expression 条件
        if (whenConfig.expression) {
            try {
                def result = pipelineScript.evaluate(whenConfig.expression)
                if (!result) return false
            } catch (e) {
                return false
            }
        }

        // tag 条件
        if (whenConfig.tag) {
            def tagName = pipelineScript.env.TAG_NAME ?: ''
            if (tagName != whenConfig.tag) return false
        }

        // tagPattern 条件
        if (whenConfig.tagPattern) {
            def tagName = pipelineScript.env.TAG_NAME ?: ''
            if (!(tagName ==~ whenConfig.tagPattern)) return false
        }

        // equals 条件
        if (whenConfig.equals) {
            def allMatch = whenConfig.equals.every { k, v ->
                def actual = pipelineScript.env."$k"
                def expected = v instanceof String && v.startsWith('${') ?
                    pipelineScript.evaluate("return \"${v}\"") : v
                return actual == expected
            }
            if (!allMatch) return false
        }

        // allOf
        if (whenConfig.allOf) {
            def allMatch = whenConfig.allOf.every { subWhen -> shouldExecute(subWhen) }
            if (!allMatch) return false
        }

        // anyOf
        if (whenConfig.anyOf) {
            def anyMatch = whenConfig.anyOf.any { subWhen -> shouldExecute(subWhen) }
            if (!anyMatch) return false
        }

        return true
    }
}
