package com.jenkins.pipeline

/**
 * 触发器配置
 *
 * 支持的 type:
 *   cron, pollSCM, upstream
 */
class TriggerConfig implements Serializable {

    String type
    String spec       // cron 表达式 / 上游项目名
    String threshold  // upstream 阈值

    static TriggerConfig fromJson(Map json) {
        return new TriggerConfig(
            type: json.type,
            spec: json.spec ?: json.cron,
            threshold: json.threshold
        )
    }
}
