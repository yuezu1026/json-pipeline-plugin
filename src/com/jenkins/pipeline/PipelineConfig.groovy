package com.jenkins.pipeline

import groovy.json.JsonSlurper

/**
 * Pipeline 配置模型 —— 将 JSON 配置解析为强类型对象
 */
class PipelineConfig implements Serializable {

    String name
    String agent = 'any'
    List<StageConfig> stages = []
    List<ParameterConfig> parameters = []
    Map<String, String> environment = [:]
    PostConfig post
    Map<String, String> tools = [:]
    List<TriggerConfig> triggers = []
    OptionsConfig options

    /**
     * 从 JSON 字符串解析 PipelineConfig
     */
    static PipelineConfig fromJson(String jsonStr) {
        def slurper = new JsonSlurper()
        def root = slurper.parseText(jsonStr)
        def pipeline = root.pipeline ?: root

        def config = new PipelineConfig(
            name: pipeline.name ?: 'Default Pipeline',
            agent: pipeline.agent ?: 'any',
            environment: pipeline.environment ?: [:],
            tools: pipeline.tools ?: [:]
        )

        // 解析 stages
        pipeline.stages?.each { stageJson ->
            config.stages << StageConfig.fromJson(stageJson)
        }

        // 解析 parameters
        pipeline.parameters?.each { paramJson ->
            config.parameters << ParameterConfig.fromJson(paramJson)
        }

        // 解析 post
        if (pipeline.post) {
            config.post = PostConfig.fromJson(pipeline.post)
        }

        // 解析 triggers
        pipeline.triggers?.each { triggerJson ->
            config.triggers << TriggerConfig.fromJson(triggerJson)
        }

        // 解析 options
        if (pipeline.options) {
            config.options = OptionsConfig.fromJson(pipeline.options)
        }

        return config
    }

    /**
     * 从 JSON 文件路径解析 PipelineConfig
     */
    static PipelineConfig fromFile(String filePath) {
        def script = new groovy.text.StreamingTemplateEngine().createTemplate('')
        def text = new File(filePath).text
        return fromJson(text)
    }
}
