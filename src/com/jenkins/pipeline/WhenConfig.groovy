package com.jenkins.pipeline

/**
 * When 条件配置 —— 控制 stage 是否执行
 */
class WhenConfig implements Serializable {

    String branch
    String environment
    String expression
    String tag
    String tagPattern
    String triggeredBy
    Boolean beforeAgent
    Boolean beforeInput
    Boolean beforeOptions
    List<WhenConfig> allOf = []
    List<WhenConfig> anyOf = []
    Map<String, String> equals = [:]

    static WhenConfig fromJson(Map json) {
        return new WhenConfig(
            branch: json.branch,
            environment: json.environment,
            expression: json.expression,
            tag: json.tag,
            tagPattern: json.tagPattern,
            triggeredBy: json.triggeredBy,
            beforeAgent: json.beforeAgent,
            beforeInput: json.beforeInput,
            beforeOptions: json.beforeOptions,
            allOf: json.allOf?.collect { WhenConfig.fromJson(it) } ?: [],
            anyOf: json.anyOf?.collect { WhenConfig.fromJson(it) } ?: [],
            equals: json.equals ?: [:]
        )
    }
}
