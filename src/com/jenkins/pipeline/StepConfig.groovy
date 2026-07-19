package com.jenkins.pipeline

/**
 * Step 配置模型
 *
 * 支持的 type:
 *   echo, sh, bat, checkout, dir, fileExists, error, isUnix, libraryResource,
 *   mail, deleteDir, stash, unstash, writeFile, readFile, retry, sleep, timeout,
 *   waitUntil, withCredentials, withEnv, script, container, build, input, publishHTML,
 *   junit, archiveArtifacts, parallel
 */
class StepConfig implements Serializable {

    String type                          // step 类型
    Map<String, Object> params = [:]     // step 参数
    List<StepConfig> children = []       // 嵌套 steps（如 dir, timeout, retry 等）

    static StepConfig fromJson(Map json) {
        def step = new StepConfig()
        step.type = json.type ?: json.remove('type')

        // 特殊处理：嵌套结构的 step（children）
        if (json.steps) {
            json.steps.each { childJson ->
                step.children << StepConfig.fromJson(childJson)
            }
            json.remove('steps')
        }

        // 剩余字段作为 params
        step.params = new HashMap<>(json)
        return step
    }
}
