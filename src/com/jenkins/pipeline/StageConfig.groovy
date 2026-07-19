package com.jenkins.pipeline

/**
 * Stage 配置模型
 */
class StageConfig implements Serializable {

    String name
    String agent                          // 可为 null，继承 pipeline 级别 agent
    List<StepConfig> steps = []
    List<StageConfig> parallel = []       // 并行 stage
    PostConfig post
    WhenConfig when
    Map<String, String> environment = [:]
    List<String> tools = []
    Map<String, Object> options = [:]

    static StageConfig fromJson(Map json) {
        def config = new StageConfig(
            name: json.name ?: 'Unnamed Stage',
            agent: json.agent,
            environment: json.environment ? new HashMap<>(json.environment) : [:],
            tools: json.tools ?: []
        )

        // 解析 steps
        json.steps?.each { stepJson ->
            if (stepJson instanceof Map) {
                config.steps << StepConfig.fromJson(stepJson)
            } else if (stepJson instanceof String) {
                // 字符串简写：默认当作 echo
                config.steps << new StepConfig(type: 'echo', params: [message: stepJson])
            }
        }

        // 解析并行 stages
        json.parallel?.each { parJson ->
            config.parallel << StageConfig.fromJson(parJson)
        }

        // 解析 post
        if (json.post) {
            config.post = PostConfig.fromJson(json.post)
        }

        // 解析 when
        if (json.when) {
            config.when = WhenConfig.fromJson(json.when)
        }

        return config
    }
}
