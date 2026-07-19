package com.jenkins.pipeline

/**
 * Pipeline Options 配置
 */
class OptionsConfig implements Serializable {

    Boolean skipDefaultCheckout
    Boolean disableConcurrentBuilds
    Boolean preserveStashes
    Integer buildDiscarderDays
    Integer buildDiscarderNum
    String timeout
    String timeoutUnit = 'HOURS'
    Boolean timestamps
    Boolean ansiColor
    List<String> retry = []

    static OptionsConfig fromJson(Map json) {
        def opts = new OptionsConfig(
            skipDefaultCheckout: json.skipDefaultCheckout,
            disableConcurrentBuilds: json.disableConcurrentBuilds,
            preserveStashes: json.preserveStashes,
            buildDiscarderDays: json.buildDiscarderDays,
            buildDiscarderNum: json.buildDiscarderNum,
            timeout: json.timeout,
            timeoutUnit: json.timeoutUnit ?: 'HOURS',
            timestamps: json.timestamps,
            ansiColor: json.ansiColor
        )
        if (json.retry) {
            opts.retry = json.retry instanceof List ? json.retry : [json.retry]
        }
        return opts
    }
}
